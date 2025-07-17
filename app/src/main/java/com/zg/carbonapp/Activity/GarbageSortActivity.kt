package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Adapter.GarbageRecordAdapter
import com.zg.carbonapp.Dao.GarbageRecord

class GarbageSortActivity : AppCompatActivity() {
    // 挑战入口卡片
    private lateinit var cardChallenge: MaterialCardView
    // 挑战次数显示
    private lateinit var tvChallengeCount: TextView
    // 识别入口卡片
    private lateinit var cardRecognition: MaterialCardView
    // 历史记录卡片
    private lateinit var cardHistory: MaterialCardView
    // 查看全部历史记录按钮
    private lateinit var btnViewAllHistory: Button
    // 最近记录列表
    private lateinit var rvRecentRecords: RecyclerView
    // 最近记录适配器
    private lateinit var recentRecordAdapter: GarbageRecordAdapter

    // onCreate：初始化主界面，设置各功能入口
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_sort)

        cardChallenge = findViewById(R.id.card_challenge)
        cardRecognition = findViewById(R.id.card_recognition)
        cardHistory = findViewById(R.id.card_history)
        btnViewAllHistory = findViewById(R.id.btn_view_all_history)
        rvRecentRecords = findViewById(R.id.rv_recent_records)
        tvChallengeCount = findViewById(R.id.tv_challenge_count)

        cardChallenge.setOnClickListener {
            startActivity(Intent(this, GarbageChallengeActivity::class.java))
        }
        cardRecognition.setOnClickListener {
            startActivity(Intent(this, GarbageRecognitionActivity::class.java))
        }
        btnViewAllHistory.setOnClickListener {
            startActivity(Intent(this, GarbageHistoryActivity::class.java))
        }

        // 设置最近三条记录列表
        rvRecentRecords.layoutManager = LinearLayoutManager(this)
        updateRecentRecords()
        rvRecentRecords.adapter = recentRecordAdapter

        updateChallengeCount()
    }

    // onResume：刷新挑战次数和最近记录
    override fun onResume() {
        super.onResume()
        updateChallengeCount()
        updateRecentRecords()
    }

    // 获取并展示最近三条记录（挑战+识别）
    private fun updateRecentRecords() {
        val recentRecords = GarbageRecordMMKV.getRecentRecords()
            .map {
                when (it) {
                    is GarbageRecord -> it
                    is com.zg.carbonapp.Dao.RecognitionRecord -> GarbageRecord(
                        garbageName = it.garbageName,
                        categoryName = it.category,
                        time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)),
                        categoryIcon = getCategoryIcon(it.category)
                    )
                    is com.zg.carbonapp.Dao.ChallengeRecord -> GarbageRecord(
                        garbageName = "挑战记录",
                        categoryName = "得分: ${it.totalScore}，正确${it.correctCount}/${it.totalQuestions}",
                        time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)),
                        categoryIcon = R.drawable.ic_leaf
                    )
                    else -> GarbageRecord("-", "-", "-", R.drawable.ic_other)
                }
            }
        recentRecordAdapter = GarbageRecordAdapter(recentRecords)
        rvRecentRecords.adapter = recentRecordAdapter
    }

    // 获取并展示今日挑战次数
    private fun updateChallengeCount() {
        val count = GarbageRecordMMKV.getTodayChallengeCount()
        tvChallengeCount.text = "今日挑战次数：$count/3"
    }

    // 根据分类获取对应图标
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