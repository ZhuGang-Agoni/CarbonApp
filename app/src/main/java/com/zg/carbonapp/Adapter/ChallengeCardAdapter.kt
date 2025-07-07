package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Fragment.ChallengeFragment.ChallengeCard
import com.zg.carbonapp.R

class ChallengeCardAdapter(
    private val data: List<ChallengeCard>,
    private val onClick: (ChallengeCard) -> Unit
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
        holder.image.setImageResource(card.imageResId)
        holder.title.text = card.title
        holder.desc.text = card.description
        holder.itemView.setOnClickListener { onClick(card) }
    }
} 