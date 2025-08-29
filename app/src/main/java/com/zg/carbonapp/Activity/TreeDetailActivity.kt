package com.zg.carbonapp.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.DB.TreeDao
import com.zg.carbonapp.DB.TreeDatabase
import com.zg.carbonapp.DB.TreeEntity
import com.zg.carbonapp.Dao.GrowthStage
import com.zg.carbonapp.Dao.Tree
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.TreeUtils
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TreeDetailActivity : AppCompatActivity() {
    private lateinit var treeDao: TreeDao
    private var tree: TreeEntity? = null
    private lateinit var timelineAdapter: TimelineAdapter

    companion object {
        private const val EXTRA_TREE_ID = "tree_id"
        fun getIntent(context: Context, treeId: String): Intent {
            return Intent(context, TreeDetailActivity::class.java).apply {
                putExtra(EXTRA_TREE_ID, treeId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_detail)

        val treeId = intent.getStringExtra(EXTRA_TREE_ID) ?: run {
            finish()
            return
        }

        treeDao = TreeDatabase.getInstance(this).treeDao()
        timelineAdapter = TimelineAdapter()

        // 初始化时间轴
        findViewById<RecyclerView>(R.id.rv_timeline).apply {
            layoutManager = LinearLayoutManager(this@TreeDetailActivity)
            adapter = timelineAdapter
        }

        // 返回按钮
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }

        // 浇水按钮
        findViewById<Button>(R.id.btn_water).setOnClickListener {
            tree?.let {
                lifecycleScope.launch {
                    val updated = it.copy(lastWaterTime = System.currentTimeMillis())
                    treeDao.updateTree(updated)
                    loadTree(treeId) // 刷新数据
                }
            }
        }

        // 加载树数据
        loadTree(treeId)
    }

    private fun loadTree(treeId: String) {
        lifecycleScope.launch {
            tree = treeDao.getAllTrees().firstOrNull { it.id == treeId } ?: run {
                finish()
                return@launch
            }
            updateUI()
        }
    }

    private fun updateUI() {
        val tree = tree ?: return
        val treeObj = Tree(
            id = tree.id,
            treeType = tree.treeType,
            plantTime = tree.plantTime,
            lastWaterTime = tree.lastWaterTime
        )
        val growthDays = TreeUtils.calculateGrowthDays(treeObj)
        val currentStage = GrowthStage.getStage(growthDays)
        val timeline = TreeUtils.getStageTimeline(treeObj)

        // 更新标题
        findViewById<TextView>(R.id.tv_title).text = "${tree.treeType} 成长档案"

        // 更新当前阶段
        findViewById<ImageView>(R.id.iv_current_stage).setImageResource(currentStage.iconRes)
        findViewById<TextView>(R.id.tv_current_title).text = currentStage.title
        findViewById<TextView>(R.id.tv_current_desc).text = currentStage.description

        // 更新时间轴
        timelineAdapter.submitList(timeline, currentStage)
    }

    // 时间轴适配器
    inner class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {
        private var items = emptyList<Pair<GrowthStage, String>>()
        private var currentStage: GrowthStage = GrowthStage.SEED

        fun submitList(list: List<Pair<GrowthStage, String>>, current: GrowthStage) {
            items = list
            currentStage = current
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivStage: ImageView = itemView.findViewById(R.id.iv_stage)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            val tvDate: TextView = itemView.findViewById(R.id.tv_date)
            val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
            val viewLine: View = itemView.findViewById(R.id.view_line)
            val viewDot: View = itemView.findViewById(R.id.view_dot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (stage, date) = items[position]
            val isCurrent = stage == currentStage
            val isPast = stage.minDays <= currentStage.minDays

            // 设置图标和颜色（已过阶段用深色，未到用浅色）
            holder.ivStage.setImageResource(stage.iconRes)
            holder.ivStage.alpha = if (isPast) 1.0f else 0.5f

            // 标题和日期
            holder.tvTitle.text = stage.title
            holder.tvDate.text = "日期：$date"
            holder.tvDesc.text = stage.description
            holder.tvDesc.visibility = if (isCurrent) View.VISIBLE else View.GONE

            // 时间轴样式（当前阶段用深色圆点和线条）
            holder.viewDot.setBackgroundResource(
                if (isCurrent) R.drawable.dot_current else R.drawable.dot_normal
            )
            holder.viewLine.setBackgroundColor(
                if (position != items.lastIndex && isPast) {
                    resources.getColor(R.color.forest_timeline)
                } else {
                    resources.getColor(android.R.color.transparent)
                }
            )
        }

        override fun getItemCount() = items.size
    }
}