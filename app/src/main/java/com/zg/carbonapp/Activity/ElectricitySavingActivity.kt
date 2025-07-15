package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R
import java.util.*

class ElectricitySavingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity_saving)

        // 7天用电量模拟（10~30度电）
        val random = Random()
        val weekData = List(7) { 10 + random.nextInt(21) } // 10~30

        // 7个ProgressBar和7个数值TextView
        val pbIds = listOf(
            R.id.pbDay1, R.id.pbDay2, R.id.pbDay3, R.id.pbDay4, R.id.pbDay5, R.id.pbDay6, R.id.pbDay7
        )
        val tvIds = listOf(
            R.id.tvVal1, R.id.tvVal2, R.id.tvVal3, R.id.tvVal4, R.id.tvVal5, R.id.tvVal6, R.id.tvVal7
        )
        pbIds.forEachIndexed { i, pbId ->
            val pb = findViewById<ProgressBar>(pbId)
            pb.max = 30
            pb.progress = weekData[i]
        }
        tvIds.forEachIndexed { i, tvId ->
            val tv = findViewById<TextView>(tvId)
            tv.text = "${weekData[i]}"
        }

        // 数据分析卡片内容动态计算
        val total = weekData.sum()
        val avg = weekData.average()
        // 假设节能率为 100 - (本周总用电/210*100)，210为满负荷用电（30*7）
        val saveRate = 100 - (total * 100 / 210.0)

        findViewById<TextView>(R.id.tvTotal).text = "本周总用电量：${String.format("%.1f", total.toDouble())} kWh"
        findViewById<TextView>(R.id.tvAvg).text = "平均每日用电：${String.format("%.1f", avg)} kWh"
        findViewById<TextView>(R.id.tvSaveRate).text = "节能率：${String.format("%.0f", saveRate)}%"
    }
} 