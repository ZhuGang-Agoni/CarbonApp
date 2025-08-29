package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.GarbageCategory
import com.zg.carbonapp.R

class GarbageGuideAdapter(
    private var category: GarbageCategory
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 数据类型：标题、标准、例子、误区
    private enum class ViewType { HEADER, STANDARD, EXAMPLE, MISUNDERSTANDING }

    // 原始展示例子（默认显示精简的displayExamples）
    private var originalDisplayExamples = category.displayExamples
    // 完整例子（用于搜索时展示匹配结果）
    private var fullExamples = category.fullExamples
    // 当前展示的例子列表（默认是精简例子，搜索时切换为匹配的完整例子）
    private var currentExamples = originalDisplayExamples

    // 更新整个分类数据（切换标签页时调用）
    fun updateCategory(newCategory: GarbageCategory) {
        category = newCategory
        originalDisplayExamples = newCategory.displayExamples // 切换为新分类的精简例子
        fullExamples = newCategory.fullExamples // 更新完整例子
        currentExamples = originalDisplayExamples // 重置为精简展示
        notifyDataSetChanged()
    }

    // 更新例子列表（搜索时调用，展示匹配的完整例子）
    fun updateExamples(filteredExamples: List<String>) {
        currentExamples = filteredExamples
        notifyDataSetChanged()
    }

    // 计算item总数：1（标题） + 1（标准） + 例子数量 + 1（误区标题） + 误区数量
    override fun getItemCount(): Int {
        return 1 + 1 + currentExamples.size + 1 + category.misunderstandings.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewType.HEADER.ordinal
            1 -> ViewType.STANDARD.ordinal
            in 2 until (2 + currentExamples.size) -> ViewType.EXAMPLE.ordinal
            2 + currentExamples.size -> ViewType.MISUNDERSTANDING.ordinal // 误区标题
            else -> ViewType.MISUNDERSTANDING.ordinal // 误区内容
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.HEADER.ordinal -> {
                val view = inflater.inflate(R.layout.item_garbage_guide_header, parent, false)
                HeaderViewHolder(view)
            }
            ViewType.STANDARD.ordinal -> {
                val view = inflater.inflate(R.layout.item_garbage_standard, parent, false)
                StandardViewHolder(view)
            }
            ViewType.EXAMPLE.ordinal -> {
                val view = inflater.inflate(R.layout.item_garbage_example, parent, false)
                ExampleViewHolder(view)
            }
            ViewType.MISUNDERSTANDING.ordinal -> {
                val view = inflater.inflate(R.layout.item_garbage_misunderstanding, parent, false)
                MisunderstandingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.ivIcon.setImageResource(category.icon)
                holder.tvName.text = category.name
            }
            is StandardViewHolder -> {
                holder.tvStandard.text = category.standard
            }
            is ExampleViewHolder -> {
                val exampleIndex = position - 2 // 例子从position=2开始
                holder.tvExample.text = currentExamples[exampleIndex]
                holder.tvNumber.text = "${exampleIndex + 1}." // 序号：1. 2. ...
            }
            is MisunderstandingViewHolder -> {
                val misunderstandingStartPos = 2 + currentExamples.size
                if (position == misunderstandingStartPos) {
                    // 误区标题
                    holder.itemView.findViewById<TextView>(R.id.tv_misunderstanding_title).visibility = View.VISIBLE
                    holder.itemView.findViewById<TextView>(R.id.tv_content).visibility = View.GONE
                } else {
                    // 误区内容
                    holder.itemView.findViewById<TextView>(R.id.tv_misunderstanding_title).visibility = View.GONE
                    val misunderstandingIndex = position - (misunderstandingStartPos + 1)
                    holder.itemView.findViewById<TextView>(R.id.tv_content).text =
                        category.misunderstandings[misunderstandingIndex]
                }
            }
        }
    }

    // 标题ViewHolder（类别名称+图标）
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
    }

    // 分类标准ViewHolder
    inner class StandardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStandard: TextView = itemView.findViewById(R.id.tv_standard)
    }

    // 例子ViewHolder
    inner class ExampleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tv_example_number)
        val tvExample: TextView = itemView.findViewById(R.id.tv_example_content)
    }

    // 误区ViewHolder（包含标题和内容）
    inner class MisunderstandingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}