package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.R

class RankingAdapter (val itemList:List<RankingItem>,context : Context):RecyclerView.Adapter<RankingAdapter.ViewHolder>(){
    inner class ViewHolder(view : View):RecyclerView.ViewHolder(view){
        val userName=view.findViewById<TextView>(R.id.user_name)
        val userId=view.findViewById<TextView>(R.id.ranking_id)
        val userEvator=view.findViewById<ImageView>(R.id.user_evator)
        val carbonCount=view.findViewById<TextView>(R.id.carbon_count)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view= LayoutInflater.from(parent.context).inflate(R.layout.rank_list_item,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
          val item=itemList[position]
          holder.userId.text=item.rank.toString()
          Glide.with(holder.itemView.context).load(item.userEvator).into(holder.userEvator)

          holder.carbonCount.text=item.carbonCount.toString()
          holder.userName.text=item.userName
    }

}