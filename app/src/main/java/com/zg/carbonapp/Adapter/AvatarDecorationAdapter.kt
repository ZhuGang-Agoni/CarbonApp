package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.databinding.ItemAvatarDecorationBinding

class AvatarDecorationAdapter(
    private val decorations: List<VirtualProduct>,
    private val currentSelectedId: Int,
    private val onItemClick: (VirtualProduct) -> Unit
) : RecyclerView.Adapter<AvatarDecorationAdapter.DecorationViewHolder>() {

    inner class DecorationViewHolder(val binding: ItemAvatarDecorationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DecorationViewHolder {
        return DecorationViewHolder(
            ItemAvatarDecorationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DecorationViewHolder, position: Int) {
        val decoration = decorations[position]
        val isSelected = decoration.id == currentSelectedId

        with(holder.binding) {
            // 绑定数据
            ivDecorationIcon.setImageResource(decoration.iconRes)
            tvDecorationName.text = decoration.name
            tvDecorationDesc.text = decoration.description

            // 设置选中状态
            cbSelected.isChecked = isSelected
            cbSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            // 点击事件
            root.setOnClickListener { onItemClick(decoration) }
        }
    }

    override fun getItemCount() = decorations.size
}