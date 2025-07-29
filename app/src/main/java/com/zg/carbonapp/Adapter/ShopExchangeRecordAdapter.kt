package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.ShopRecord
import com.zg.carbonapp.R
import java.text.SimpleDateFormat
import java.util.*

class ShopExchangeRecordAdapter(
    val context: Context,
    var list: List<ShopRecord>
) : RecyclerView.Adapter<ShopExchangeRecordAdapter.ViewHolder>() {

    private val itemList: MutableList<ShopRecord> = list.toMutableList()

    // 时间格式化工具（输入格式：与ShopRecord的time字段格式一致）
    private val inputFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) // 假设原始时间格式是这个
    }

    // 输出格式（展示用）
    private val outputFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) // 可根据需要修改
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time = view.findViewById<TextView>(R.id.shop_time)
        val name = view.findViewById<TextView>(R.id.shop_name)
        val point = view.findViewById<TextView>(R.id.shop_point)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.point.text = "所需积分：${item.shopPoint}"
        holder.name.text = "商品名称：${item.shopName}"

        // 修复时间格式化逻辑
        holder.time.text = "兑换时间：${formatTime(item.time)}"
    }

    override fun getItemCount(): Int = itemList.size

    // 格式化时间（核心修复）
    private fun formatTime(timeStr: Long): String {
       return outputFormat.format(Date(timeStr))
    }

    // 清空记录
    fun clearLog() {
        itemList.clear()
        notifyDataSetChanged()
    }

    // 更新数据列表
    fun updateList(newList: List<ShopRecord>) {
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
}