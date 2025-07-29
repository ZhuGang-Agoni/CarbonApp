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

        // 设置出行方式图标
        holder.ivModeIcon.setImageResource(
            item.modelRavel)

        holder.tvModeName.text = getTravelModeName(item.travelModel)
        holder.tvCarbonCount.text = "${item.carbonCount} g"
        holder.tvDistance.text = "${item.distance} km"

        // 格式化时间显示
        holder.tvTime.text = dateFormat.format(Date(item.time))

        // 显示路线（起点→终点）
        holder.tvAddress.text = item.travelRoute
    }

    // 将出行方式代码转换为中文名称
    private fun getTravelModeName(modeCode: String): String {
        return when (modeCode) {
            "walk" -> "步行"
            "ride" -> "骑行"
            "bus" -> "公交"
            "subway" -> "地铁"
            else -> modeCode
        }
    }

    // 更新数据列表
    fun updateList(newList: List<ItemTravelRecord>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = recordList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // 使用时间戳作为唯一标识判断是否是同一个Item
                return recordList[oldItemPosition].time == newList[newItemPosition].time
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = recordList[oldItemPosition]
                val newItem = newList[newItemPosition]
                // 比较所有字段是否相同
                return oldItem.travelModel == newItem.travelModel &&
                        oldItem.carbonCount == newItem.carbonCount &&
                        oldItem.distance == newItem.distance &&
                        oldItem.travelRoute == newItem.travelRoute
            }
        })

        // 更新数据并应用差异
        recordList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = recordList.size
}