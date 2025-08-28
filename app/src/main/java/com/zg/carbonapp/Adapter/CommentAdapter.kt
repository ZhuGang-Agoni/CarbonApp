package com.zg.carbonapp.Adapter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zg.carbonapp.Dao.UserComment
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ItemCommentBinding

class CommentAdapter(
    private val onDeleteClick: (UserComment) -> Unit, // 删除回调（通知上层处理删除）
    private val currentUserId: String // 当前登录用户 ID（判断是否显示删除按钮）
) : ListAdapter<UserComment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    // 记录当前滑动的position，用于防止多个item同时出现删除按钮
    private var currentSwipedPosition = -1
    private var itemTouchHelper: ItemTouchHelper? = null
    private var recyclerView: RecyclerView? = null

    // 评论列表项 ViewHolder
    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var startX = 0f
        private var isSwiped = false

        fun bind(comment: UserComment) {
            binding.apply {
                // 加载头像
                loadAvatar(comment.avatar, comment.username, itemView.context, ivAvatar)
                // 绑定数据
                tvUsername.text = comment.username
                tvContent.text = comment.content
                tvCommentTime.text = formatCommentTime(comment.commentTime)

                // 初始状态下删除按钮不可见
                btnDelete.visibility = View.GONE

                // 只有当前用户的评论才允许左滑删除
                if (comment.userId == currentUserId) {
                    // 设置删除按钮点击事件
                    btnDelete.setOnClickListener {
                        showDeleteDialog(itemView.context, comment) {
                            onDeleteClick(comment) // 触发外部删除逻辑
                            // 删除后重置滑动状态
                            resetSwipedPosition()
                        }
                    }

                    // 设置触摸监听器，用于处理右滑恢复
                    clCommentContent.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // 记录触摸起始点
                                startX = event.x
                                return@setOnTouchListener false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                // 如果当前项是滑出的项，并且是向右滑动
                                if (isSwiped && event.x - startX > 50) {
                                    // 右滑恢复
                                    resetSwipedPosition()
                                    return@setOnTouchListener true
                                }
                                return@setOnTouchListener false
                            }
                            else -> return@setOnTouchListener false
                        }
                    }
                } else {
                    // 不是当前用户的评论，禁用滑动功能
                    clCommentContent.setOnTouchListener { v, event ->
                        // 拦截触摸事件，防止滑动
                        true
                    }
                }
            }
        }

        // 显示删除按钮
        fun showDeleteButton() {
            binding.btnDelete.visibility = View.VISIBLE
            isSwiped = true
        }

        // 隐藏删除按钮
        fun hideDeleteButton() {
            binding.btnDelete.visibility = View.GONE
            isSwiped = false
        }
    }

    // 格式化评论时间（兼容时间戳和字符串）
    private fun formatCommentTime(timeString: String): String {
        return try {
            val timestamp = timeString.toLong()
            getTimeAgo(timestamp)
        } catch (e: NumberFormatException) {
            try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val date = format.parse(timeString)
                date?.let { getTimeAgo(it.time) } ?: timeString
            } catch (e1: java.text.ParseException) {
                timeString
            }
        }
    }

    // 计算相对时间（刚刚、X 分钟前等）
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < java.util.concurrent.TimeUnit.MINUTES.toMillis(1) -> "刚刚"
            diff < java.util.concurrent.TimeUnit.HOURS.toMillis(1) -> "${java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
            diff < java.util.concurrent.TimeUnit.DAYS.toMillis(1) -> "${java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff)}小时前"
            diff < java.util.concurrent.TimeUnit.DAYS.toMillis(7) -> "${java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)}天前"
            else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        }
    }

    // 统一加载头像（处理权限和异常）
    private fun loadAvatar(
        avatarUrl: String?,
        username: String,
        context: Context,
        imageView: ImageView
    ) {
        imageView.setImageResource(R.drawable.default_avatar)
        if (avatarUrl.isNullOrEmpty()) {
            Log.w("AvatarLoad", "用户[$username] - 头像URL为空")
            return
        }
        // Android 13 以下适配存储权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                (context as? androidx.appcompat.app.AppCompatActivity)?.let {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 10086)
                }
                return
            }
        }
        // Glide 加载圆形头像
        Glide.with(context)
            .load(avatarUrl)
            .circleCrop()
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    Log.e("AvatarLoad", "用户[$username] - 加载失败: ${e?.message}", e)
                    return false
                }
                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    Log.d("AvatarLoad", "用户[$username] - 加载成功")
                    return false
                }
            })
            .into(imageView)
    }

    // 删除确认弹窗
    private fun showDeleteDialog(context: Context, comment: UserComment, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("删除评论")
            .setMessage("确定要删除这条评论吗？删除后无法恢复")
            .setPositiveButton("确定") { dialog, _ ->
                onConfirm.invoke()
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    // 绑定数据
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))

        // 如果当前项是滑动的项，显示删除按钮，否则隐藏
        if (position == currentSwipedPosition) {
            holder.showDeleteButton()
        } else {
            holder.hideDeleteButton()
        }
    }

    // 设置滑动帮助类
    fun setupSwipeToDelete(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val comment = getItem(position)

                // 只有当前用户的评论才能滑动删除
                if (comment.userId == currentUserId) {
                    // 更新滑动位置
                    if (currentSwipedPosition != -1 && currentSwipedPosition != position) {
                        notifyItemChanged(currentSwipedPosition)
                    }
                    currentSwipedPosition = position
                    notifyItemChanged(position)
                } else {
                    // 不是当前用户的评论，重置滑动状态
                    notifyItemChanged(position)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder != null) {
                    // 滑动开始时，隐藏之前显示的删除按钮
                    if (currentSwipedPosition != -1 && currentSwipedPosition != viewHolder.adapterPosition) {
                        val previousPosition = currentSwipedPosition
                        currentSwipedPosition = -1
                        notifyItemChanged(previousPosition)
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // 滑动结束时，如果不是有效的滑动，重置状态
                val position = viewHolder.adapterPosition
                val comment = getItem(position)
                if (comment.userId != currentUserId) {
                    currentSwipedPosition = -1
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.5f // 滑动阈值设为50%
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // 限制最大滑动距离为删除按钮的宽度
                val maxSwipeDistance = 80.dpToPx(viewHolder.itemView.context)
                val clampedDX = when {
                    dX < -maxSwipeDistance -> -maxSwipeDistance.toFloat()
                    dX > 0 -> 0f // 不允许向右滑动
                    else -> dX
                }

                super.onChildDraw(c, recyclerView, viewHolder, clampedDX, dY, actionState, isCurrentlyActive)

                // 如果正在向右滑动并且已经显示了删除按钮，则恢复原状
                if (isCurrentlyActive && clampedDX > -10 && currentSwipedPosition == viewHolder.adapterPosition) {
                    resetSwipedPosition()
                }
            }
        }

        itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper?.attachToRecyclerView(recyclerView)

        // 添加点击事件监听器，点击空白区域时恢复滑动状态
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN && currentSwipedPosition != -1) {
                    // 检查是否点击在非滑动的项上
                    val child = rv.findChildViewUnder(e.x, e.y)
                    if (child != null) {
                        val position = rv.getChildAdapterPosition(child)
                        if (position != currentSwipedPosition) {
                            resetSwipedPosition()
                        }
                    } else {
                        // 点击在空白区域
                        resetSwipedPosition()
                    }
                }
                return false
            }
        })
    }

    // 重置滑动状态
    fun resetSwipedPosition() {
        if (currentSwipedPosition != -1) {
            val previousPosition = currentSwipedPosition
            currentSwipedPosition = -1
            notifyItemChanged(previousPosition)
        }
    }

    // dp 转 px 扩展函数
    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    // DiffUtil 回调（优化列表刷新）
    class CommentDiffCallback : DiffUtil.ItemCallback<UserComment>() {
        override fun areItemsTheSame(oldItem: UserComment, newItem: UserComment): Boolean =
            oldItem.userId == newItem.userId && oldItem.commentTime == newItem.commentTime

        override fun areContentsTheSame(oldItem: UserComment, newItem: UserComment): Boolean =
            oldItem.content == newItem.content &&
                    oldItem.username == newItem.username &&
                    oldItem.avatar == newItem.avatar &&
                    oldItem.commentTime == newItem.commentTime
    }
}