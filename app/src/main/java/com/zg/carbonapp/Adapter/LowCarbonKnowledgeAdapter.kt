package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.R

class LowCarbonKnowledgeAdapter(
    private val context: Context,
    private var dataList: List<LowCarbonKnowledge>
) : RecyclerView.Adapter<LowCarbonKnowledgeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_knowledge_image)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_knowledge_title)
        val lottieIndicator: LottieAnimationView = itemView.findViewById(R.id.lottie_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_low_carbon_knowledge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val knowledge = dataList[position]

        holder.ivImage.setImageResource(knowledge.imageResId)
        holder.tvTitle.text = knowledge.title

        // 设置Lottie指示器
        if (knowledge.lottieResId != 0) {
            holder.lottieIndicator.setAnimation(knowledge.lottieResId)
            holder.lottieIndicator.playAnimation()
            holder.lottieIndicator.loop(true)
            holder.lottieIndicator.visibility = View.VISIBLE
        } else {
            holder.lottieIndicator.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            showDetailDialog(knowledge)
        }
    }

    override fun getItemCount(): Int = dataList.size

    private fun showDetailDialog(knowledge: LowCarbonKnowledge) {
        // 创建自定义对话框视图
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_knowledge_detail, null)

        val lottieAnimation = dialogView.findViewById<LottieAnimationView>(R.id.lottieAnimation)
        val tvContent = dialogView.findViewById<TextView>(R.id.tvContent)

        // 设置Lottie动画
        if (knowledge.lottieResId != 0) {
            lottieAnimation.setAnimation(knowledge.lottieResId)
            lottieAnimation.playAnimation()
            lottieAnimation.loop(true)
        } else {
            lottieAnimation.visibility = View.GONE
        }

        tvContent.text = knowledge.content

        // 创建对话框
        AlertDialog.Builder(context)
            .setTitle(knowledge.title)
            .setView(dialogView)
            .setPositiveButton("关闭") { dialog, _ ->
                dialog.dismiss()
                lottieAnimation.cancelAnimation() // 停止动画释放资源
            }
            .show()
    }

    fun updateData(newData: List<LowCarbonKnowledge>) {
        dataList = newData
        notifyDataSetChanged()
    }
}