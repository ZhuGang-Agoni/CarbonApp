package com.zg.carbonapp.Adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.FragmentFeedItemCommunityBinding
import android.Manifest
import android.content.pm.PackageManager
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File

class CommunityFeedAdapter(
    private val onLikeClick: (Int) -> Unit,
    private val onCommentClick: (Int) -> Unit,
    private val onShareClick: (Int) -> Unit, // 对应收藏点击
    private val onAvatarClick: (Int) -> Unit
) : ListAdapter<UserFeed, CommunityFeedAdapter.ViewHolder>(FeedDiffCallback()) {

    inner class ViewHolder(private val binding: FragmentFeedItemCommunityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(feed: UserFeed, context: Context) {
            // 基础信息绑定
            binding.tvUsername.text = feed.username
            binding.tvTime.text = feed.createTime
            binding.tvContent.text = feed.content
            binding.tvLikeCount.text = feed.likeCount.toString()
            binding.tvCommentCount.text = feed.commentCount.toString()
            binding.tvShareCount.text = feed.shareCount.toString() // 收藏数

            // 1. 加载用户头像（原有逻辑保留）
            loadAvatar(feed.avatar, feed.username, context)

            // 2. 根据状态显示图标（关键：适配本地存储的状态）
            // 点赞图标：已点赞（红色）/未点赞（灰色）
            binding.ivLike.setImageResource(
                if (feed.isLiked) R.drawable.like__1_ else R.drawable.like
            )
            // 收藏图标：已收藏（黄色）/未收藏（灰色）
            binding.ivShare.setImageResource(
                if (feed.isSaved) R.drawable.save_1 else R.drawable.save
            )
            // 评论图标：已评论（蓝色）/未评论（灰色）
            binding.ivComment.setImageResource(
                if (feed.isCommented) R.drawable.comment_light_1 else R.drawable.comment_light
            )

            // 3. 绑定动态图片（原有逻辑保留）
            setupImages(binding, feed.images, context)

            // 4. 点击事件绑定（原有逻辑保留）
            binding.ivAvatar.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAvatarClick(adapterPosition)
                }
            }
            binding.ivLike.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onLikeClick(adapterPosition)
                }
            }
            binding.ivComment.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCommentClick(adapterPosition)
                }
            }
            binding.ivShare.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onShareClick(adapterPosition)
                }
            }
        }

        // 原有头像加载逻辑（保留）
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
                        context as android.app.Activity,
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

        // 原有图片加载逻辑（保留）
        private fun setupImages(binding: FragmentFeedItemCommunityBinding, images: List<String>, context: Context) {
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentFeedItemCommunityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), holder.itemView.context)
    }
}

// 原有FeedImageAdapter和FeedDiffCallback（保留）
class FeedImageAdapter(private val images: List<String>, private val context: Context) :
    RecyclerView.Adapter<FeedImageAdapter.ImageViewHolder>() {
    inner class ImageViewHolder(private val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView) {
        fun bind(imagePath: String) {
            try {
                val file = File(imagePath)
                if (file.exists() && file.isFile) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                    imageView.setImageBitmap(bitmap)
                    Log.d("ImageLoad", "路径加载成功: $imagePath")
                } else {
                    Log.e("ImageLoad", "文件不存在: $imagePath")
                    imageView.setImageResource(R.drawable.ic_image_placeholder)
                }
            } catch (e: Exception) {
                Log.e("ImageLoad", "路径加载失败: $imagePath, 错误: ${e.message}")
                imageView.setImageResource(R.drawable.ic_image_error)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                dpToPx(150, context),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(4, context), 0, dpToPx(4, context), 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImageViewHolder(imageView)
    }
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }
    override fun getItemCount(): Int = images.size
    private fun dpToPx(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

class FeedDiffCallback : DiffUtil.ItemCallback<UserFeed>() {
    override fun areItemsTheSame(oldItem: UserFeed, newItem: UserFeed): Boolean {
        return oldItem.feedId == newItem.feedId // 用feedId判断是否为同一条动态
    }
    override fun areContentsTheSame(oldItem: UserFeed, newItem: UserFeed): Boolean {
        return oldItem == newItem // 全字段对比，确保状态更新时触发刷新
    }
}