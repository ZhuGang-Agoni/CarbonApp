package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Adapter.RecordsAdapter
import com.zg.carbonapp.MMKV.RecordManager
import com.zg.carbonapp.R


class RecordsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        recyclerView = findViewById(R.id.records_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val records = RecordManager.getRecordsByTime()
        adapter = RecordsAdapter(records)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 当返回该界面时，刷新记录
        val records = RecordManager.getRecordsByTime()
        adapter.updateRecords(records)
    }
}