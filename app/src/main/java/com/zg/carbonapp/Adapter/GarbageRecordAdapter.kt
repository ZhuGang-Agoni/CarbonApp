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
    private val recordList: List<GarbageRecord>,
    private val onItemClick: ((GarbageRecord) -> Unit)? = null
) : RecyclerView.Adapter<GarbageRecordAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRecordIcon: ImageView = itemView.findViewById(R.id.iv_record_icon)
        val tvRecordTitle: TextView = itemView.findViewById(R.id.tv_record_title)
        val tvRecordDetail: TextView = itemView.findViewById(R.id.tv_record_detail)
        val tvRecordTime: TextView = itemView.findViewById(R.id.tv_record_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garbage_record_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = recordList[position]
        
        holder.ivRecordIcon.setImageResource(record.categoryIcon)
        holder.tvRecordTitle.text = record.garbageName
        holder.tvRecordDetail.text = record.categoryName
        holder.tvRecordTime.text = record.time

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(record)
        }
    }

    override fun getItemCount(): Int = recordList.size
} 