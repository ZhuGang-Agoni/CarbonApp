package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.FriendRanking
import com.zg.carbonapp.R

class FriendRankingAdapter(
    private val friends: List<FriendRanking>
) : RecyclerView.Adapter<FriendRankingAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvNickname: TextView = view.findViewById(R.id.tvNickname)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvTreeCount: TextView = view.findViewById(R.id.tvTreeCount)
        val ivTree: ImageView = view.findViewById(R.id.ivTree)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.tvRank.text = (position + 1).toString()
        holder.ivAvatar.setImageResource(friend.avatarResId)
        holder.tvNickname.text = friend.nickname
        holder.tvDesc.text = "一起低碳种树"
        holder.tvTreeCount.text = friend.treeCount.toString()
        holder.ivTree.setImageResource(R.drawable.ic_tree
        )

        // 设置排名圆形背景色
        val context = holder.itemView.context
        val bgDrawable = when (position) {
            0 -> ContextCompat.getDrawable(context, R.drawable.bg_rank_gold)
            1 -> ContextCompat.getDrawable(context, R.drawable.bg_rank_silver)
            2 -> ContextCompat.getDrawable(context, R.drawable.bg_rank_bronze)
            else -> ContextCompat.getDrawable(context, R.drawable.bg_rank_circle)
        }
        holder.tvRank.background = bgDrawable
    }

    override fun getItemCount(): Int = friends.size
} 