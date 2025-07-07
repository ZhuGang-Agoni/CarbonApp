package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.R

class TravelRecordAdapter(
    private val recordList: List<TravelRecord>
) : RecyclerView.Adapter<TravelRecordAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivModeIcon: ImageView = itemView.findViewById(R.id.ivModeIcon)
        val tvModeName: TextView = itemView.findViewById(R.id.tvModeName)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = recordList[position]
        holder.ivModeIcon.setImageResource(record.modeIcon)
        holder.tvModeName.text = record.modeName
        holder.tvDistance.text = record.distance
        holder.tvTime.text = record.time
    }

    override fun getItemCount(): Int = recordList.size
} 