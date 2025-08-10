package com.zg.carbonapp.Adapter

import android.app.AlertDialog
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
import com.zg.carbonapp.databinding.DialogRouteDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class TravelRecordAdapter(
    private var recordList: List<ItemTravelRecord>,
    private val context: Context // 持有上下文，用于创建对话框
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
        val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordList[position]

        // 设置图标
        try {
            holder.ivModeIcon.setImageResource(item.modelRavel)
        } catch (e: Exception) {
            holder.ivModeIcon.setImageResource(R.drawable.walk)
        }

        // 绑定数据
        holder.tvModeName.text = item.travelModel
        holder.tvCarbonCount.text = "减碳 ${item.carbonCount}g"
        holder.tvDistance.text = item.distance
        holder.tvTime.text = dateFormat.format(Date(item.time))
        holder.tvAddress.text = item.travelRoute // 地址会多行完全显示

        // 点击事件：在Adapter内部处理，直接弹出对话框
        holder.itemView.setOnClickListener {
            showRouteDetailDialog(item)
        }
    }

    // 显示路线详情对话框（Adapter内部实现）
    private fun showRouteDetailDialog(record: ItemTravelRecord) {
        // 加载对话框布局
        val inflater = LayoutInflater.from(context)
        val dialogBinding = DialogRouteDetailBinding.inflate(inflater)

        // 设置对话框内容（使用记录中保存的路线标题和描述）
        dialogBinding.routeTitle.text = record.routeTitle
        dialogBinding.routeDescription.text = record.routeDescription

        // 创建并显示对话框
        AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
                        oldItem.travelRoute == newItem.travelRoute &&
                        oldItem.routeTitle == newItem.routeTitle &&
                        oldItem.routeDescription == newItem.routeDescription
            }
        })

        recordList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = recordList.size
}