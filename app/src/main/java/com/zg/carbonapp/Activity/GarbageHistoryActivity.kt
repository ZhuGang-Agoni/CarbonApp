package com.zg.carbonapp.Activity

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Adapter.GarbageRecordAdapter
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button

class GarbageHistoryActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var rvRecords: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var btnClear: Button
    
    private lateinit var recordAdapter: GarbageRecordAdapter
    private val recordList = mutableListOf<GarbageRecord>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_history)
        
        initViews()
        initListeners()
        loadAllRecords()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        rvRecords = findViewById(R.id.rv_records)
        tvEmpty = findViewById(R.id.tv_empty)
        btnClear = findViewById(R.id.btn_clear)
    }
    
    private fun initListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        btnClear.setOnClickListener {
            GarbageRecordMMKV.clearRecords()
            loadAllRecords()
            android.widget.Toast.makeText(this, "已清空", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadAllRecords() {
        // 获取所有挑战记录
        val challengeRecords = GarbageRecordMMKV.getChallengeRecords()
        val recognitionRecords = GarbageRecordMMKV.getRecognitionRecords()
        
        recordList.clear()
        
        // 添加挑战记录
        challengeRecords.forEach { record ->
            recordList.add(
                GarbageRecord(
                    garbageName = "挑战记录",
                    categoryName = "得分: ${record.totalScore}，正确${record.correctCount}/${record.totalQuestions}",
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                    categoryIcon = R.drawable.ic_leaf // 或其它icon
                )
            )
        }
        
        // 添加识别记录
        recognitionRecords.forEach { record ->
            recordList.add(
                GarbageRecord(
                    garbageName = record.garbageName,
                    categoryName = record.category,
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                    categoryIcon = getCategoryIcon(record.category)
                )
            )
        }
        
        // 按时间排序
        recordList.sortByDescending { 
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.time)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        
        if (recordList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvRecords.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvRecords.visibility = View.VISIBLE
            
            recordAdapter = GarbageRecordAdapter(recordList) { record ->
                // 判断是否为挑战记录
                if (record.garbageName == "挑战记录") {
                    // 找到原始 ChallengeRecord
                    val challengeRecords = com.zg.carbonapp.MMKV.GarbageRecordMMKV.getChallengeRecords()
                    val original = challengeRecords.find {
                        "得分: ${it.totalScore}，正确${it.correctCount}/${it.totalQuestions}" == record.categoryName &&
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) == record.time
                    }
                    if (original != null && original.totalScore == original.totalQuestions * 10) {
                        // 满分，弹窗跳转
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("满分挑战记录")
                            .setMessage("恭喜你满分！要不要试试更高级的挑战？")
                            .setPositiveButton("去试试") { _, _ ->
                                val intent = android.content.Intent(this, GameMenuActivity::class.java)
                                startActivity(intent)
                            }
                            .setNegativeButton("下次吧", null)
                            .show()
                        return@GarbageRecordAdapter
                    }
                }
                // 查找对应的 RecognitionRecord 以获取 explanation
                val recognition = com.zg.carbonapp.MMKV.GarbageRecordMMKV.getRecognitionRecords().find {
                    it.garbageName == record.garbageName &&
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) == record.time
                }
                val explanation = recognition?.explanation ?: "暂无详细说明"
                val message = "分类：${record.categoryName}\n\n时间：${record.time}\n\n说明：$explanation"
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(record.garbageName)
                    .setMessage(message)
                    .setPositiveButton("知道了", null)
                    .show()
            }
            rvRecords.layoutManager = LinearLayoutManager(this)
            rvRecords.adapter = recordAdapter
        }
    }
    
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