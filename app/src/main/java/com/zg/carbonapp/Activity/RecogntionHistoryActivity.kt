package com.zg.carbonapp.Activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Adapter.RecognitionRecordAdapter
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File

class RecognitionHistoryActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var rvRecords: RecyclerView
    private lateinit var tvEmpty: com.google.android.material.card.MaterialCardView
    private lateinit var btnClear: TextView

    private lateinit var recordAdapter: RecognitionRecordAdapter
    private val recordList = mutableListOf<GarbageRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recogntion_history) // 使用相同的布局

        // 设置状态栏透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

        initViews()
        initListeners()
        loadRecognitionRecords()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        rvRecords = findViewById(R.id.rv_records)
        tvEmpty = findViewById(R.id.tv_empty)
        btnClear = findViewById(R.id.btn_clear)

        // 修改标题
        tvTitle.text = "识别记录"
    }

    private fun initListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        btnClear.setOnClickListener {
            // 只清空识别记录
            GarbageRecordMMKV.clearRecognitionRecords()
            loadRecognitionRecords()
            android.widget.Toast.makeText(this, "已清空识别记录", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecognitionRecords() {
        // 只获取识别记录
        val recognitionRecords = GarbageRecordMMKV.getRecognitionRecords()

        recordList.clear()

        // 添加识别记录
        recognitionRecords.forEach { record ->
            recordList.add(
                GarbageRecord(
                    garbageName = record.garbageName,
                    categoryName = record.category,
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                    categoryIcon = R.drawable.ic_paizhao2,
                    imagePath = record.imageUrl
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

        // 更新适配器
        updateAdapter()
    }

    private fun updateAdapter() {
        if (recordList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvRecords.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvRecords.visibility = View.VISIBLE

            recordAdapter = RecognitionRecordAdapter(recordList) { record ->
                // 显示包含图片的详情对话框
                showRecordDetailDialog(record, record.categoryName)
            }
            rvRecords.layoutManager = LinearLayoutManager(this)
            rvRecords.adapter = recordAdapter
        }
    }

    // 显示包含图片的记录详情对话框
    private fun showRecordDetailDialog(record: GarbageRecord, explanation: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_record_detail, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvCategory = dialogView.findViewById<TextView>(R.id.tv_dialog_category)
        val tvTime = dialogView.findViewById<TextView>(R.id.tv_dialog_time)
        val tvExplanation = dialogView.findViewById<TextView>(R.id.tv_dialog_explanation)
        val ivImage = dialogView.findViewById<ImageView>(R.id.iv_dialog_image)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_dialog_close)

        tvTitle.text = record.garbageName
        tvCategory.text = "分类：${record.categoryName}"
        tvTime.text = "时间：${record.time}"
        tvExplanation.text = "说明：$explanation"

        // 使用 Glide 加载图片（如果有）
        if (!record.imagePath.isNullOrEmpty()) {
            val file = File(record.imagePath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .apply(
                        RequestOptions()
                            .centerCrop())
                    .into(ivImage)
                ivImage.visibility = View.VISIBLE
            } else {
                ivImage.setImageResource(R.drawable.ic_paizhao2)
                ivImage.visibility = View.VISIBLE
            }
        } else {
            ivImage.setImageResource(R.drawable.ic_paizhao2)
            ivImage.visibility = View.VISIBLE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}