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
import com.zg.carbonapp.MMKV.TokenManager
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
    private var currentFeed: UserFeed? = null // 当前动态（从MMKV获取）

    // 当前登录用户信息
    private val currentUser: User? by lazy { UserMMKV.getUser() }
    private val COMMENT_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 获取传递的feedId
        feedId = intent.getStringExtra("feedId")
        if (feedId.isNullOrEmpty()) {
            Toast.makeText(this, "动态ID获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. 校验登录状态
        if (currentUser == null || !TokenManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. 初始化视图
        initView()
        // 4. 加载动态详情（从MMKV）
        loadFeedDataFromMMKV()
        // 5. 加载评论（从MMKV）
        loadCommentsFromMMKV()
    }

    /**
     * 初始化视图（保留原UI逻辑，仅修改数据来源）
     */
    private fun initView() {
        // 返回按钮
        binding.toolbar.setOnClickListener { finish() }

        // 评论适配器（支持删除评论）
        commentAdapter = CommentAdapter(
            onDeleteClick = { deleteComment(it) },
            currentUserId = currentUser?.userId ?: ""
        )
        binding.recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(this@UserCommentActivity)
            adapter = commentAdapter
        }
        commentAdapter.setupSwipeToDelete(binding.recyclerViewComment)

        // 初始隐藏图片列表
        setupImages(binding, emptyList(), this)

        // 评论输入框监听（不为空才启用发送按钮）
        binding.etCommentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSendComment.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 发送评论
        binding.btnSendComment.setOnClickListener {
            val commentContent = binding.etCommentInput.text.toString().trim()
            if (commentContent.isNotBlank() && feedId != null && currentUser != null) {
                publishCommentToMMKV(commentContent)
            }
        }

        // 点赞按钮
        binding.btnLike.setOnClickListener {
            currentFeed?.let { handleLikeInMMKV(it) }
        }

        // 收藏按钮
        binding.btnShare.setOnClickListener {
            currentFeed?.let { handleSaveInMMKV(it) }
        }
    }

    /**
     * 从MMKV加载动态详情
     */
    private fun loadFeedDataFromMMKV() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 从MMKV获取所有动态，筛选当前feedId对应的动态
                val allFeeds = MMKVManager.getAllFeeds()
                currentFeed = allFeeds.firstOrNull { it.feedId == feedId }

                withContext(Dispatchers.Main) {
                    if (currentFeed == null) {
                        Toast.makeText(this@UserCommentActivity, "动态不存在或已删除", Toast.LENGTH_SHORT).show()
                        finish()
                        return@withContext
                    }

                    // 2. 更新UI显示动态信息
                    currentFeed?.apply {
                        binding.tvUsername.text = username
                        binding.tvTime.text = createTime
                        binding.tvContent.text = content

                        // 加载用户头像
                        loadAvatar(avatar, username, this@UserCommentActivity)

                        // 加载动态图片
                        setupImages(binding, images, this@UserCommentActivity)

                        // 更新点赞/收藏状态图标
                        updateLikeAndSaveState(isLiked, isSaved)
                    }
                }

            } catch (e: Exception) {
                Log.e("UserComment", "加载动态失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "加载动态失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 从MMKV加载评论列表
     */
    private fun loadCommentsFromMMKV() {
        val targetFeedId = feedId ?: return


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 从MMKV获取当前动态的所有评论
                val comments = MMKVManager.getCommentsByFeedId(targetFeedId)

                withContext(Dispatchers.Main) {

                    // 2. 提交评论列表到适配器
                    commentAdapter.submitList(comments)
                    // 3. 显示评论数
                    binding.tvCommentCount.text = "评论(${comments.size})"
                }

            } catch (e: Exception) {
                Log.e("UserComment", "加载评论失败: ${e.message}", e)
                withContext(Dispatchers.Main) {

                    Toast.makeText(this@UserCommentActivity, "加载评论失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 发布评论到MMKV（本地保存）
     */
    private fun publishCommentToMMKV(commentContent: String) {
        val targetFeedId = feedId ?: return
        val user = currentUser ?: return

        // 1. 构造评论对象
        val newComment = UserComment(
            feedId = targetFeedId,
            userId = user.userId ?: "",
            username = user.userName ?: "未知用户",
            avatar = user.userAvatar ?: "",
            content = commentContent,
            commentTime = getCurrentTime() // 当前时间
        )

        binding.btnSendComment.isEnabled = false // 防止重复提交
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 2. 保存评论到MMKV（自动更新评论数和commentedFeeds）
                MMKVManager.addComment(targetFeedId, newComment)

                withContext(Dispatchers.Main) {
                    // 3. 清空输入框
                    binding.etCommentInput.text.clear()
                    // 4. 重新加载评论列表
                    loadCommentsFromMMKV()
                    // 5. 重新加载动态（更新评论数显示）
                    loadFeedDataFromMMKV()
                    Toast.makeText(this@UserCommentActivity, "评论发布成功", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("UserComment", "发布评论失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "评论发布失败", Toast.LENGTH_SHORT).show()
                    binding.btnSendComment.isEnabled = true
                }
            }
        }
    }

    /**
     * 从MMKV删除评论
     */
    private fun deleteComment(comment: UserComment) {
        val targetFeedId = feedId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 从MMKV删除评论（自动更新评论数）
                MMKVManager.deleteComment(targetFeedId, comment)

                withContext(Dispatchers.Main) {
                    // 2. 重新加载评论列表
                    loadCommentsFromMMKV()
                    // 3. 重新加载动态（更新评论数）
                    loadFeedDataFromMMKV()
                    Toast.makeText(this@UserCommentActivity, "评论删除成功", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("UserComment", "删除评论失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "评论删除失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 本地点赞/取消点赞（同步MMKV所有相关缓存）
     */
    private fun handleLikeInMMKV(feed: UserFeed) {
        val newLikeState = !feed.isLiked
        val newLikeCount = if (newLikeState) feed.likeCount + 1 else feed.likeCount - 1

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 更新当前动态的点赞状态
                val updatedFeed = feed.copy(
                    isLiked = newLikeState,
                    likeCount = newLikeCount
                )
                currentFeed = updatedFeed

                // 2. 同步更新MMKV的"所有动态"
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()
                val feedIndex = allFeeds.indexOfFirst { it.feedId == feed.feedId }
                if (feedIndex != -1) {
                    allFeeds[feedIndex] = updatedFeed
                    MMKVManager.saveAllFeeds(allFeeds)
                }

                // 3. 同步更新MMKV的"点赞列表"
                val likedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                if (newLikeState) {
                    if (!likedFeeds.any { it.feedId == feed.feedId }) {
                        likedFeeds.add(updatedFeed)
                    }
                } else {
                    likedFeeds.removeAll { it.feedId == feed.feedId }
                }
                MMKVManager.saveLikedFeeds(likedFeeds)

                // 4. 更新UI
                withContext(Dispatchers.Main) {
                    updateLikeAndSaveState(newLikeState, feed.isSaved)
                    Toast.makeText(
                        this@UserCommentActivity,
                        if (newLikeState) "点赞成功" else "取消点赞成功",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("UserComment", "点赞操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "点赞操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 本地收藏/取消收藏（同步MMKV所有相关缓存）
     */
    private fun handleSaveInMMKV(feed: UserFeed) {
        val newSaveState = !feed.isSaved
        val newSaveCount = if (newSaveState) feed.shareCount + 1 else feed.shareCount - 1

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 更新当前动态的收藏状态
                val updatedFeed = feed.copy(
                    isSaved = newSaveState,
                    shareCount = newSaveCount
                )
                currentFeed = updatedFeed

                // 2. 同步更新MMKV的"所有动态"
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()
                val feedIndex = allFeeds.indexOfFirst { it.feedId == feed.feedId }
                if (feedIndex != -1) {
                    allFeeds[feedIndex] = updatedFeed
                    MMKVManager.saveAllFeeds(allFeeds)
                }

                // 3. 同步更新MMKV的"收藏列表"
                val savedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                if (newSaveState) {
                    if (!savedFeeds.any { it.feedId == feed.feedId }) {
                        savedFeeds.add(updatedFeed)
                    }
                } else {
                    savedFeeds.removeAll { it.feedId == feed.feedId }
                }
                MMKVManager.saveSavedFeeds(savedFeeds)

                // 4. 更新UI
                withContext(Dispatchers.Main) {
                    updateLikeAndSaveState(feed.isLiked, newSaveState)
                    Toast.makeText(
                        this@UserCommentActivity,
                        if (newSaveState) "收藏成功" else "取消收藏成功",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("UserComment", "收藏操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCommentActivity, "收藏操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 加载用户头像（保留原Glide逻辑，支持本地路径）
     */
    private fun loadAvatar(avatarUrl: String?, username: String, context: Context) {
        binding.ivAvatar.setImageResource(R.drawable.default_avatar)
        if (avatarUrl.isNullOrEmpty()) {
            Log.w("AvatarLoad", "用户[$username] - 头像路径为空")
            return
        }

        // 权限校验（针对本地图片路径）
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

        // Glide加载（支持本地路径/网络URL）
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
                    Log.e("AvatarLoad", "用户[$username] - 头像加载失败: ${e?.message}", e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("AvatarLoad", "用户[$username] - 头像加载成功")
                    return false
                }
            })
            .into(binding.ivAvatar)
    }

    /**
     * 动态图片列表设置（保留原逻辑）
     */
    private fun setupImages(binding: ActivityUserCommentBinding, images: List<String>, context: Context) {
        if (images.isEmpty()) {
            binding.rvImages.visibility = ViewGroup.GONE
            return
        }
        binding.rvImages.visibility = ViewGroup.VISIBLE

        // 根据图片数量设置列表高度
        val height = when {
            images.size == 1 -> 300
            images.size in 2..4 -> 200
            else -> 150
        }
        binding.rvImages.layoutParams.height = dpToPx(height, context)

        // 图片适配器
        val imageAdapter = FeedImageAdapter(images, context)
        binding.rvImages.adapter = imageAdapter
        binding.rvImages.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    /**
     * DP转PX（工具方法）
     */
    private fun dpToPx(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * 获取当前时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(Date())
    }

    /**
     * 更新点赞/收藏图标状态
     */
    private fun updateLikeAndSaveState(isLiked: Boolean, isSaved: Boolean) {
        binding.ivLike.setImageResource(
            if (isLiked) R.drawable.like__1_ else R.drawable.like
        )
        binding.ivShare.setImageResource(
            if (isSaved) R.drawable.save_1 else R.drawable.save
        )
        // 更新点赞数显示
        currentFeed?.let {
            binding.tvLikeCount.text = "${it.likeCount} 点赞"
        }
    }

    /**
     * 权限回调（头像加载权限）
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10086 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，重新加载头像
                currentFeed?.let {
                    loadAvatar(it.avatar, it.username, this)
                }
            } else {
                Toast.makeText(this, "缺少存储权限，头像可能无法正常加载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 返回时通知上一页刷新数据
     */
    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra("feedId", feedId))
        super.finish()
    }
}