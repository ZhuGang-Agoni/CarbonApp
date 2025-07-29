package com.zg.carbonapp.Adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.R

class LowCarbonKnowledgeAdapter(
    private val context: Context,
    private val dataList: List<LowCarbonKnowledge>
) : RecyclerView.Adapter<LowCarbonKnowledgeAdapter.ViewHolder>() {

    // 子项ViewHolder
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_knowledge_image)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_knowledge_title)
    }

    // 创建ViewHolder（加载子项布局）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_low_carbon_knowledge, parent, false)
        return ViewHolder(view)
    }

    // 绑定数据到ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val knowledge = dataList[position]

        // 设置图片和标题
        holder.ivImage.setImageResource(knowledge.imageResId)
        holder.tvTitle.text = knowledge.title

        // 子项点击事件：弹出详情对话框
        holder.itemView.setOnClickListener {
            showDetailDialog(knowledge)
        }
    }

    // 数据总数
    override fun getItemCount(): Int = dataList.size

    // 显示详情对话框
    private fun showDetailDialog(knowledge: LowCarbonKnowledge) {
        AlertDialog.Builder(context)
            .setTitle(knowledge.title)           // 标题
            .setMessage(knowledge.content)       // 详细内容
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}