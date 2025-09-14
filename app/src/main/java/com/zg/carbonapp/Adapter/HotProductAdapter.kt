package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.filament.Colors
import com.zg.carbonapp.Dao.Rarity
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ItemVirtualProductBinding

class HotProductAdapter(
    private val context: Context,
    private var products: List<VirtualProduct>,
    private val onItemClick: (VirtualProduct) -> Unit
) : RecyclerView.Adapter<HotProductAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemVirtualProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVirtualProductBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            // 设置商品数据
            productIcon.setImageResource(product.iconRes)
            productName.text = product.name
            productDescription.text = product.description
            productPoints.text = product.points.toString()

            // 修复：使用MaterialCardView自带方法设置背景色（避免类型转换）
            root.setCardBackgroundColor(getRarityColor(product.rarity))

            // 设置边框颜色
            root.strokeColor = ContextCompat.getColor(context, R.color.eco_primary_green)

            // 添加特效标识
            effectIndicator.visibility = if (product.effectRes != 0) View.VISIBLE else View.GONE
            if (product.effectRes != 0) {
                effectIndicator.setImageResource(R.drawable.ic_leaf)
            }

            // 点击动画
            root.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_scale))
                onItemClick(product)
            }
        }
    }

    override fun getItemCount() = products.size

    // 优化：使用DiffUtil高效更新列表
    fun updateList(newList: List<VirtualProduct>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = products.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                products[oldPos].id == newList[newPos].id

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                products[oldPos] == newList[newPos]
        })
        products = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private fun getRarityColor(rarity: Rarity): Int {
        return when (rarity) {
            Rarity.COMMON -> ContextCompat.getColor(context, R.color.eco_rare)
            Rarity.RARE -> ContextCompat.getColor(context, R.color.eco_epic)
            Rarity.EPIC -> ContextCompat.getColor(context, R.color.eco_epic)
            Rarity.LEGENDARY -> ContextCompat.getColor(context, R.color.eco_legendary)
        }
    }
}