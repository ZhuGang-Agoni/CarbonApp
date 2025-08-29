package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Activity.GarbageReportActivity
import com.zg.carbonapp.databinding.ItemGarbageCategoryDetailBinding

class GarbageCategoryAdapter(
    private val categoryDetails: List<GarbageReportActivity.CategoryDetail>
) : RecyclerView.Adapter<GarbageCategoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGarbageCategoryDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(detail: GarbageReportActivity.CategoryDetail) {
            binding.categoryName.text = detail.name
            binding.categoryCount.text = "${detail.count}次"
            binding.categoryPercentage.text = "${detail.percentage}%"
            binding.categoryColor.setBackgroundTintList(
                itemView.context.resources.getColorStateList(detail.colorRes, null)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGarbageCategoryDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categoryDetails[position])
    }

    override fun getItemCount() = categoryDetails.size
}
