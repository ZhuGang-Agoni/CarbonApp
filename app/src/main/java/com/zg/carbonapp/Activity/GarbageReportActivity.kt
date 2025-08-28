package com.zg.carbonapp.Activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.zg.carbonapp.Adapter.GarbageCategoryAdapter
import com.zg.carbonapp.Dao.RecognitionRecord
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.databinding.ActivityGarbageReportBinding
import com.zg.carbonapp.databinding.ItemSuggestionBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class GarbageReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGarbageReportBinding
    private lateinit var weeklyRecords: List<RecognitionRecord>
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var deepSeekHelper: DeepSeekHelper // DeepSeek助手实例

    // 垃圾类别统计数据
    private val categoryCountMap = mutableMapOf<String, Int>()
    private var totalCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGarbageReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化DeepSeek助手
        deepSeekHelper = DeepSeekHelper()

        // 初始化ToolBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "垃圾分类周报"

        dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)

        // 获取上周记录
        weeklyRecords = intent.getParcelableArrayListExtra<RecognitionRecord>("weekly_records") as List<RecognitionRecord>
        processRecords()

        // 设置日期范围
        setupDateRange()

        // 填充概览数据
        fillOverviewData()

        // 绘制图表
        drawCategoryChart()

        // 设置分类详情列表
        setupCategoryDetailList()

        // 生成并显示建议（改为API流式输出）
        generateSuggestionsWithAI()
    }

    // 处理记录数据
    private fun processRecords() {
        weeklyRecords.forEach { record ->
            // 统计各类别数量
            categoryCountMap[record.category] = (categoryCountMap[record.category] ?: 0) + 1
            totalCount++
        }
    }

    // 设置日期范围
    private fun setupDateRange() {
        if (weeklyRecords.isEmpty()) return

        // 按时间排序找到最早和最晚的记录
        val sortedRecords = weeklyRecords.sortedBy { it.timestamp }
        val startDate = Date(sortedRecords.first().timestamp)
        val endDate = Date(sortedRecords.last().timestamp)

        binding.dateRange.text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
    }

    // 填充概览数据
    private fun fillOverviewData() {
        binding.totalCount.text = totalCount.toString()
        binding.categoryCount.text = categoryCountMap.size.toString()
        binding.accuracyRate.text = "92%" // 示例数据，实际应根据正确分类数计算
    }

    // 绘制分类占比图表
    private fun drawCategoryChart() {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // 为不同类别设置不同颜色
        val categoryColors = mapOf(
            "可回收物" to resources.getColor(R.color.recyclable),
            "厨余垃圾" to resources.getColor(R.color.kitchen),
            "有害垃圾" to resources.getColor(R.color.harmful),
            "其他垃圾" to resources.getColor(R.color.other)
        )

        // 添加数据条目
        categoryCountMap.forEach { (category, count) ->
            val percentage = (count.toFloat() / totalCount * 100).toInt()
            entries.add(PieEntry(percentage.toFloat(), "$category $percentage%"))

            // 添加颜色
            colors.add(categoryColors[category] ?: ColorTemplate.COLORFUL_COLORS[categoryCountMap.keys.indexOf(category) % ColorTemplate.COLORFUL_COLORS.size])
        }

        // 创建数据集
        val dataSet = PieDataSet(entries, "垃圾分类占比")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f

        // 创建数据对象
        val data = PieData(dataSet)

        // 配置图表
        val chart = binding.categoryChart
        chart.data = data
        chart.description = Description().apply {
            text = "" // 隐藏描述
        }
        chart.legend.isEnabled = true // 显示图例
        chart.legend.textSize = 12f
        chart.setUsePercentValues(true)
        chart.invalidate() // 刷新图表
    }

    // 设置分类详情列表
    private fun setupCategoryDetailList() {
        val categoryDetails = mutableListOf<CategoryDetail>()

        categoryCountMap.forEach { (category, count) ->
            val percentage = (count.toFloat() / totalCount * 100).toInt()
            val color = when (category) {
                "可回收物" -> R.color.recyclable
                "厨余垃圾" -> R.color.kitchen
                "有害垃圾" -> R.color.harmful
                "其他垃圾" -> R.color.other
                else -> R.color.primary
            }

            categoryDetails.add(CategoryDetail(category, count, percentage, color))
        }

        // 排序：按数量降序
        categoryDetails.sortByDescending { it.count }

        // 设置适配器
        binding.categoryDetailRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoryDetailRecycler.adapter = GarbageCategoryAdapter(categoryDetails)
    }

    // 生成改进建议（使用DeepSeek API流式输出）
    private fun generateSuggestionsWithAI() {
        // 显示加载状态
        val progressBar = ProgressBar(this).apply {
            layoutParams = binding.suggestionsContainer.layoutParams
        }
        binding.suggestionsContainer.addView(progressBar)

        // 构建提示词（包含用户的垃圾分类统计数据）
        val prompt = buildString {
            append("根据以下垃圾分类统计数据，生成个性化改进建议（3-5条，简洁实用）：\n")
            append("1. 总分类次数：$totalCount 次\n")
            categoryCountMap.forEach { (category, count) ->
                val percentage = (count.toFloat() / totalCount * 100).toInt()
                append("2. $category：$count 次（占比 $percentage%）\n")
            }
            append("建议需要结合数据特点，针对性强，语言口语化，不要使用Markdown格式")
        }

        // 创建建议条目视图（用于流式显示）
        val suggestionBinding = ItemSuggestionBinding.inflate(layoutInflater)
        binding.suggestionsContainer.removeView(progressBar) // 移除加载框
        binding.suggestionsContainer.addView(suggestionBinding.root)

        // 调用DeepSeek API流式输出
        deepSeekHelper.sendMessageStream(
            prompt = prompt,
            charDelay = 30, // 字符间隔30ms，模拟打字效果
            onChar = { char ->
                // 实时更新建议文本
                val currentText = suggestionBinding.suggestionText.text.toString()
                suggestionBinding.suggestionText.text = currentText + char
            },
            onComplete = {
                // 完成后可以添加图标或样式美化
                suggestionBinding.suggestionIcon.visibility = View.VISIBLE
            },
            onError = { errorMsg ->
                // 错误处理，显示默认建议
                suggestionBinding.suggestionText.text = "获取建议失败：$errorMsg\n可尝试增加可回收物分类，注意有害垃圾单独投放哦~"
                suggestionBinding.suggestionIcon.visibility = View.VISIBLE
            }
        )
    }

    // 分类详情数据类
    data class CategoryDetail(
        val name: String,
        val count: Int,
        val percentage: Int,
        val colorRes: Int
    )

    // 处理返回按钮
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 页面销毁时取消请求，避免内存泄漏
    override fun onDestroy() {
        super.onDestroy()
        // 若DeepSeekHelper有取消请求的方法，可在此调用
    }
}