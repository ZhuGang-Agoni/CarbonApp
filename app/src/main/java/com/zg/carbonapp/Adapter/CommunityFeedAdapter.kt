package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.FragmentFeedItemCommunityBinding

class CommunityFeedAdapter(
    private val onLikeClick: (Int) -> Unit,//这种写法是比较合理的
    private val onCommentClick: (Int) -> Unit,
    private val onShareClick: (Int) -> Unit,
    private val onAvatarClick: (Int) -> Unit
) : ListAdapter<UserFeed, CommunityFeedAdapter.ViewHolder>(FeedDiffCallback()) {

    inner class ViewHolder(private val binding: FragmentFeedItemCommunityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(feed: UserFeed) {
            // 绑定基本数据
            binding.tvUsername.text = feed.username
            binding.tvTime.text = feed.createTime
            binding.tvContent.text = feed.content
            binding.tvLikeCount.text = feed.likeCount.toString()
            binding.tvCommentCount.text = feed.commentCount.toString()
            binding.tvShareCount.text = feed.shareCount.toString()

            // 加载头像
            Glide.with(binding.ivAvatar.context)
                .load(feed.avatar)
//                .placeholder(R.drawable.img)
//                .error(R.drawable.img)
                .circleCrop()
                .into(binding.ivAvatar)

            // 设置点赞状态（修复逻辑：根据isLiked设置图标和颜色）
            val isLiked = feed.isLiked
            binding.ivLike.setImageResource(
                if (isLiked) R.drawable.like__1_ else R.drawable.like
            )

            // 处理图片展示
            setupImages(binding, feed.images)

            // 设置点击事件
            binding.ivAvatar.setOnClickListener { onAvatarClick (adapterPosition) }
            binding.ivLike.setOnClickListener { onLikeClick(adapterPosition) }
            binding.ivComment.setOnClickListener { onCommentClick(adapterPosition) }
            binding.ivShare.setOnClickListener { onShareClick(adapterPosition) }
        }

        private fun setupImages(binding: FragmentFeedItemCommunityBinding, images: List<String>) {
            if (images.isEmpty()) {
                binding.rvImages.visibility = ViewGroup.GONE
                return
            }

            binding.rvImages.visibility = ViewGroup.VISIBLE
            binding.rvImages.layoutParams.height = if (images.size == 1) 300 else 150

            val imageAdapter = FeedImageAdapter(images)
            binding.rvImages.adapter = imageAdapter
            binding.rvImages.layoutManager = object : LinearLayoutManager(binding.rvImages.context) {
                override fun canScrollHorizontally(): Boolean = true
            }
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
        holder.bind(getItem(position))
    }
}

class FeedImageAdapter(private val images: List<String>) :
    RecyclerView.Adapter<FeedImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView) {

        fun bind(imageUrl: String) {
            Glide.with(imageView.context)
                .load(imageUrl)
//                .placeholder(R.drawable.img_2)
//                .error(R.drawable.img_2)
                .centerCrop()
                .into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                150,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 0, 4, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
}

class FeedDiffCallback : DiffUtil.ItemCallback<UserFeed>() {
    override fun areItemsTheSame(oldItem: UserFeed, newItem: UserFeed): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: UserFeed, newItem: UserFeed): Boolean {
        return oldItem == newItem
    }
}