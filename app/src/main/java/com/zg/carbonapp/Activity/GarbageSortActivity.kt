package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.zg.carbonapp.Adapter.GarbageRecordAdapter
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.Dao.RecognitionRecord
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Utils.ReportUtils
import java.util.*

class GarbageSortActivity : AppCompatActivity() {
    // 原有控件
    private lateinit var cardChallenge: MaterialCardView
    private lateinit var tvChallengeCount: TextView
    private lateinit var cardRecognition: MaterialCardView
    private lateinit var cardHistory: MaterialCardView
    private lateinit var btnViewAllHistory: Button
    private lateinit var rvRecentRecords: RecyclerView
    private lateinit var recentRecordAdapter: GarbageRecordAdapter

    private lateinit var backButton: ImageView
    // 新增控件：周报入口
    private lateinit var cardWeeklyReport: MaterialCardView
    private lateinit var tvReportDesc: TextView

    // 其他新增控件
    private lateinit var tvGarbageTip: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvCarbonReduction: TextView
    private lateinit var tvLevel: TextView

    // 分类小贴士列表（随机展示）
    private val garbageTips = listOf(
        "小贴士：废旧电池属于有害垃圾，需单独投放至专用回收箱哦~",
        "小贴士：外卖餐盒若有油污，需冲洗干净后再作为可回收物投放~",
        "小贴士：菜叶、果皮等厨余垃圾可用于堆肥，变废为宝！",
        "小贴士：旧衣服属于可回收物，捐赠或投放至衣物回收箱均可~",
        "小贴士：碎玻璃需用报纸包裹后投放，避免划伤回收人员~"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_sort)

        // 绑定原有控件
        cardChallenge = findViewById(R.id.card_challenge)
        cardRecognition = findViewById(R.id.card_recognition)
        cardHistory = findViewById(R.id.card_history)
        btnViewAllHistory = findViewById(R.id.btn_view_all_history)
        rvRecentRecords = findViewById(R.id.rv_recent_records)
        tvChallengeCount = findViewById(R.id.tv_challenge_count)

         backButton=findViewById(R.id.backButton)
        // 绑定新增控件（周报入口）
        cardWeeklyReport = findViewById(R.id.card_weekly_report)
        tvReportDesc = findViewById(R.id.tv_report_desc)
        tvGarbageTip = findViewById(R.id.tv_garbage_tip)
        tvTotalCount = findViewById(R.id.tv_total_count)
        tvCarbonReduction = findViewById(R.id.tv_carbon_reduction)
        tvLevel = findViewById(R.id.tv_level)

        // 初始化列表
        rvRecentRecords.layoutManager = LinearLayoutManager(this)
        updateRecentRecords()
        rvRecentRecords.adapter = recentRecordAdapter

        // 初始化数据
        updateChallengeCount()
        updateStatisticData()
        updateRandomTip()
        updateReportDesc() // 初始化周报描述

        // 点击事件
        cardChallenge.setOnClickListener {
            startActivity(Intent(this, GarbageChallengeActivity::class.java))
        }
        cardRecognition.setOnClickListener {
            startActivity(Intent(this, GarbageRecognitionActivity::class.java))
        }
        btnViewAllHistory.setOnClickListener {
            startActivity(Intent(this, GarbageHistoryActivity::class.java))
        }

        backButton.setOnClickListener {finish() }

        // 新增：周报入口点击事件
        cardWeeklyReport.setOnClickListener {
            val allRecords = GarbageRecordMMKV.getRecognitionRecords() ?: run {
                Toast.makeText(this, "暂无分类记录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 根据当前周是否结束，显示对应周期的周报
            val targetRecords = if (ReportUtils.isCurrentWeekCompleted()) {
                // 周日：显示本周记录（周一至周日）
                getCurrentWeekRecords(allRecords)
            } else {
                // 周一至周六：显示上周记录（上周一至上周日）
                ReportUtils.getLastWeekRecords(allRecords)
            }

            if (targetRecords.isEmpty()) {
                Toast.makeText(this, "暂无周报数据", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, GarbageReportActivity::class.java)
                // 使用 putParcelableArrayListExtra
                intent.putParcelableArrayListExtra("weekly_records", ArrayList(targetRecords))
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateChallengeCount()
        updateRecentRecords()
        updateStatisticData() // 刷新统计数据
        checkWeeklyReport()
        updateReportDesc() // 刷新周报描述
    }

    // 新增：更新周报入口描述（显示当前可查看的周期）
    private fun updateReportDesc() {
        val (startDate, endDate) = if (ReportUtils.isCurrentWeekCompleted()) {
            // 周日：显示本周周期
            ReportUtils.getCurrentWeekDateRange()
        } else {
            // 周一至周六：显示上周周期
            ReportUtils.getLastWeekDateRange()
        }
        tvReportDesc.text = "查看 $startDate - $endDate 分类统计与建议"
    }

    // 新增：获取本周记录（周一至当前时间）
    private fun getCurrentWeekRecords(records: List<RecognitionRecord>): List<RecognitionRecord> {
        val calendar = Calendar.getInstance()
        // 本周一0点
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val currentMonday = calendar.timeInMillis

        // 当前时间（截止到现在）
        val currentTime = System.currentTimeMillis()

        return records.filter { record ->
            record.timestamp in currentMonday..currentTime
        }
    }

    // 随机展示一条分类小贴士
    private fun updateRandomTip() {
        val randomTip = garbageTips.random()
        tvGarbageTip.text = randomTip
    }

    // 更新统计数据（总次数、减碳量、等级）
    private fun updateStatisticData() {
        // 总分类次数（挑战+识别）
        val totalCount = GarbageRecordMMKV.getAllRecords().size
        tvTotalCount.text = totalCount.toString()

        // 累计减碳量（假设每次分类平均减碳0.02kg）
        val carbonReduction = totalCount * 0.02f
        tvCarbonReduction.text = String.format("%.1f", carbonReduction)

        // 环保等级（每10次升级一级）
        val level = (totalCount / 10) + 1
        tvLevel.text = "Lv.$level"
    }

    // 原有方法：更新最近记录
    private fun updateRecentRecords() {
        val recentRecords = GarbageRecordMMKV.getRecentRecords()
            .map {
                when (it) {
                    is GarbageRecord -> it
                    is RecognitionRecord -> GarbageRecord(
                        garbageName = it.garbageName,
                        categoryName = it.category,
                        time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp)),
                        categoryIcon = getCategoryIcon(it.category)
                    )
                    is com.zg.carbonapp.Dao.ChallengeRecord -> GarbageRecord(
                        garbageName = "挑战记录",
                        categoryName = "得分: ${it.totalScore}，正确${it.correctCount}/${it.totalQuestions}",
                        time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp)),
                        categoryIcon = R.drawable.ic_leaf
                    )
                    else -> GarbageRecord("-", "-", "-", R.drawable.ic_other)
                }
            }
        recentRecordAdapter = GarbageRecordAdapter(recentRecords)
        rvRecentRecords.adapter = recentRecordAdapter
    }

    // 原有方法：更新挑战次数
    private fun updateChallengeCount() {
        val count = GarbageRecordMMKV.getTodayChallengeCount()
        tvChallengeCount.text = "今日挑战次数：$count/3"
    }

    // 原有方法：检查周报提醒
    private fun checkWeeklyReport() {
        val allRecords = GarbageRecordMMKV.getRecognitionRecords() ?: return

        if (ReportUtils.shouldShowWeeklyReport(allRecords)) {
            val lastWeekRecords = ReportUtils.getLastWeekRecords(allRecords)
            showReportDialog(lastWeekRecords)
        }
    }

    /**
     * 显示周报提醒对话框
     */
    private fun showReportDialog(records: List<RecognitionRecord>) {
        AlertDialog.Builder(this)
            .setTitle("垃圾分类周报")
            .setMessage("您上个星期的垃圾分类记录报告已经生成完毕，点击查看去签收你的专属报告吧！")
            .setPositiveButton("查看报告") { _, _ ->
                ReportUtils.startReportActivity(this, records)
            }
            .setNegativeButton("稍后查看") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 原有方法：获取分类图标
    private fun getCategoryIcon(category: String): Int {
        return when (category) {
            "可回收物" -> R.drawable.ic_recyclable
            "有害垃圾" -> R.drawable.ic_hazardous
            "厨余垃圾" -> R.drawable.ic_kitchen
            "其他垃圾" -> R.drawable.ic_other
            else -> R.drawable.ic_other
        }
    }
}