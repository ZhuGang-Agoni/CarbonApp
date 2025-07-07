package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.TravelRecordAdapter
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityGreenTravelBinding

class GreenTravelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGreenTravelBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGreenTravelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 模拟出行记录数据
        val travelList = listOf(
            TravelRecord("公交", "5.2 km", "08:30", R.drawable.ic_bus),
            TravelRecord("骑行", "2.8 km", "09:10", R.drawable.ic_bike),
            TravelRecord("地铁", "7.5 km", "12:20", R.drawable.ic_metro),
            TravelRecord("步行", "1.3 km", "18:05", R.drawable.ic_walk),
            TravelRecord("公交", "3.6 km", "19:40", R.drawable.ic_bus)
        )
        val adapter = TravelRecordAdapter(travelList)
        binding.recyclerViewTravelRecord.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTravelRecord.adapter = adapter
    }
} 