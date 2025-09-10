package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zg.carbonapp.Adapter.CommentAdapter
import com.zg.carbonapp.Adapter.FeedImageAdapter
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.Dao.UserComment
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Service.RetrofitClient
import com.zg.carbonapp.databinding.ActivityUserCommentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserCommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserCommentBinding
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var feedImageAdapter: FeedImageAdapter
    private var feedId: String? = null
    private var currentFeed: UserFeed? = null

    // 使用 UserMMKV 获取用户信息
    private val currentUser: User? by lazy { UserMMKV.getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        feedId = intent.getStringExtra("feedId")
        if (feedId.isNullOrEmpty()) {
            Toast.makeText(this, "动态信息获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initView()
        loadFeedData()
        loadComments()
    }

    private fun initView() {
        binding.icBack.setOnClickListener { finish() }

        commentAdapter = CommentAdapter(
            onDeleteClick = { deleteComment(it) },
            currentUserId = currentUser?.userId ?: ""
        )

        binding.recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(this@UserCommentActivity)
            adapter = commentAdapter
        }
        commentAdapter.setupSwipeToDelete(binding.recyclerViewComment)

        setupImages(binding, emptyList(), this)

        binding.etCommentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSendComment.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSendComment.setOnClickListener {
            val commentContent = binding.etCommentInput.text.toString().trim()
            if (commentContent.isNotBlank() && feedId != null && currentUser != null) {
                publishComment(commentContent)
            }
        }

        binding.btnLike.setOnClickListener {
            currentFeed?.let { handleLikeClick(it) }
        }

        binding.btnShare.setOnClickListener {
            currentFeed?.let { handleSaveClick(it) }
        }
    }

    // 添加 loadComments 方法
    private fun loadComments() {
        val feedId = this.feedId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCommentsByFeedId(feedId)
                if (response.isSuccessful && response.body()?.code == 200) {
                    val comments = response.body()?.data ?: emptyList()

                    withContext(Dispatchers.Main) {
                        // 将 Comment 对象转换为 UserComment 对象
                        val userComments = comments.map { comment ->
                            UserComment(
                                feedId = comment.feedId,
                                userId = comment.userId,
                                username = comment.userName,
                                avatar = comment.avatar,
                                content = comment.content,
                                commentTime = comment.commentTime
                            )
                        }
                        commentAdapter.submitList(userComments)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserCommentActivity, "加载评论失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "加载评论失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserCommentActivity", "加载评论失败", e)
                }
            }
        }
    }

    private fun setupImages(binding: ActivityUserCommentBinding, images: List<String>, context: Context) {
        if (images.isEmpty()) {
            binding.rvImages.visibility = ViewGroup.GONE
            return
        }
        binding.rvImages.visibility = ViewGroup.VISIBLE
        val height = when {
            images.size == 1 -> 300
            images.size in 2..4 -> 200
            else -> 150
        }
        binding.rvImages.layoutParams.height = dpToPx(height, context)
        val imageAdapter = FeedImageAdapter(images, context)
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun dpToPx(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun loadFeedData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 获取动态详情
                val response = RetrofitClient.instance.getDynamicDetail(feedId!!)
                if (response.isSuccessful && response.body()?.code == 200) {
                    val dynamicDetail = response.body()?.data
                    withContext(Dispatchers.Main) {
                        dynamicDetail?.let { detail ->
                            // 更新UI显示动态信息
                            binding.tvUsername.text = detail.dynamic.userName
                            binding.tvTime.text = detail.dynamic.createTime
                            binding.tvContent.text = detail.dynamic.content

                            // 加载头像
                            loadAvatar(detail.dynamic.avatar, detail.dynamic.userName, this@UserCommentActivity)

                            // 加载图片
                            setupImages(binding, detail.dynamic.pics, this@UserCommentActivity)

                            // 更新点赞和收藏状态
                            updateLikeAndSaveState(detail.dynamic.isLiked, detail.dynamic.isSaved)

                            // 保存到本地currentFeed
                            currentFeed?.let { feed ->
                                feed.feedId = detail.dynamic.feedId
                                feed.userId = detail.dynamic.userId
                                feed.username = detail.dynamic.userName
                                feed.avatar = detail.dynamic.avatar
                                feed.content = detail.dynamic.content
                                feed.images = detail.dynamic.pics
                                feed.createTime = detail.dynamic.createTime
                            }
                        } ?: run {
                            Toast.makeText(this@UserCommentActivity, "动态信息获取失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserCommentActivity, "动态信息获取失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "动态信息获取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserCommentActivity", "加载动态信息失败", e)
                }
            }
        }
    }

    private fun publishComment(content: String) {
        val feedId = this.feedId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")
                val response = RetrofitClient.instance.publishComment(
                    "Bearer $token",
                    feedId,
                    content
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    // 重新加载评论和动态信息
                    loadComments()
                    loadFeedData()

                    val isCommented = response.body()?.data ?: false
                    withContext(Dispatchers.Main) {
                        currentFeed?.isCommented = isCommented
                        binding.etCommentInput.text.clear()
                        Toast.makeText(this@UserCommentActivity, "评论发布成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserCommentActivity, "评论发布失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "评论发布失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserCommentActivity", "发布评论失败", e)
                }
            }
        }
    }

    private fun loadAvatar(avatarUrl: String?, username: String, context: Context) {
        binding.ivAvatar.setImageResource(R.drawable.default_avatar)
        if (avatarUrl.isNullOrEmpty()) {
            Log.w("AvatarLoad", "用户[$username] - 头像URL为空")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as AppCompatActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    10086
                )
                return
            }
        }
        Glide.with(context)
            .load(avatarUrl)
            .circleCrop()
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("AvatarLoad", "用户[$username] - 加载失败: ${e?.message}", e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("AvatarLoad", "用户[$username] - 加载成功")
                    return false
                }
            })
            .into(binding.ivAvatar)
    }

    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(Date())
    }

    private fun deleteComment(comment: UserComment) {
        // 注意：接口文档中没有提供删除评论的接口
        // 这里只能从本地删除，无法从服务器删除
        Toast.makeText(this, "无法删除服务器评论", Toast.LENGTH_SHORT).show()
    }

    private fun handleLikeClick(feed: UserFeed) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")
                val response = RetrofitClient.instance.likeDynamic(
                    "Bearer $token",
                    feed.feedId
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    // 重新加载动态信息
                    loadFeedData()
                    val isLiked = response.body()?.data ?: false
                    withContext(Dispatchers.Main) {
                        currentFeed?.isLiked = isLiked
                        Toast.makeText(
                            this@UserCommentActivity,
                            if (isLiked) "取消点赞成功" else "点赞成功",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserCommentActivity, "操作失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserCommentActivity", "点赞失败", e)
                }
            }
        }
    }

    private fun handleSaveClick(feed: UserFeed) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")
                val response = RetrofitClient.instance.collectDynamic(
                    "Bearer $token",
                    feed.feedId
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    // 重新加载动态信息
                    loadFeedData()
                    val isSaved = response.body()?.data ?: false
                    withContext(Dispatchers.Main) {
                        currentFeed?.isSaved = isSaved
                        Toast.makeText(
                            this@UserCommentActivity,
                            if (isSaved) "取消收藏成功" else "收藏成功",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserCommentActivity, "操作失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserCommentActivity", "收藏失败", e)
                }
            }
        }
    }

    private fun updateLikeAndSaveState(isLiked: Boolean, isSaved: Boolean) {
        binding.ivLike.setImageResource(
            if (isLiked) R.drawable.like__1_ else R.drawable.like
        )
        binding.ivShare.setImageResource(
            if (isSaved) R.drawable.save_1 else R.drawable.save
        )
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }
}