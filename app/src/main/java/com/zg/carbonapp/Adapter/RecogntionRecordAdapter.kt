package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.R
import java.io.File

class RecognitionRecordAdapter(
    private val records: List<GarbageRecord>,
    private val onItemClick: (GarbageRecord) -> Unit = {}
) : RecyclerView.Adapter<RecognitionRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.iv_photo)
        val tvTitle: TextView = view.findViewById(R.id.tv_record_title)
        val tvDetail: TextView = view.findViewById(R.id.tv_record_detail)
        val tvTime: TextView = view.findViewById(R.id.tv_record_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garbage_record_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        holder.tvTitle.text = record.garbageName
        holder.tvDetail.text = record.categoryName
        holder.tvTime.text = record.time

        // 显示照片
        if (!record.imagePath.isNullOrEmpty()) {
            // 异步加载照片
            val options = RequestOptions()
                .override(100, 100)
                .centerCrop()

            Glide.with(holder.itemView.context)
                .load(File(record.imagePath))
                .apply(options)
                .into(holder.ivPhoto)
        } else {
            // 显示默认图标
            holder.ivPhoto.setImageResource(record.categoryIcon)
        }

        // 设置点击监听器
        holder.itemView.setOnClickListener { onItemClick(record) }
    }

    override fun getItemCount() = records.size
}