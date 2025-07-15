package com.zg.carbonapp.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.Product
import com.zg.carbonapp.R
//这个是替代的意思
class AlternativeAdapter(
    private val alternatives: List<Product>,
    private val originalCarbon: Double  // 原商品碳足迹
) : RecyclerView.Adapter<AlternativeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvAlternativeName)
        val tvCarbon: TextView = itemView.findViewById(R.id.tvAlternativeCarbon)
        val tvSavings: TextView = itemView.findViewById(R.id.tvCarbonSavings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alternative, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = alternatives[position]
        holder.tvName.text = product.name
        holder.tvCarbon.text = "碳足迹：${product.carbonFootprint} kgCO₂e/${product.unit}"

        // 计算减排比例（修复负数判断）
        val saving = originalCarbon - product.carbonFootprint
        val savingRate = if (originalCarbon > 0) (saving / originalCarbon * 100).toInt() else 0
        holder.tvSavings.text = "减少碳排放 $savingRate%"
        holder.tvSavings.setTextColor(if (savingRate > 0) Color.GREEN else Color.RED)
    }

    override fun getItemCount() = alternatives.size
}