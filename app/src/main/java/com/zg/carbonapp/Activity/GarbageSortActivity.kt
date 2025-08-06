package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
    // 挑战进度组件
    private lateinit var challengeProgressView: android.view.View

    // onCreate：初始化主界面，设置各功能入口
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置系统UI，使状态栏透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        
        setContentView(R.layout.activity_garbage_sort)

        cardChallenge = findViewById(R.id.card_challenge)
        cardRecognition = findViewById(R.id.card_recognition)
        cardHistory = findViewById(R.id.card_history)
        btnViewAllHistory = findViewById(R.id.btn_view_all_history)
        rvRecentRecords = findViewById(R.id.rv_recent_records)
        challengeProgressView = findViewById(R.id.challenge_progress)

        // 设置标题栏按钮点击事件
        findViewById<android.widget.ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        findViewById<android.widget.ImageView>(R.id.searchButton).setOnClickListener {
            showToast("搜索功能开发中")
        }
        
        // 启动卡片动画
        startCardAnimations()

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
        
        // 更新挑战进度
        updateChallengeProgress()
    }

    // onResume：刷新最近记录
    override fun onResume() {
        super.onResume()
        updateRecentRecords()
        updateChallengeProgress()
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
                        categoryIcon = R.drawable.ic_timeline_record
                    )
                    else -> GarbageRecord("-", "-", "-", R.drawable.ic_garbage_other)
                }
            }
        recentRecordAdapter = GarbageRecordAdapter(recentRecords)
        rvRecentRecords.adapter = recentRecordAdapter
    }



    // 根据分类获取对应图标
    private fun getCategoryIcon(category: String): Int {
        return when (category) {
            "可回收物" -> R.drawable.ic_garbage_recyclable
            "有害垃圾" -> R.drawable.ic_garbage_harmful
            "厨余垃圾" -> R.drawable.ic_garbage_kitchen
            "其他垃圾" -> R.drawable.ic_garbage_other
            else -> R.drawable.ic_garbage_other
        }
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // 更新挑战进度
    private fun updateChallengeProgress() {
        val count = GarbageRecordMMKV.getTodayChallengeCount()
        val progress = (count * 100 / 3).coerceAtMost(100)
        
        val progressBar = challengeProgressView.findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        val progressText = challengeProgressView.findViewById<android.widget.TextView>(R.id.tv_progress_text)
        
        progressBar.progress = progress
        progressText.text = "$count/3"
    }
    
    // 启动卡片动画
    private fun startCardAnimations() {
        val slideUpAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        val breathAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.breath_animation)
        
        // 为挑战卡片添加动画
        cardChallenge.startAnimation(slideUpAnimation)
        findViewById<android.widget.ImageView>(R.id.iv_challenge_icon).startAnimation(breathAnimation)
        
        // 为识别卡片添加动画（延迟200ms）
        cardRecognition.postDelayed({
            cardRecognition.startAnimation(slideUpAnimation)
            findViewById<android.widget.ImageView>(R.id.iv_recognition_icon).startAnimation(breathAnimation)
        }, 200)
    }
} 