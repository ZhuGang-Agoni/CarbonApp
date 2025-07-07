package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.R

class GarbageRecordAdapter(
    private val recordList: List<GarbageRecord>
) : RecyclerView.Adapter<GarbageRecordAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        val tvGarbageName: TextView = itemView.findViewById(R.id.tv_garbage_name)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garbage_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = recordList[position]
        
        holder.ivCategoryIcon.setImageResource(record.categoryIcon)
        holder.tvGarbageName.text = record.garbageName
        holder.tvCategoryName.text = record.categoryName
        holder.tvTime.text = record.time
    }

    override fun getItemCount(): Int = recordList.size
} 