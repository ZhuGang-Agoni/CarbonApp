package com.zg.carbonapp.Adapter

// HotProductAdapter.kt
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.ExchangeProduct
import com.zg.carbonapp.databinding.ItemProductCardBinding


class HotProductAdapter(
    private val context: Context,
    private val hotProducts: List<ExchangeProduct>,
    private val onItemClick: (ExchangeProduct) -> Unit // 点击回调
) : RecyclerView.Adapter<HotProductAdapter.ViewHolder>() {

    // 绑定item布局
    inner class ViewHolder(val binding: ItemProductCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = hotProducts[position]
        holder.binding.apply {
            // 绑定商品数据到item布局
            productImage.setImageResource(product.imageRes)
            productName.text = product.name
            productDescription.text = product.description
            productPoints.text = product.points.toString()

            // 商品item点击
//            root.setOnClickListener { onItemClick(product) }
            // 兑换按钮点击
            exchangeButton.setOnClickListener { onItemClick(product) }
        }
    }

    override fun getItemCount() = hotProducts.size
}