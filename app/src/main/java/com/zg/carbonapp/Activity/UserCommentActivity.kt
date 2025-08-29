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
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
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
    private var currentFeed: UserFeed? = null  // 当前动态对象（包含发布者信息）

    // 从UserMMKV获取当前登录用户信息
    private val currentUser: User? by lazy { UserMMKV.getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递的feedId
        feedId = intent.getStringExtra("feedId")
        if (feedId.isNullOrEmpty()) {
            Toast.makeText(this, "动态信息获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 检查用户登录状态
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
        // 返回按钮
        binding.icBack.setOnClickListener{
            finish()
        }

        // 评论列表Adapter（使用当前登录用户ID判断删除权限）
        commentAdapter = CommentAdapter(
            onDeleteClick = { deleteComment(it) },
            currentUserId = currentUser?.userId ?: ""
        )


        binding.recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(this@UserCommentActivity)
            adapter = commentAdapter
        }
       commentAdapter.setupSwipeToDelete(binding.recyclerViewComment)


        // 动态图片Adapter
        setupImages(binding, emptyList(),this)

        // 评论输入框监听
        binding.etCommentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSendComment.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 发布评论按钮
        binding.btnSendComment.setOnClickListener {
            val commentContent = binding.etCommentInput.text.toString().trim()
            if (commentContent.isNotBlank() && feedId != null && currentUser != null) {
                publishComment(commentContent)
            }
        }

        // 动态点赞按钮
        binding.btnLike.setOnClickListener {
            currentFeed?.let { handleLikeClick(it) }
        }

        // 动态收藏按钮
        binding.btnShare.setOnClickListener {
            currentFeed?.let { handleSaveClick(it) }
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

    /**
     * 加载动态数据（从MMKV获取，包含发布者信息）
     */
    private fun loadFeedData() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 从MMKV获取所有动态，匹配当前feedId
            val allFeeds = MMKVManager.getAllFeeds()
            currentFeed = allFeeds.firstOrNull { it.feedId == feedId }

            withContext(Dispatchers.Main) {
                currentFeed?.let { feed ->
                    // 动态发布者信息（来自UserFeed）
                    binding.tvUsername.text = feed.username
                    binding.tvTime.text = feed.createTime
                    binding.tvContent.text = feed.content
                    binding.tvLikeCount.text = feed.likeCount.toString()
                    binding.tvCommentCount.text = feed.commentCount.toString()
                    binding.tvShareCount.text = feed.shareCount.toString()

                    // 加载动态发布者的头像
                    loadAvatar(feed.avatar, feed.username, this@UserCommentActivity)

                    // 加载动态图片
                    setupImages(binding,feed.images,this@UserCommentActivity)

                    // 更新点赞/收藏状态
                    updateLikeAndSaveState(feed)
                } ?: run {
                    Toast.makeText(this@UserCommentActivity, "动态已删除", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /**
     * 加载评论列表
     */
    private fun loadComments() {
        lifecycleScope.launch(Dispatchers.IO) {
            val comments = MMKVManager.getCommentsByFeedId(feedId ?: "")

            withContext(Dispatchers.Main) {
                if (comments.isEmpty()) {
//                    binding.tvEmpty.visibility = View.VISIBLE
                    commentAdapter.submitList(emptyList())
                } else {
//                    binding.tvEmpty.visibility = View.GONE
                    commentAdapter.submitList(comments)
                }
            }
        }
    }

    /**
     * 发布评论（使用从UserMMKV获取的当前登录用户信息）
     * 关键修改：发布成功后回传「更新后的评论数+feedId」
     */
    private fun publishComment(content: String) {
        val feedId = this.feedId ?: return
        val user = currentUser ?: return

        // 创建评论对象（评论者信息来自UserMMKV）
        val newComment = UserComment(
            feedId = feedId,
            userId = user.userId,         // 当前登录用户ID
            username = user.userName,     // 当前登录用户名
            avatar = user.userEvator,     // 当前登录用户头像
            content = content,
            commentTime = getCurrentTime()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            MMKVManager.addComment(feedId, newComment)

            // 1. 获取更新后的动态（包含最新评论数）
            val updatedFeed = MMKVManager.getAllFeeds().firstOrNull { it.feedId == feedId }
            val newCommentCount = updatedFeed?.commentCount ?: 0

            withContext(Dispatchers.Main) {
                binding.etCommentInput.text.clear()
                loadComments()
                // 2. 刷新当前页面评论数显示
                currentFeed = updatedFeed
                currentFeed?.let {
                    binding.tvCommentCount.text = it.commentCount.toString()
                }

                // 3. 关键：回传结果给来源Fragment（feedId + 最新评论数）
                val resultIntent = Intent()
                resultIntent.putExtra("feedId", feedId)
                resultIntent.putExtra("newCommentCount", newCommentCount)
                setResult(RESULT_OK, resultIntent)

                Toast.makeText(this@UserCommentActivity, "评论发布成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 加载头像（支持动态发布者头像）
     */
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

    /**
     * 获取目前时间
     */
    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(Date())
    }

    /**
     * 删除评论
     * 关键修改：删除成功后回传「更新后的评论数+feedId」
     */
    private fun deleteComment(comment: UserComment) {
        val feedId = this.feedId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            MMKVManager.deleteComment(feedId, comment)

            // 1. 获取更新后的动态（包含最新评论数）
            val updatedFeed = MMKVManager.getAllFeeds().firstOrNull { it.feedId == feedId }
            val newCommentCount = updatedFeed?.commentCount ?: 0

            withContext(Dispatchers.Main) {
                loadComments()
                // 2. 刷新当前页面评论数显示
                currentFeed = updatedFeed
                currentFeed?.let {
                    binding.tvCommentCount.text = it.commentCount.toString()
                }

                // 3. 关键：回传结果给来源Fragment（feedId + 最新评论数）
                val resultIntent = Intent()
                resultIntent.putExtra("feedId", feedId)
                resultIntent.putExtra("newCommentCount", newCommentCount)
                setResult(RESULT_OK, resultIntent)

                Toast.makeText(this@UserCommentActivity, "评论已删除", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 点赞逻辑
     */
    private fun handleLikeClick(feed: UserFeed) {
        val newLikeState = !feed.isLiked
        lifecycleScope.launch(Dispatchers.IO) {
            val allFeeds = MMKVManager.getAllFeeds().toMutableList()
            val index = allFeeds.indexOfFirst { it.feedId == feed.feedId }
            if (index != -1) {
                val updatedFeed = allFeeds[index].copy(
                    isLiked = newLikeState,
                    likeCount = feed.likeCount + if (newLikeState) 1 else -1
                )
                allFeeds[index] = updatedFeed
                MMKVManager.saveAllFeeds(allFeeds)

                val likedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                if (newLikeState) {
                    if (!likedFeeds.any { it.feedId == feed.feedId }) {
                        likedFeeds.add(updatedFeed)
                    }
                } else {
                    likedFeeds.removeAll { it.feedId == feed.feedId }
                }
                MMKVManager.saveLikedFeeds(likedFeeds)

                withContext(Dispatchers.Main) {
                    currentFeed = updatedFeed
                    binding.tvLikeCount.text = updatedFeed.likeCount.toString()
                    binding.ivLike.setImageResource(
                        if (newLikeState) R.drawable.like__1_ else R.drawable.like
                    )
                    Toast.makeText(
                        this@UserCommentActivity,
                        if (newLikeState) "点赞成功" else "取消点赞",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * 收藏逻辑
     */
    private fun handleSaveClick(feed: UserFeed) {
        val newSaveState = !feed.isSaved
        lifecycleScope.launch(Dispatchers.IO) {
            val allFeeds = MMKVManager.getAllFeeds().toMutableList()
            val index = allFeeds.indexOfFirst { it.feedId == feed.feedId }
            if (index != -1) {
                val updatedFeed = allFeeds[index].copy(
                    isSaved = newSaveState,
                    shareCount = feed.shareCount + if (newSaveState) 1 else -1
                )
                allFeeds[index] = updatedFeed
                MMKVManager.saveAllFeeds(allFeeds)

                val savedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                if (newSaveState) {
                    if (!savedFeeds.any { it.feedId == feed.feedId }) {
                        savedFeeds.add(updatedFeed)
                    }
                } else {
                    savedFeeds.removeAll { it.feedId == feed.feedId }
                }
                MMKVManager.saveSavedFeeds(savedFeeds)

                withContext(Dispatchers.Main) {
                    currentFeed = updatedFeed
                    binding.tvShareCount.text = updatedFeed.shareCount.toString()
                    binding.ivShare.setImageResource(
                        if (newSaveState) R.drawable.save_1 else R.drawable.save
                    )
                    Toast.makeText(
                        this@UserCommentActivity,
                        if (newSaveState) "收藏成功" else "取消收藏",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * 更新点赞和收藏状态显示
     */
    private fun updateLikeAndSaveState(feed: UserFeed) {
        binding.ivLike.setImageResource(
            if (feed.isLiked) R.drawable.like__1_ else R.drawable.like
        )
        binding.ivShare.setImageResource(
            if (feed.isSaved) R.drawable.save_1 else R.drawable.save
        )
    }

    // 优化：返回时若未发布/删除评论，也正常回传结果（避免来源Fragment接收不到结果）
    override fun finish() {
        if (intent.hasExtra("feedId") && currentFeed != null) {
            val resultIntent = Intent()
            resultIntent.putExtra("feedId", feedId)
            resultIntent.putExtra("newCommentCount", currentFeed?.commentCount ?: 0)
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_OK)
        }
        super.finish()
    }
}