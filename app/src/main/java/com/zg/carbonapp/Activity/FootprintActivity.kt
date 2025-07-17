package com.zg.carbonapp.Activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.zg.carbonapp.Dao.FootprintStepRecord
import com.zg.carbonapp.databinding.ActivityFootprintBinding
import java.text.SimpleDateFormat
import java.util.*
import com.zg.carbonapp.MMKV.CarbonFootprintDataMMKV
import com.zg.carbonapp.Service.SensorManager

class FootprintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFootprintBinding
    // 步数传感器管理器（支持Google Fit和本地传感器）
    private lateinit var sensorManager: SensorManager
    // 本周7天的日期字符串（yyyy-MM-dd）
    private lateinit var weekDates: List<String>
    // 本周7天的步数数据
    private var weekStepRecords: MutableList<FootprintStepRecord> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化传感器管理器
        sensorManager = SensorManager(this)
        sensorManager.initializeSensors()

        // 获取本周7天的日期
        weekDates = getThisWeekDates()
        // 异步获取本周7天真实步数，全部获取后刷新UI
        getWeekSteps(weekDates) { records ->
            weekStepRecords = records.toMutableList()
            updateUI(weekStepRecords)
        }

        // 注册本地步数实时监听，仅刷新当天数据和UI
        sensorManager.setOnStepChangedListener { todaySteps ->
            val today = getTodayDate()
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            val todayShort = sdf.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today) ?: Date())
            // 更新本周步数列表中当天的步数
            weekStepRecords.forEachIndexed { idx, record ->
                if (record.date == todayShort) {
                    weekStepRecords[idx] = FootprintStepRecord(todayShort, todaySteps)
                }
            }
            updateUI(weekStepRecords)
        }
    }

    // 异步获取本周7天真实步数，全部获取后统一刷新UI
    private fun getWeekSteps(weekDates: List<String>, callback: (List<FootprintStepRecord>) -> Unit) {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val stepRecords = MutableList(7) { FootprintStepRecord(sdf.format(Date()), 0) }
        var completed = 0
        weekDates.forEachIndexed { idx, date ->
            // 异步获取每一天的步数（Google Fit优先）
            sensorManager.getStepsForDate(date) { steps ->
                val shortDate = sdf.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date())
                stepRecords[idx] = FootprintStepRecord(shortDate, steps)
                Log.d("FootprintActivity", "date=$shortDate, steps=$steps") // 打印每一天的步数
                completed++
                if (completed == 7) {
                    callback(stepRecords)
                }
            }
        }
    }

    // 刷新UI，包括本周总步数、累计碳吸收、步数柱状图
    private fun updateUI(weekStepRecords: List<FootprintStepRecord>) {
        val totalSteps = weekStepRecords.sumOf { it.steps }
        val totalCarbon = totalSteps * 0.00004
        binding.tvTotalSteps.text = "本周总步数：$totalSteps"
        binding.tvTotalCarbon.text = "累计碳吸收：${"%.2f".format(totalCarbon)}g"

        // 构造柱状图数据
        val entries = weekStepRecords.mapIndexed { idx, record ->
            BarEntry(idx.toFloat(), record.steps.toFloat())
        }
        val dataSet = BarDataSet(entries, "步数")
        dataSet.color = getColor(com.zg.carbonapp.R.color.green_dark)
        val barData = BarData(dataSet)
        binding.barChart.data = barData
        binding.barChart.description.isEnabled = false
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.valueFormatter = DayAxisValueFormatter(weekStepRecords)
        binding.barChart.invalidate()
    }

    // 获取本周7天的日期字符串（yyyy-MM-dd）
    private fun getThisWeekDates(): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dayOfWeek)
        return (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            date
        }
    }

    // 获取今天日期字符串（yyyy-MM-dd）
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // 柱状图X轴日期格式化
    class DayAxisValueFormatter(private val list: List<FootprintStepRecord>) : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val idx = value.toInt()
            return if (idx in list.indices) list[idx].date else ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放传感器资源，防止内存泄漏
        sensorManager.releaseSensors()
    }
} 