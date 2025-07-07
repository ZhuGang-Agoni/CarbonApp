package com.zg.carbonapp.Activity

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zg.carbonapp.Adapter.GarbageRecordAdapter
import com.zg.carbonapp.Dao.GarbageRecord
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.MyToast

class GarbageSortActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnHelp: ImageButton
    private lateinit var cardRecyclable: MaterialCardView
    private lateinit var cardHazardous: MaterialCardView
    private lateinit var cardKitchen: MaterialCardView
    private lateinit var cardOther: MaterialCardView
    private lateinit var tvViewAll: TextView
    private lateinit var rvRecords: RecyclerView
    private lateinit var fabCamera: FloatingActionButton

    private lateinit var recordAdapter: GarbageRecordAdapter
    private val recordList = mutableListOf<GarbageRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_sort)

        initViews()
        initListeners()
        initData()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        btnBack = findViewById(R.id.btn_back)
        btnHelp = findViewById(R.id.btn_help)
        cardRecyclable = findViewById(R.id.card_recyclable)
        cardHazardous = findViewById(R.id.card_hazardous)
        cardKitchen = findViewById(R.id.card_kitchen)
        cardOther = findViewById(R.id.card_other)
        tvViewAll = findViewById(R.id.tv_view_all)
        rvRecords = findViewById(R.id.rv_records)
        fabCamera = findViewById(R.id.fab_camera)
    }

    private fun initListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnHelp.setOnClickListener {
            MyToast.sendToast("垃圾分类帮助信息", this)
        }

        cardRecyclable.setOnClickListener {
            MyToast.sendToast("可回收物：废纸、废塑料、废玻璃、废金属、废织物等", this)
        }

        cardHazardous.setOnClickListener {
            MyToast.sendToast("有害垃圾：废电池、废灯管、废药品、废油漆等", this)
        }

        cardKitchen.setOnClickListener {
            MyToast.sendToast("厨余垃圾：剩菜剩饭、果皮、蛋壳、茶渣等", this)
        }

        cardOther.setOnClickListener {
            MyToast.sendToast("其他垃圾：除以上三种之外的其他生活废弃物", this)
        }

        tvViewAll.setOnClickListener {
            MyToast.sendToast("查看全部分类记录", this)
        }

        fabCamera.setOnClickListener {
            MyToast.sendToast("拍照识别垃圾类型", this)
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            val searchText = etSearch.text.toString()
            if (searchText.isNotEmpty()) {
                MyToast.sendToast("搜索：$searchText", this)
            }
            true
        }
    }

    private fun initData() {
        // 初始化记录数据
        recordList.add(GarbageRecord("塑料瓶", "可回收物", "今天 14:30", R.drawable.ic_recyclable))
        recordList.add(GarbageRecord("电池", "有害垃圾", "今天 12:15", R.drawable.ic_hazardous))
        recordList.add(GarbageRecord("苹果皮", "厨余垃圾", "今天 10:20", R.drawable.ic_kitchen))

        // 设置RecyclerView
        recordAdapter = GarbageRecordAdapter(recordList)
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = recordAdapter
    }
} 