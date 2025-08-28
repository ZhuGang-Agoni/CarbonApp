package com.zg.carbonapp.Adapter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.R
import android.graphics.drawable.Drawable
import androidx.fragment.app.FragmentActivity

class RankingAdapter(
    itemList: List<RankingItem>,
    private val context: Context
) : RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    private val itemList: MutableList<RankingItem> = itemList.toMutableList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName = view.findViewById<TextView>(R.id.user_name)
        val userId = view.findViewById<TextView>(R.id.ranking_id)
        val userEvator = view.findViewById<ImageView>(R.id.user_evator)
        val carbonCount = view.findViewById<TextView>(R.id.carbon_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rank_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.userId.text = item.rank.toString()
        holder.carbonCount.text = item.carbonCount.toString()
        holder.userName.text = item.userName

        // 加载用户头像（对齐 CommunityFeedAdapter 逻辑）
        loadAvatar(holder.userEvator, item.userEvator, item.userName)

        // 排名颜色
        when (item.rank) {
            1 -> holder.userId.setTextColor(ContextCompat.getColor(context, R.color.gold))
            2 -> holder.userId.setTextColor(ContextCompat.getColor(context, R.color.silver))
            3 -> holder.userId.setTextColor(ContextCompat.getColor(context, R.color.bronze))
            else -> holder.userId.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    // 与 CommunityFeedAdapter 对齐的头像加载逻辑
    private fun loadAvatar(imageView: ImageView, avatarUrl: String?, username: String) {
        // 先设置默认头像兜底
        imageView.setImageResource(R.drawable.default_avatar)

        if (avatarUrl.isNullOrEmpty()) {
            Log.w("AvatarLoad", "用户[$username] - 头像URL为空，显示默认头像")
            return
        }

        // 权限处理：针对 Android 13（TIRAMISU）以下版本，处理 READ_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // 若在 FragmentActivity 环境中，请求权限
                if (context is FragmentActivity) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        10086
                    )
                } else {
                    Log.w("AvatarLoad", "用户[$username] - 无读取存储权限且非 FragmentActivity 环境，无法加载头像")
                    return
                }
            }
        }

        // 使用 Glide 加载，配置与 CommunityFeedAdapter 一致的监听
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
            .into(imageView)
    }

    fun updateData(newList: List<RankingItem>) {
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
        Log.d("RankingAdapter", "数据已更新，数量: ${itemList.size}")
    }
}