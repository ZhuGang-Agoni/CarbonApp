package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity // 新增：用于获取FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.Rarity
import com.zg.carbonapp.Dao.VirtualProduct
//import com.zg.carbonapp.Dialog.AvatarPreviewDialog // 导入对话框
import com.zg.carbonapp.MMKV.UserMMKV // 导入UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.AvatarPreviewDialog
import com.zg.carbonapp.databinding.ItemProductHorizontalBinding

class NewProductAdapter(
    private val context: Context,
    private var products: List<VirtualProduct>,
    private val onItemClick: (VirtualProduct) -> Unit
) : RecyclerView.Adapter<NewProductAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProductHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductHorizontalBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            productIcon.setImageResource(product.iconRes)
            productName.text = product.name
            productDescription.text = product.description
            productPoints.text = product.points.toString()
            rarityText.text = when (product.rarity) {
                Rarity.COMMON -> "普通"
                Rarity.RARE -> "稀有"
                Rarity.EPIC -> "史诗"
                Rarity.LEGENDARY -> "传说"
            }

            // 设置稀有度颜色
            rarityText.setTextColor(getRarityColor(product.rarity))

            // ---------------------- 预览按钮逻辑（修改部分）----------------------
            previewButton.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_scale))

                // 1. 从UserMMKV获取用户信息
                val user = UserMMKV.getUser()
                if (user == null) {
                    // 若用户信息为空，提示登录（可根据需求调整）
                    android.widget.Toast.makeText(context, "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (context is FragmentActivity) {
                    AvatarPreviewDialog(
                        user = user,
                        product = product
                    ).show(
                        context.supportFragmentManager,
                        AvatarPreviewDialog.TAG
                    )
                }
            }
            // -------------------------------------------------------------------

            // 兑换按钮（原有逻辑不变）
            exchangeButton.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_scale))
                onItemClick(product)
            }
        }
    }

    override fun getItemCount() = products.size

    // 优化：使用DiffUtil高效更新（原有逻辑不变）
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

    // 获取稀有度颜色（原有逻辑不变）
    private fun getRarityColor(rarity: Rarity): Int {
        return ContextCompat.getColor(context, when (rarity) {
            Rarity.COMMON -> R.color.rarity_common_text
            Rarity.RARE -> R.color.rarity_rare_text
            Rarity.EPIC -> R.color.rarity_epic_text
            Rarity.LEGENDARY -> R.color.rarity_legendary_text
        })
    }
}