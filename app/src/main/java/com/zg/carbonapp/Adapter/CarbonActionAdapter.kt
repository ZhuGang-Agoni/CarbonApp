package com.zg.carbonapp.Adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Entity.CarbonAction
import com.zg.carbonapp.databinding.ItemCarbonActionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CarbonActionAdapter : ListAdapter<CarbonAction, CarbonActionAdapter.ViewHolder>(DiffCallback) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    inner class ViewHolder(private val binding: ItemCarbonActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: CarbonAction) {
            binding.tvProductName.text = action.productName
            binding.tvAction.text = action.action
            binding.tvReducedCarbon.text = String.format("减碳: %.2f kg", action.reducedCarbon)
            binding.tvTime.text = dateFormat.format(action.actionTime)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarbonActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CarbonAction>() {
        override fun areItemsTheSame(oldItem: CarbonAction, newItem: CarbonAction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CarbonAction, newItem: CarbonAction): Boolean {
            return oldItem == newItem
        }
    }
}