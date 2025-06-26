package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.zg.carbonapp.databinding.ActivityFootprintBinding
import java.text.SimpleDateFormat
import java.util.*

class FootprintActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFootprintBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 模拟近7天步数
        val stepList = generateStepData()
        val carbonList = stepList.map { it.steps * 0.00004 }
        val totalSteps = stepList.sumOf { it.steps }
        val totalCarbon = carbonList.sum()

        binding.tvTotalSteps.text = "本周总步数：$totalSteps"
        binding.tvTotalCarbon.text = "累计碳吸收：${"%.2f".format(totalCarbon)}g"

        // 柱状图可视化
        val entries = stepList.mapIndexed { idx, record ->
            BarEntry(idx.toFloat(), record.steps.toFloat())
        }
        val dataSet = BarDataSet(entries, "步数")
        dataSet.color = getColor(com.zg.carbonapp.R.color.green_dark)
        val barData = BarData(dataSet)
        binding.barChart.data = barData
        binding.barChart.description.isEnabled = false
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.valueFormatter = DayAxisValueFormatter(stepList)
        binding.barChart.invalidate()
    }

    private fun generateStepData(): List<StepRecord> {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        return (0..6).map {
            val date = sdf.format(calendar.time)
            val steps = (5000..15000).random()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            StepRecord(date, steps)
        }.reversed()
    }

    data class StepRecord(val date: String, val steps: Int)

    class DayAxisValueFormatter(private val list: List<StepRecord>) : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val idx = value.toInt()
            return if (idx in list.indices) list[idx].date else ""
        }
    }
} 