package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.R
import java.text.SimpleDateFormat
import java.util.*

class TravelRecordAdapter(
    private val recordList: List<ItemTravelRecord>,
    private val context: Context
) : RecyclerView.Adapter<TravelRecordAdapter.ViewHolder>() {

    // 时间格式化工具（优化为单例模式，线程安全）
    private val dateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivModeIcon: ImageView = itemView.findViewById(R.id.ivModeIcon)
        val tvModeName: TextView = itemView.findViewById(R.id.tvModeName)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvCarbonCount: TextView = itemView.findViewById(R.id.tvCarbon)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordList[position]

        // 修复图片加载：直接使用资源ID，无需Glide
        holder.ivModeIcon.setImageResource(item.modelRavel)

        holder.tvModeName.text = item.travelModel
        holder.tvCarbonCount.text = item.carbonCount
        holder.tvDistance.text = item.distance

        // 修复时间显示：将时间戳格式化为"2025-07-11 10:11"
        holder.tvTime.text = dateFormat.format(Date(item.time))

        // 显示路线（起点→终点）
        holder.tvAddress.text = item.travelRoute
    }

    override fun getItemCount(): Int = recordList.size
}