package com.zg.carbonapp.Activity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.zg.carbonapp.Dao.DailyStepData
import com.zg.carbonapp.MMKV.StepCarbonMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.databinding.ActivityCarbonHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CarbonHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonHistoryBinding
    private lateinit var weekData: List<DailyStepData>
    private lateinit var monthData: List<DailyStepData>
    private var currentData: List<DailyStepData> = emptyList()
    private var timeType = TimeType.WEEK

    // DeepSeek集成（保持原样）
    private val deepSeekHelper = DeepSeekHelper()
    private var deepSeekJob: Job? = null

    enum class TimeType { WEEK, MONTH, YEAR }

    // 每日步数清零的WorkManager任务
    class DailyStepResetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())

            // 获取当前日期的数据
            val todayData = StepCarbonMMKV.getTodayData() ?: DailyStepData(today)

            // 如果是新的一天且数据未清零，则重置步数
            if (todayData.steps > 0) {
                // 保存当前数据作为历史记录（保持原样）
                StepCarbonMMKV.saveTodayData(todayData)

                // 创建新的每日数据对象（步数清零）
                val newTodayData = DailyStepData(
                    date = today,
                    steps = 0,
                    carbonReduction = 0f,
                    carbonPoints = todayData.carbonPoints // 保留积分，只清零步数和减排量
                )
                StepCarbonMMKV.saveTodayData(newTodayData)
            }
            return Result.success()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化每日步数清零任务（每天0点执行）
        setupDailyStepReset()

        // 加载数据
        weekData = StepCarbonMMKV.getWeekData()
        monthData = StepCarbonMMKV.getMonthData()
        currentData = weekData

        initChart()
        updateTotalData()
        updateTrendAnalysis() // 保持原有DeepSeek逻辑
        initTimeSelector()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // 设置每日0点自动清零步数
    private fun setupDailyStepReset() {
        // 计算距离下次0点的时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果当前时间已过今天0点，则设置为明天0点
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 创建约束条件（不需要网络）
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // 创建每日执行的任务
        val resetRequest = PeriodicWorkRequestBuilder<DailyStepResetWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(
                calendar.timeInMillis - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS
            )
            .build()

        // 加入任务队列（保证唯一任务）
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "DailyStepReset",
                ExistingPeriodicWorkPolicy.REPLACE,
                resetRequest
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消正在进行的DeepSeek请求（保持原样）
        deepSeekJob?.cancel()
    }

    private fun initTimeSelector() {
        binding.btnWeek.setOnClickListener {
            setTimeType(TimeType.WEEK)
            currentData = weekData
            updateUI()
        }
        binding.btnMonth.setOnClickListener {
            setTimeType(TimeType.MONTH)
            currentData = monthData
            updateUI()
        }
        binding.btnYear.setOnClickListener {
            setTimeType(TimeType.YEAR)
            currentData = if (monthData.size > 12) monthData.take(12) else monthData
            updateUI()
        }
    }

    private fun setTimeType(type: TimeType) {
        timeType = type
        binding.btnWeek.apply {
            backgroundTintList = getColorStateList(
                if (type == TimeType.WEEK) R.color.green_primary else R.color.gray_200
            )
            setTextColor(getColor(if (type == TimeType.WEEK) android.R.color.white else R.color.gray_600))
        }
        binding.btnMonth.apply {
            backgroundTintList = getColorStateList(
                if (type == TimeType.MONTH) R.color.green_primary else R.color.gray_200
            )
            setTextColor(getColor(if (type == TimeType.MONTH) android.R.color.white else R.color.gray_600))
        }
        binding.btnYear.apply {
            backgroundTintList = getColorStateList(
                if (type == TimeType.YEAR) R.color.green_primary else R.color.gray_200
            )
            setTextColor(getColor(if (type == TimeType.YEAR) android.R.color.white else R.color.gray_600))
        }
    }

    private fun updateUI() {
        initChart()
        updateTotalData()
        updateTrendAnalysis()
    }

    private fun initChart() {
        val chart = binding.combinedChart
        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            isHighlightPerDragEnabled = true
            drawOrder = arrayOf(DrawOrder.BAR, DrawOrder.LINE)
        }

        val xAxis = chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index !in currentData.indices) return ""

                    return when (timeType) {
                        TimeType.WEEK -> {
                            val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                            val date = currentData[index].date
                            val calendar = Calendar.getInstance().apply {
                                time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
                            }
                            weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                        }
                        TimeType.MONTH -> currentData[index].date.split("-")[2] + "日"
                        TimeType.YEAR -> currentData[index].date.split("-")[1] + "月"
                    }
                }
            }
            setLabelCount(minOf(currentData.size, 7), true)
            axisMinimum = -0.5f
            axisMaximum = currentData.size - 0.5f
            setDrawGridLines(false)
        }

        val leftAxis = chart.axisLeft.apply {
            axisMinimum = 0f
            labelCount = 5
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F0F0F0")
            // 清除所有限制线，确保只有一条平均线
            removeAllLimitLines()
        }

        val rightAxis = chart.axisRight.apply {
            axisMinimum = 0f
            labelCount = 5
            setDrawGridLines(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = "%.1fkg".format(value)
            }
        }

        // 步数柱状图
        val barEntries = currentData.mapIndexed { index, data ->
            BarEntry(index.toFloat(), data.steps.toFloat())
        }
        val barDataSet = BarDataSet(barEntries, "步数").apply {
            color = getColor(R.color.blue_primary)
            axisDependency = YAxis.AxisDependency.LEFT
            barBorderWidth = 0f
            setDrawValues(false)
        }

        // 碳减排折线图
        val lineEntries = currentData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.carbonReduction)
        }
        val lineDataSet = LineDataSet(lineEntries, "碳减排").apply {
            color = getColor(R.color.green_primary)
            setCircleColor(getColor(R.color.green_primary))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            axisDependency = YAxis.AxisDependency.RIGHT
            setDrawValues(false)
        }

        // 添加单条平均步数线（修复多条线问题）
        if (currentData.isNotEmpty()) {
            val avgSteps = currentData.map { it.steps }.average().toFloat()
            val avgLine = LimitLine(avgSteps, "平均步数: ${avgSteps.roundToInt()}").apply {
                lineColor = getColor(R.color.orange_primary)
                lineWidth = 1.5f
                textSize = 10f
                textColor = getColor(R.color.gray_500)
                // 设置平均线位置，避免与数据重叠
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            }
            leftAxis.addLimitLine(avgLine)
        }

        chart.data = CombinedData().apply {
            setData(BarData(barDataSet))
            setData(LineData(lineDataSet))
        }
        chart.animateXY(800, 800)
        chart.invalidate()
    }

    private fun updateTotalData() {
        val totalSteps = currentData.sumOf { it.steps }
        val totalCarbon = currentData.fold(0f) { acc, item -> acc + item.carbonReduction }
        val totalPoints = currentData.sumOf { it.carbonPoints }

        binding.tvTotalSteps.text = "%,d 步".format(totalSteps)
        binding.tvTotalCarbon.text = "%.2f kg".format(totalCarbon)
        binding.tvTotalPoints.text = "%,d 分".format(totalPoints)
    }

    // 保持原有DeepSeek逻辑不变
    private fun updateTrendAnalysis() {
        if (currentData.isEmpty()) {
            binding.tvTrend.text = "暂无数据可分析，请先积累步行记录~"
            return
        }

        // 取消之前的请求
        deepSeekJob?.cancel()

        // 准备数据用于生成提示
        val maxStepsDay = currentData.maxByOrNull { it.steps }
        val avgSteps = currentData.map { it.steps }.average().toInt()
        val totalCarbon = currentData.sumOf { it.carbonReduction.toDouble() }.toFloat()
        val totalPoints = currentData.sumOf { it.carbonPoints }

        // 构建DeepSeek提示
        val prompt = buildString {
            append("你是一个环保数据分析专家，请根据以下步行碳减排数据生成一段简洁的数据分析（不超过150字）：\n")
            append("时间范围：${when (timeType) {
                TimeType.WEEK -> "本周"
                TimeType.MONTH -> "本月"
                TimeType.YEAR -> "今年"
            }}\n")
            append("总步数：${binding.tvTotalSteps.text}\n")
            append("总碳减排量：${binding.tvTotalCarbon.text}\n")
            append("总积分：${binding.tvTotalPoints.text}\n")
            append("平均每日步数：$avgSteps}步\n")

            maxStepsDay?.let {
                append("最佳表现日：${it.date}（${it.steps}步，减排${it.carbonReduction}kg）\n")
            }

            append("数据趋势：")
            if (currentData.size > 1) {
                val first = currentData.first().steps
                val last = currentData.last().steps
                when {
                    last > first * 1.2 -> append("明显上升趋势")
                    last < first * 0.8 -> append("有所下降趋势")
                    else -> append("保持稳定")
                }
            } else {
                append("单日数据")
            }
            append("\n请用友好、鼓励的语气给出分析，并适当提供环保建议。")
        }

        // 显示加载状态
        binding.tvTrend.text = "数据分析中..."

        // 启动DeepSeek流式请求
        deepSeekJob = CoroutineScope(Dispatchers.Main).launch {
            var fullResponse = ""
            deepSeekHelper.sendMessageStream(
                prompt = prompt,
                charDelay = 30, // 更快的字符显示速度
                onChar = { char ->
                    fullResponse += char
                    binding.tvTrend.text = fullResponse
                },
                onComplete = {
                    // 可选：完成后可以添加一些操作
                },
                onError = { errorMsg ->
                    binding.tvTrend.text = "分析失败: $errorMsg\n可尝试重新选择时间段"
                }
            )
        }
    }

    private fun getLastPeriodData(): List<DailyStepData> {
        return when (timeType) {
            TimeType.WEEK -> {
                val calendar = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                (0..6).map { i ->
                    val date = sdf.format(calendar.time)
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    StepCarbonMMKV.getWeekData().find { it.date == date } ?: DailyStepData(date)
                }
            }
            else -> emptyList()
        }
    }

    private fun getWeekDay(dateStr: String): String {
        val calendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!
        }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            Calendar.SATURDAY -> "六"
            else -> ""
        }
    }

    private fun getMaxMonth(data: List<DailyStepData>): String {
        val monthSteps = data.groupBy { it.date.split("-")[1] }
            .mapValues { (_, days) -> days.sumOf { it.steps } }
        return monthSteps.maxByOrNull { it.value }?.key?.let { "$it 月" } ?: ""
    }
}
