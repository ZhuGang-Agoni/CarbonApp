package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.R
import com.zg.carbonapp.model.GameRecord

import java.text.SimpleDateFormat
import java.util.*

class RecordsAdapter(private var records: List<GameRecord>) : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.avatar_image)
        val scoreText: TextView = itemView.findViewById(R.id.score_text)
        val playerTypeText: TextView = itemView.findViewById(R.id.player_type_text)
        val dateText: TextView = itemView.findViewById(R.id.date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // 设置头像
        holder.avatarImage.setImageResource(record.playerAvatar)

        // 设置分数
        holder.scoreText.text = "得分: ${record.score}"

        // 转换角色类型为中文显示
        holder.playerTypeText.text = "角色: ${getPlayerTypeName(record.playerType)}"

        // 设置日期时间
        holder.dateText.text = "时间: ${dateFormat.format(record.date)}"
    }

    override fun getItemCount(): Int = records.size

    /**
     * 更新记录列表并按时间倒序排序（最新的在前）
     */
    fun updateRecords(newRecords: List<GameRecord>) {
        records = newRecords.sortedByDescending { it.date.time }
        notifyDataSetChanged()
    }

    /**
     * 将英文角色类型转换为中文
     */
    private fun getPlayerTypeName(type: String): String {
        return when (type) {
            "kitchen" -> "厨余垃圾桶"
            "recyclable" -> "可回收垃圾桶"
            "hazardous" -> "有害垃圾桶"
            "other" -> "其他垃圾桶"
            else -> type
        }
    }
}
