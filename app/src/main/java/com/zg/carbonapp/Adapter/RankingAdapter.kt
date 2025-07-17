package com.zg.carbonapp.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.R

class RankingAdapter(
    // 1. 改为接收不可变列表，内部转换为可变列表（核心修改）
    itemList: List<RankingItem>,
    context: Context
) : RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    // 2. 内部用可变列表存储数据（核心修改）
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

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.userId.text = item.rank.toString()

        holder.carbonCount.text = item.carbonCount.toString()
        holder.userName.text = item.userName

//        Glide.with(holder.itemView.context)
//            .load(item.userEvator)            // 加载头像（URL/资源ID）
//            .error(R.drawable.default_avatar)       // 加载失败头像
//
//            .into(holder.userEvator)
        try {
            val parts = item.userEvator.split(".")
            if (parts.size == 3 && parts[0] == "R" && parts[1] == "drawable") {
                val resName = parts[2]
                val resId = holder.itemView.context.resources.getIdentifier(
                    resName, "drawable", holder.itemView.context.packageName
                )
                if (resId != 0) {
                    holder.userEvator.setImageResource(resId)
                    return
                }
            }
        } catch (e: Exception) {
            // 失败时显示默认头像
            holder.userEvator.setImageResource(R.drawable.default_avatar)
            Log.e("RankingAdapter", "解析资源失败: ${item.userEvator}", e)
        }



        holder.itemView.setOnClickListener {
            // 点击事件逻辑
        }
        // 在Adapter的onBindViewHolder中添加
        when (item.rank) {
            1 -> holder.userId.setTextColor(holder.itemView.context.getColor(R.color.gold))
            2 -> holder.userId.setTextColor(holder.itemView.context.getColor(R.color.silver))
            3 -> holder.userId.setTextColor(holder.itemView.context.getColor(R.color.bronze))
            else -> holder.userId.setTextColor(holder.itemView.context.getColor(R.color.black))
        }
    }

    // 3. 修复更新数据方法（现在可修改内部可变列表）
    fun updateData(newList: List<RankingItem>) {
        itemList.clear() // 现在不会报错了
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
}