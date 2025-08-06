package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.R
import java.text.SimpleDateFormat
import java.util.*

class TravelRecordAdapter(
    private var recordList: List<ItemTravelRecord>,
    private val context: Context
) : RecyclerView.Adapter<TravelRecordAdapter.ViewHolder>() {

    // 时间格式化工具
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

        // 关键修复：设置图片前先校验资源有效性
        try {
            // 使用保存的图片资源ID设置图标
            holder.ivModeIcon.setImageResource(item.modelRavel)
        } catch (e: Exception) {
            // 异常处理：如果资源无效，使用默认图标
            holder.ivModeIcon.setImageResource(R.drawable.walk)
        }

        holder.tvModeName.text = item.travelModel // 直接使用已转换的中文名称
        holder.tvCarbonCount.text = "减碳 ${item.carbonCount}g"
        holder.tvDistance.text = item.distance // 直接使用格式化好的距离（如"1.2公里"）
        holder.tvTime.text = dateFormat.format(Date(item.time))
        holder.tvAddress.text = item.travelRoute
    }

    // 更新数据列表
    fun updateList(newList: List<ItemTravelRecord>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = recordList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return recordList[oldItemPosition].time == newList[newItemPosition].time
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = recordList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem.travelModel == newItem.travelModel &&
                        oldItem.carbonCount == newItem.carbonCount &&
                        oldItem.distance == newItem.distance &&
                        oldItem.travelRoute == newItem.travelRoute
            }
        })

        recordList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = recordList.size
}