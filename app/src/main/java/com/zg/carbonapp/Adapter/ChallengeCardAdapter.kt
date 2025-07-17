package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.FeatureCard
import com.zg.carbonapp.R
import com.bumptech.glide.Glide
import java.io.File

class ChallengeCardAdapter(
    private val data: List<FeatureCard>,
    private val onClick: (FeatureCard) -> Unit
) : RecyclerView.Adapter<ChallengeCardAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivCardImage)
        val title: TextView = view.findViewById(R.id.tvCardTitle)
        val desc: TextView = view.findViewById(R.id.tvCardDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = data[position]
        val imageUrl = card.imageUrl
        if (imageUrl.startsWith("file://") || imageUrl.startsWith("/")) {
            // 加载本地图片
            Glide.with(holder.image.context)
                .load(if (imageUrl.startsWith("file://")) imageUrl else File(imageUrl))
                .placeholder(R.drawable.ic_other)
                .into(holder.image)
        } else {
            // 加载资源图片
            val context = holder.image.context
            val resId = context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
            if (resId != 0) {
                holder.image.setImageResource(resId)
            } else {
                holder.image.setImageResource(R.drawable.ic_other)
            }
        }
        holder.title.text = card.title
        holder.desc.text = card.description
        holder.itemView.setOnClickListener { onClick(card) }
    }
} 