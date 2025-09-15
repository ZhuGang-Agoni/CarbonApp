package com.zg.carbonapp.Activity

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.zg.carbonapp.Dao.DailyStepData
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.MMKV.StepCarbonMMKV
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.databinding.ActivityTravelStatisticBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TravelStatisticActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var binding: ActivityTravelStatisticBinding
    private val TAG = "TravelStatisticActivity"
    private lateinit var deepSeekHelper: DeepSeekHelper
    private var last7DaysRecords: List<ItemTravelRecord> = emptyList()
    // 新增：存储7天内的步数减碳数据
    private var stepCarbonData: Map<String, DailyStepData> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTravelStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lineChart = binding.lineChart
        pieChart = binding.pieChart
        deepSeekHelper = DeepSeekHelper()

        // 新增：初始化步数减碳数据
        initStepCarbonData()
        // 初始化摘要数据（已整合步数数据）
        initSummaryData()
        setupLineChart()
        setupPieChart()

        // 显示AI分析卡片
        binding.aiAnalysisCard.visibility = View.VISIBLE
    }



    // 新增：初始化7天内的步数减碳数据
    private fun initStepCarbonData() {
        val weekData = StepCarbonMMKV.getWeekData()
        // 转换为 <日期(MM/dd), DailyStepData> 的映射，方便查询
        stepCarbonData = weekData.associateBy {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date) ?: Date()
            )
        }
        Log.d(TAG, "7天步数减碳数据: $stepCarbonData")
    }

    private fun initSummaryData() {
        try {

            // 添加模拟数据 - 步数减碳（近7天）
            val calendar = Calendar.getInstance()
            val mockStepData = mutableMapOf<String, DailyStepData>()
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dateKey = SimpleDateFormat("MM/dd", Locale.getDefault()).format(calendar.time)

                val steps = 4000 + Random().nextInt(11000)
                val stepData = DailyStepData(date).apply {
                    this.steps = steps
                    calculateCarbon()
                }
                mockStepData[dateKey] = stepData
            }

            // 使用模拟数据
            stepCarbonData = mockStepData
            val allRecords = TravelRecordManager.getRecords().list

            // 新增：获取今日步数减碳数据
            val todayStepData = StepCarbonMMKV.getTodayData()
            val todayStepCarbon = todayStepData?.carbonReduction ?: 0.0

            // 计算今日总减碳（原有出行记录 + 今日步数）
            val todayStart = getTodayStartTime()
            val todayTravelCarbon = allRecords
                .filter { it.time >= todayStart }
                .sumOf { (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000 }
            val totalTodayCarbon = todayTravelCarbon.toFloat() + todayStepCarbon.toFloat()

            // 计算7天前的时间点
            // 计算7天前的时间点
            calendar.timeInMillis = todayStart
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgo = calendar.timeInMillis

            // 筛选7天内的出行记录
            last7DaysRecords = allRecords.filter { it.time >= sevenDaysAgo }

            // 计算7天总减碳（原有出行记录 + 7天步数）
            val sevenDayTravelCarbon = last7DaysRecords
                .sumOf { (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000 }
            val sevenDayStepCarbon = stepCarbonData.values.sumOf { it.carbonReduction.toDouble() }
            val totalSevenDayCarbon = sevenDayTravelCarbon + sevenDayStepCarbon

            // 统计出行方式（含步行步数）
            val modeStats = mutableMapOf<String, ModeStat>()
            // 1. 先统计原有出行记录
            last7DaysRecords.forEach { record ->
                val mode = when (record.travelModel) {
                    "bus" -> "公交"
                    "ride" -> "骑行"
                    "subway" -> "地铁"
                    "walk" -> "步行"
                    else -> "其他"
                }
                val carbon = (record.carbonCount.toDoubleOrNull() ?: 0.0) / 1000
                val current = modeStats[mode] ?: ModeStat(0, 0.0)
                modeStats[mode] = current.copy(
                    count = current.count + 1,
                    totalCarbon = current.totalCarbon + carbon
                )
            }
            // 2. 新增：将步数减碳数据计入"步行"方式
            val stepTotalCarbon = stepCarbonData.values.sumOf { it.carbonReduction.toDouble() }
            val walkStat = modeStats["步行"] ?: ModeStat(0, 0.0)
            // 步数按1次记录计算（实际可根据需求调整）
            modeStats["步行"] = walkStat.copy(
                count = walkStat.count + 1,
                totalCarbon = walkStat.totalCarbon + stepTotalCarbon
            )

            // 计算最常用方式
            val mostUsedMode = modeStats.values.maxWithOrNull(
                compareBy<ModeStat> { it.count }.thenByDescending { it.totalCarbon }
            )?.let { stat ->
                modeStats.entries
                    .filter { it.value.count == stat.count }
                    .maxByOrNull { it.value.totalCarbon }
                    ?.key
            } ?: "无"

            // 更新UI
            binding.apply {
                tvTotayCarbonSaved.text = "今日减碳 %.3f kg".format(totalTodayCarbon)
                tvTotalCarbon.text = "%.3f kg".format(totalSevenDayCarbon)
                tvMostUsedMode.text = mostUsedMode
            }

            // 生成AI分析报告（已包含步数数据）
            generateAIAnalysis()

        } catch (e: Exception) {
            Log.e(TAG, "初始化摘要数据失败: ${e.message}", e)
            showEmptyData()
        }
    }

    /**
     * 生成AI分析报告 - 包含步数减碳数据
     */
    private val handler = Handler(Looper.getMainLooper())
    private val responseBuilder = StringBuilder()
    private var currentPosition = 0
    private var isGenerating = false

    private fun generateAIAnalysis() {
        // 合并出行记录和步数数据，判断是否有数据
        val hasTravelData = last7DaysRecords.isNotEmpty()
        val hasStepData = stepCarbonData.isNotEmpty()
        if (!hasTravelData && !hasStepData) {
            binding.aiAnalysisText.text = "暂无出行和步数数据，无法生成分析报告"
            binding.aiAnalysisText.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            return
        }

        // 重置状态
        responseBuilder.clear()
        currentPosition = 0
        isGenerating = true

        // 构建包含步数数据的提示词
        val prompt = buildAnalysisPrompt()

        // 显示加载进度
        binding.progressBar.visibility = View.VISIBLE
        binding.aiAnalysisText.visibility = View.VISIBLE
        binding.aiAnalysisText.text = "正在生成分析报告..."

        deepSeekHelper.sendMessageStream(
            prompt = prompt,
            charDelay = 20,
            onChar = { char ->
                responseBuilder.append(char)
                if (responseBuilder.length == 1) {
                    handler.post(displayCharacterRunnable)
                }
            },
            onComplete = {
                handler.post {
                    binding.progressBar.visibility = View.GONE
                    isGenerating = false
                }
            },
            onError = { errorMsg ->
                handler.post {
                    binding.progressBar.visibility = View.GONE
                    binding.aiAnalysisText.text = "AI分析失败: $errorMsg\n请稍后重试"
                    isGenerating = false
                }
            }
        )
    }

    // 逐字显示任务
    private val displayCharacterRunnable = object : Runnable {
        override fun run() {
            if (currentPosition < responseBuilder.length) {
                val displayText = responseBuilder.substring(0, currentPosition + 1)
                binding.aiAnalysisText.text = displayText

                // 滚动到底部
                val textView = binding.aiAnalysisText
                val layout = textView.layout
                if (layout != null && textView.lineCount > 0) {
                    val lastLine = layout.getLineTop(textView.lineCount)
                    val visibleHeight = textView.height - textView.paddingTop - textView.paddingBottom
                    if (lastLine > visibleHeight) {
                        textView.scrollTo(0, lastLine - visibleHeight)
                    }
                }

                currentPosition++
                handler.postDelayed(this, 30)
            } else if (isGenerating) {
                handler.postDelayed(this, 100)
            }
        }
    }

    /**
     * 构建AI分析提示词 - 包含步数减碳数据
     */
    private fun buildAnalysisPrompt(): String {
        val modeStats = mutableMapOf<String, ModeStat>()
        val carbonByDay = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

        // 1. 统计原有出行方式
        last7DaysRecords.forEach { record ->
            val mode = when (record.travelModel) {
                "bus" -> "公交"
                "ride" -> "骑行"
                "subway" -> "地铁"
                "walk" -> "步行"
                else -> "其他"
            }
            val carbon = (record.carbonCount.toDoubleOrNull() ?: 0.0) / 1000
            val current = modeStats[mode] ?: ModeStat(0, 0.0)
            modeStats[mode] = current.copy(
                count = current.count + 1,
                totalCarbon = current.totalCarbon + carbon
            )

            // 记录每日出行减碳
            val date = Date(record.time)
            val dayKey = dateFormat.format(date)
            val calendar = Calendar.getInstance().apply { time = date }
            val weekDay = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val dayLabel = "$weekDay\n$dayKey"
            carbonByDay[dayLabel] = carbonByDay.getOrDefault(dayLabel, 0.0) + carbon
        }

        // 2. 新增：将步数减碳数据计入步行方式和每日数据
        stepCarbonData.forEach { (stepDate, stepData) ->
            // 解析步数日期的星期
            val date = SimpleDateFormat("MM/dd", Locale.getDefault()).parse(stepDate) ?: return@forEach
            val calendar = Calendar.getInstance().apply { time = date }
            val weekDay = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val dayLabel = "$weekDay\n$stepDate"

            // 计入每日总减碳
            carbonByDay[dayLabel] = carbonByDay.getOrDefault(dayLabel, 0.0) + stepData.carbonReduction

            // 计入步行方式统计
            val walkStat = modeStats["步行"] ?: ModeStat(0, 0.0)
            modeStats["步行"] = walkStat.copy(
                count = walkStat.count + 1, // 按1次/天计算
                totalCarbon = walkStat.totalCarbon + stepData.carbonReduction
            )
        }

        // 3. 构建提示词
        val prompt = StringBuilder()
        prompt.append("你是环保专家，根据以下7天出行数据（含步行步数）分析碳足迹并提供建议：\n\n")

        prompt.append("1. 出行方式统计：\n")
        modeStats.forEach { (mode, stat) ->
            val totalCount = modeStats.values.sumOf { it.count.toDouble() }
            val percentage = if (totalCount > 0) (stat.count / totalCount * 100).toInt() else 0
            prompt.append("- $mode: ${stat.count}次 (${percentage}%), 减碳${"%.2f".format(stat.totalCarbon)}kg\n")
        }

        prompt.append("\n2. 每日减碳趋势（含步行）：\n")
        carbonByDay.forEach { (day, carbon) ->
            prompt.append("- $day: 减碳${"%.3f".format(carbon)}kg\n")
        }

        prompt.append("\n请完成：\n")
        prompt.append("1. 分析出行习惯（200字内）\n")
        prompt.append("2. 指出减碳最多的方式和改进方向（100字内）\n")
        prompt.append("3. 3条具体减碳建议（每条≤50字）\n")
        prompt.append("4. 一句鼓励的话\n")
        prompt.append("用中文回复，简洁可行。")

        Log.d(TAG, "AI提示词: ${prompt.toString()}")
        return prompt.toString()
    }

    /**
     * 空数据时显示默认值
     */
    private fun showEmptyData() {
        binding.apply {
            tvTotayCarbonSaved.text = "今日减碳 0.000 kg"
            tvTotalCarbon.text = "0.000 kg"
            tvMostUsedMode.text = "无"
        }
    }

    /**
     * 获取今日0点的时间戳
     */
    private fun getTodayStartTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun setupLineChart() {
        val allRecords = TravelRecordManager.getRecords().list
        val calendar = Calendar.getInstance().apply {
            timeInMillis = getTodayStartTime()
            add(Calendar.DAY_OF_YEAR, -7)
        }
        val sevenDaysAgo = calendar.timeInMillis
        val last7DaysRecords = allRecords.filter { it.time >= sevenDaysAgo }

        // 新增：获取7天步数数据并合并到每日减碳中
        val weeklyData = getWeeklyData(last7DaysRecords)
        val entries = ArrayList<Entry>()
        val days = ArrayList<String>()

        weeklyData.forEachIndexed { index, data ->
            entries.add(Entry(index.toFloat(), data.totalCarbon))
            days.add(data.dayLabel)
        }

        val dataSet = LineDataSet(entries, "每日减碳量 (kg)")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 3f
        dataSet.setCircleColor(Color.parseColor("#388E3C"))
        dataSet.circleRadius = 5f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#C8E6C9")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // X轴配置
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(days)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        // Y轴配置
        lineChart.axisRight.isEnabled = false
        val yAxis = lineChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "%.1f kg".format(value)
            }
        }

        // 图表整体配置
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.animateXY(1000, 1000)
        lineChart.invalidate()
    }

    private fun setupPieChart() {
        val records = TravelRecordManager.getRecords().list
        val travelModeData = getTravelModeData(records)
        val filteredData = travelModeData.filter { it.value > 0 }

        if (filteredData.isEmpty()) {
            pieChart.setNoDataText("暂无出行数据")
            pieChart.setNoDataTextColor(Color.GRAY)
            pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        filteredData.forEach { (mode, value) ->
            entries.add(PieEntry(value, mode))
        }

        val dataSet = PieDataSet(entries, "出行方式减碳占比")
        dataSet.colors = listOf(
            Color.parseColor("#FF9800"), // 公交
            Color.parseColor("#4CAF50"),  // 骑行
            Color.parseColor("#2196F3"), // 地铁
            Color.parseColor("#9C27B0"),  // 步行（含步数）
            Color.parseColor("#F44336")   // 其他
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        // 图表配置
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setDrawEntryLabels(false)
        pieChart.setUsePercentValues(true)
        pieChart.holeRadius = 30f
        pieChart.transparentCircleRadius = 35f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "出行方式\n减碳占比"
        pieChart.setCenterTextSize(14f)
        pieChart.setCenterTextColor(Color.parseColor("#333333"))

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    // 新增：合并出行记录和步数数据，计算每日总减碳
    private fun getWeeklyData(records: List<ItemTravelRecord>): List<DayData> {
        val calendar = Calendar.getInstance()
        val dayDataList = mutableListOf<DayData>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

        // 获取过去7天的数据（包括今天）
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = calendar.time
            val dayKey = dateFormat.format(date)
            val weekDay = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val dayLabel = "$weekDay\n$dayKey"

            // 1. 计算当天出行减碳
            val dayRecords = records.filter { isSameDay(Date(it.time), date) }
            val travelCarbon = dayRecords.sumOf {
                (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000
            }.toFloat()

            // 2. 计算当天步数减碳
            val stepCarbon = stepCarbonData[dayKey]?.carbonReduction ?: 0f

            // 3. 总减碳 = 出行 + 步数
            val totalCarbon = travelCarbon + stepCarbon

            dayDataList.add(DayData(dayLabel, totalCarbon))
        }

        return dayDataList
    }

    // 新增：合并出行记录和步数数据，统计各方式总减碳
    private fun getTravelModeData(records: List<ItemTravelRecord>): Map<String, Float> {
        val modeMap = mutableMapOf(
            "公交" to 0f,
            "骑行" to 0f,
            "地铁" to 0f,
            "步行" to 0f,
            "其他" to 0f
        )

        // 1. 统计原有出行方式减碳
        records.forEach { record ->
            val mode = when (record.travelModel) {
                "bus" -> "公交"
                "ride" -> "骑行"
                "subway" -> "地铁"
                "walk" -> "步行"
                else -> "其他"
            }
            val carbon = (record.carbonCount.toFloatOrNull() ?: 0f) / 1000
            modeMap[mode] = modeMap[mode]!! + carbon
        }

        // 2. 新增：统计步数减碳，计入步行
        val stepTotalCarbon = stepCarbonData.values.sumOf { it.carbonReduction.toDouble() }.toFloat()
        modeMap["步行"] = modeMap["步行"]!! + stepTotalCarbon

        return modeMap
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    // 内部数据类：存储每日减碳数据
    private data class DayData(val dayLabel: String, val totalCarbon: Float)

    // 内部数据类：存储出行方式统计（次数+总减碳量）
    private data class ModeStat(val count: Int, val totalCarbon: Double)

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
//package com.zg.carbonapp.Activity
//
//import android.graphics.Color
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.View
//import androidx.appcompat.app.AppCompatActivity
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.charts.PieChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.*
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
//import com.github.mikephil.charting.formatter.ValueFormatter
//import com.zg.carbonapp.Dao.ItemTravelRecord
//import com.zg.carbonapp.MMKV.TravelRecordManager
//import com.zg.carbonapp.Tool.DeepSeekHelper
//import com.zg.carbonapp.databinding.ActivityTravelStatisticBinding
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.collections.ArrayList
//
//class TravelStatisticActivity : AppCompatActivity() {
//
//    private lateinit var lineChart: LineChart
//    private lateinit var pieChart: PieChart
//    private lateinit var binding: ActivityTravelStatisticBinding
//    private val TAG = "TravelStatisticActivity"
//    private lateinit var deepSeekHelper: DeepSeekHelper
//    private var last7DaysRecords: List<ItemTravelRecord> = emptyList()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityTravelStatisticBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        lineChart = binding.lineChart
//        pieChart = binding.pieChart
//        deepSeekHelper = DeepSeekHelper()
//
//        initSummaryData()
//        setupLineChart()
//        setupPieChart()
//
//        // 显示AI分析卡片
//        binding.aiAnalysisCard.visibility = View.VISIBLE
//    }
//
//    private fun initSummaryData() {
//        try {
//            val allRecords = TravelRecordManager.getRecords().list
//            if (allRecords.isEmpty()) {
//                showEmptyData()
//                return
//            }
//
//            val todayStart = getTodayStartTime()
//            val todayCarbon = allRecords
//                .filter { it.time >= todayStart }
//                .sumOf { (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000 }
//
//            // 计算7天前的时间点
//            val calendar = Calendar.getInstance().apply {
//                timeInMillis = todayStart
//                add(Calendar.DAY_OF_YEAR, -7)
//            }
//            val sevenDaysAgo = calendar.timeInMillis
//
//            last7DaysRecords = allRecords.filter { it.time >= sevenDaysAgo }
//
//            val sevenDayCarbon = last7DaysRecords
//                .sumOf { (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000 }
//
//            val modeStats = mutableMapOf<String, ModeStat>()
//            last7DaysRecords.forEach { record ->
//                val mode = when (record.travelModel) {
//                    "bus" -> "公交"
//                    "ride" -> "骑行"
//                    "subway" -> "地铁"
//                    "walk" -> "步行"
//                    else -> "其他"
//                }
//                val carbon = (record.carbonCount.toDoubleOrNull() ?: 0.0) / 1000
//                val current = modeStats[mode] ?: ModeStat(0, 0.0)
//                modeStats[mode] = current.copy(
//                    count = current.count + 1,
//                    totalCarbon = current.totalCarbon + carbon
//                )
//            }
//
//            // 修复最常用方式计算逻辑
//            val mostUsedMode = modeStats.values.maxWithOrNull(
//                compareBy<ModeStat> { it.count }.thenByDescending { it.totalCarbon }
//            )?.let { stat ->
//                modeStats.entries
//                    .filter { it.value.count == stat.count }
//                    .maxByOrNull { it.value.totalCarbon }
//                    ?.key
//            } ?: "无"
//
//            binding.apply {
//                tvTotayCarbonSaved.text = "今日减碳 %.3f kg".format(todayCarbon)
//                tvTotalCarbon.text = "%.3f kg".format(sevenDayCarbon)
//                tvMostUsedMode.text = mostUsedMode
//            }
//
//            // 生成AI分析报告
//            generateAIAnalysis()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "初始化摘要数据失败: ${e.message}", e)
//            showEmptyData()
//        }
//    }
//
//    /**
//     * 生成AI分析报告 - 优化后的流式输出版本
//     */
//    private val handler = Handler(Looper.getMainLooper())
//    private val responseBuilder = StringBuilder()
//    private var currentPosition = 0
//    private var isGenerating = false
//
//    private fun generateAIAnalysis() {
//        if (last7DaysRecords.isEmpty()) {
//            binding.aiAnalysisText.text = "暂无出行数据，无法生成分析报告"
//            binding.aiAnalysisText.visibility = View.VISIBLE
//            binding.progressBar.visibility = View.GONE
//            return
//        }
//
//        // 重置状态
//        responseBuilder.clear()
//        currentPosition = 0
//        isGenerating = true
//
//        // 构建提示词
//        val prompt = buildAnalysisPrompt()
//
//        // 显示加载进度
//        binding.progressBar.visibility = View.VISIBLE
//        binding.aiAnalysisText.visibility = View.VISIBLE
//        binding.aiAnalysisText.text = "正在生成分析报告..."
//
//        deepSeekHelper.sendMessageStream(
//            prompt = prompt,
//            charDelay = 20,
//            onChar = { char ->
//                // 收集字符但不立即显示
//                responseBuilder.append(char)
//
//                // 如果是第一个字符，启动逐字显示循环
//                if (responseBuilder.length == 1) {
//                    handler.post(displayCharacterRunnable)
//                }
//            },
//            onComplete = {
//                handler.post {
//                    binding.progressBar.visibility = View.GONE
//                    isGenerating = false
//                    Log.d(TAG, "AI分析报告生成完成")
//                }
//            },
//            onError = { errorMsg ->
//                handler.post {
//                    binding.progressBar.visibility = View.GONE
//                    binding.aiAnalysisText.text = "AI分析失败: $errorMsg\n请稍后重试"
//                    Log.e(TAG, "AI分析失败: $errorMsg")
//                    isGenerating = false
//                }
//            }
//        )
//    }
//
//    // 逐字显示任务
//    private val displayCharacterRunnable = object : Runnable {
//        override fun run() {
//            if (currentPosition < responseBuilder.length) {
//                // 显示到当前位置的文本
//                val displayText = responseBuilder.substring(0, currentPosition + 1)
//                binding.aiAnalysisText.text = displayText
//
//                // 滚动到可见区域底部
//                val textView = binding.aiAnalysisText
//                val layout = textView.layout
//                if (layout != null && textView.lineCount > 0) {
//                    val lastLine = layout.getLineTop(textView.lineCount)
//                    val visibleHeight = textView.height - textView.paddingTop - textView.paddingBottom
//
//                    if (lastLine > visibleHeight) {
//                        textView.scrollTo(0, lastLine - visibleHeight)
//                    }
//                }
//
//                currentPosition++
//
//                // 继续显示下一个字符
//                handler.postDelayed(this, 30) // 每个字符显示间隔30毫秒
//            } else if (isGenerating) {
//                // 等待更多内容
//                handler.postDelayed(this, 100)
//            }
//        }
//    }
//
//    /**
//     * 构建AI分析提示词
//     */
//    private fun buildAnalysisPrompt(): String {
//        val modeStats = mutableMapOf<String, ModeStat>()
//        val carbonByDay = mutableMapOf<String, Double>()
//        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
//        val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
//
//        // 统计出行方式和每日减碳
//        last7DaysRecords.forEach { record ->
//            val mode = when (record.travelModel) {
//                "bus" -> "公交"
//                "ride" -> "骑行"
//                "subway" -> "地铁"
//                "walk" -> "步行"
//                else -> "其他"
//            }
//            val carbon = record.carbonCount.toDoubleOrNull() ?: 0.0
//            val current = modeStats[mode] ?: ModeStat(0, 0.0)
//            modeStats[mode] = current.copy(
//                count = current.count + 1,
//                totalCarbon = current.totalCarbon + carbon / 1000
//            )
//
//            val date = Date(record.time)
//            val dayKey = dateFormat.format(date)
//            val calendar = Calendar.getInstance().apply { time = date }
//            val weekDay = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
//            val dayLabel = "$weekDay\n$dayKey"
//            carbonByDay[dayLabel] = carbonByDay.getOrDefault(dayLabel, 0.0) + (carbon / 1000)
//        }
//
//        // 构建提示词内容
//        val prompt = StringBuilder()
//        prompt.append("你是环保专家，根据以下7天出行数据分析碳足迹并提供建议：\n\n")
//
//        prompt.append("1. 出行方式统计：\n")
//        modeStats.forEach { (mode, stat) ->
//            val percentage = (stat.count.toDouble() / last7DaysRecords.size * 100).toInt()
//            prompt.append("- $mode: ${stat.count}次 (${percentage}%), 减碳${"%.2f".format(stat.totalCarbon)}kg\n")
//        }
//
//        prompt.append("\n2. 每日减碳趋势：\n")
//        carbonByDay.forEach { (day, carbon) ->
//            prompt.append("- $day: 减碳${"%.3f".format(carbon)}kg\n")
//        }
//
//        prompt.append("\n请完成：\n")
//        prompt.append("1. 分析出行习惯（200字内）\n")
//        prompt.append("2. 指出减碳最多的方式和改进方向（100字内）\n")
//        prompt.append("3. 3条具体减碳建议（每条≤50字）\n")
//        prompt.append("4. 一句鼓励的话\n")
//        prompt.append("用中文回复，简洁可行。")
//
//        Log.d(TAG, "AI提示词: ${prompt.toString()}")
//        return prompt.toString()
//    }
//
//    /**
//     * 空数据时显示默认值
//     */
//    private fun showEmptyData() {
//        binding.apply {
//            tvTotayCarbonSaved.text = "今日减碳 0.000 kg"
//            tvTotalCarbon.text = "0.000 kg"
//            tvMostUsedMode.text = "无"
//        }
//    }
//
//    /**
//     * 获取今日0点的时间戳
//     */
//    private fun getTodayStartTime(): Long {
//        val calendar = Calendar.getInstance()
//        calendar.set(Calendar.HOUR_OF_DAY, 0)
//        calendar.set(Calendar.MINUTE, 0)
//        calendar.set(Calendar.SECOND, 0)
//        calendar.set(Calendar.MILLISECOND, 0)
//        return calendar.timeInMillis
//    }
//
//    private fun setupLineChart() {
//        val allRecords = TravelRecordManager.getRecords().list
//        val calendar = Calendar.getInstance().apply {
//            timeInMillis = getTodayStartTime()
//            add(Calendar.DAY_OF_YEAR, -7)
//        }
//        val sevenDaysAgo = calendar.timeInMillis
//        val last7DaysRecords = allRecords.filter { it.time >= sevenDaysAgo }
//
//        val weeklyData = getWeeklyData(last7DaysRecords)
//        val entries = ArrayList<Entry>()
//        val days = ArrayList<String>()
//
//        weeklyData.forEachIndexed { index, data ->
//            entries.add(Entry(index.toFloat(), data.totalCarbon))
//            days.add(data.dayLabel)
//        }
//
//        val dataSet = LineDataSet(entries, "每日减碳量 (kg)")
//        dataSet.color = Color.parseColor("#4CAF50")
//        dataSet.valueTextColor = Color.BLACK
//        dataSet.lineWidth = 3f
//        dataSet.setCircleColor(Color.parseColor("#388E3C"))
//        dataSet.circleRadius = 5f
//        dataSet.setDrawFilled(true)
//        dataSet.fillColor = Color.parseColor("#C8E6C9")
//        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
//
//        val lineData = LineData(dataSet)
//        lineChart.data = lineData
//
//        // X轴配置
//        val xAxis = lineChart.xAxis
//        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.valueFormatter = IndexAxisValueFormatter(days)
//        xAxis.granularity = 1f
//        xAxis.setDrawGridLines(false)
//        xAxis.labelRotationAngle = -45f
//
//        // Y轴配置
//        lineChart.axisRight.isEnabled = false
//        val yAxis = lineChart.axisLeft
//        yAxis.axisMinimum = 0f
//        yAxis.valueFormatter = object : ValueFormatter() {
//            override fun getFormattedValue(value: Float): String {
//                return "%.1f kg".format(value)
//            }
//        }
//
//        // 图表整体配置
//        lineChart.description.isEnabled = false
//        lineChart.legend.isEnabled = false
//        lineChart.setTouchEnabled(true)
//        lineChart.setPinchZoom(true)
//        lineChart.animateXY(1000, 1000)
//        lineChart.invalidate()
//    }
//
//    private fun setupPieChart() {
//        val records = TravelRecordManager.getRecords().list
//        val travelModeData = getTravelModeData(records)
//        val filteredData = travelModeData.filter { it.value > 0 }
//
//        if (filteredData.isEmpty()) {
//            pieChart.setNoDataText("暂无出行数据")
//            pieChart.setNoDataTextColor(Color.GRAY)
//            pieChart.invalidate()
//            return
//        }
//
//        val entries = ArrayList<PieEntry>()
//        filteredData.forEach { (mode, value) ->
//            entries.add(PieEntry(value, mode))
//        }
//
//        val dataSet = PieDataSet(entries, "出行方式减碳占比")
//        dataSet.colors = listOf(
//            Color.parseColor("#FF9800"), // 公交
//            Color.parseColor("#4CAF50"),  // 骑行
//            Color.parseColor("#2196F3"), // 地铁
//            Color.parseColor("#9C27B0"),  // 步行
//            Color.parseColor("#F44336")   // 其他
//        )
//        dataSet.valueTextSize = 12f
//        dataSet.valueTextColor = Color.WHITE
//
//        val pieData = PieData(dataSet)
//        pieChart.data = pieData
//
//        // 图表配置
//        pieChart.description.isEnabled = false
//        pieChart.legend.isEnabled = true
//        pieChart.setEntryLabelColor(Color.BLACK)
//        pieChart.setEntryLabelTextSize(12f)
//        pieChart.setDrawEntryLabels(false)
//        pieChart.setUsePercentValues(true)
//        pieChart.holeRadius = 30f
//        pieChart.transparentCircleRadius = 35f
//        pieChart.isDrawHoleEnabled = true
//        pieChart.setHoleColor(Color.TRANSPARENT)
//        pieChart.setDrawCenterText(true)
//        pieChart.centerText = "出行方式\n减碳占比"
//        pieChart.setCenterTextSize(14f)
//        pieChart.setCenterTextColor(Color.parseColor("#333333"))
//
//        pieChart.animateY(1000)
//        pieChart.invalidate()
//    }
//
//    private fun getWeeklyData(records: List<ItemTravelRecord>): List<DayData> {
//        val calendar = Calendar.getInstance()
//        val dayDataList = mutableListOf<DayData>()
//
//        // 获取过去7天的数据（包括今天）
//        for (i in 6 downTo 0) {
//            calendar.time = Date()
//            calendar.add(Calendar.DAY_OF_YEAR, -i)
//            val date = calendar.time
//            val dayLabel = SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
//
//            // 过滤当天的记录
//            val dayRecords = records.filter { isSameDay(Date(it.time), date) }
//
//            // 计算当天总减碳量（kg）
//            val totalCarbon = dayRecords.sumOf {
//                (it.carbonCount.toDoubleOrNull() ?: 0.0) / 1000
//            }.toFloat()
//
//            dayDataList.add(DayData(dayLabel, totalCarbon))
//        }
//
//        return dayDataList
//    }
//
//    private fun getTravelModeData(records: List<ItemTravelRecord>): Map<String, Float> {
//        val modeMap = mutableMapOf(
//            "公交" to 0f,
//            "骑行" to 0f,
//            "地铁" to 0f,
//            "步行" to 0f,
//            "其他" to 0f
//        )
//
//        records.forEach { record ->
//            val mode = when (record.travelModel) {
//                "bus" -> "公交"
//                "ride" -> "骑行"
//                "subway" -> "地铁"
//                "walk" -> "步行"
//                else -> "其他"
//            }
//            val carbon = (record.carbonCount.toFloatOrNull() ?: 0f) / 1000
//            modeMap[mode] = modeMap[mode]!! + carbon
//        }
//        return modeMap
//    }
//
//    private fun isSameDay(date1: Date, date2: Date): Boolean {
//        val cal1 = Calendar.getInstance().apply { time = date1 }
//        val cal2 = Calendar.getInstance().apply { time = date2 }
//        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
//                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
//                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
//    }
//
//    // 内部数据类：存储每日减碳数据
//    private data class DayData(val dayLabel: String, val totalCarbon: Float)
//
//    // 内部数据类：存储出行方式统计（次数+总减碳量）
//    private data class ModeStat(val count: Int, val totalCarbon: Double)
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // 移除所有待处理的回调
//        handler.removeCallbacksAndMessages(null)
//    }
//}
