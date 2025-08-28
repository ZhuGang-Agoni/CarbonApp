package com.zg.carbonapp.Activity


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.Dao.DailyStepData
import com.zg.carbonapp.MMKV.StepCarbonMMKV

import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityCarbonReportBinding
import java.text.SimpleDateFormat
import java.util.*

class CarbonReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonReportBinding
    private lateinit var todayData: DailyStepData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取今日数据
        todayData = intent.getParcelableExtra("todayData") ?: DailyStepData(getTodayDate())
        val monthData = StepCarbonMMKV.getMonthData()
        val monthTotal =
            monthData.fold(0f) { acc, dailyStepData -> acc + dailyStepData.carbonReduction }
        // 填充报告内容
        binding.apply {
            // 日期
            val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            tvDate.text = sdf.format(Date())

            // 今日数据
            tvTodaySteps.text = "${todayData.steps} 步"
            tvTodayKm.text = String.format("%.1f 公里", todayData.steps / 1000f)
            tvTodayCarbon.text = String.format("%.2f kg", todayData.carbonReduction)

            // 本月数据
            tvMonthDays.text = "${monthData.size} 天"
            tvMonthTotalSteps.text = "${monthData.sumOf { it.steps }} 步"
            tvMonthCarbon.text = String.format("%.2f kg", monthTotal)

            // 等效种树
            val monthTrees = (monthTotal / 10f).toInt()
            tvMonthTrees.text = "$monthTrees 棵（每棵树年固碳10kg）"

            // 环保贡献描述
            tvContribution.text = when {
                todayData.carbonReduction > 1.0 -> "优秀！今日减排超过1kg，相当于为地球减少了1kg温室气体排放"
                todayData.carbonReduction > 0.5 -> "良好！继续保持，你的步行正在为低碳地球做贡献"
                else -> "加油！多步行可以增加减排量，积累更多碳积分"
            }
        }

    }

    private fun getTodayDate() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

}