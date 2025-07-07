package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.FriendRankingAdapter
import com.zg.carbonapp.Dao.FriendRanking
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityCarbonFootprintBinding

class CarbonFootprintActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonFootprintBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 模拟步数和可种树数
        val todaySteps = (5000..15000).random()
        val treeCount = (todaySteps * 0.00004 / 0.5).toInt() // 0.5g CO₂ = 1棵树
        binding.tvStepCount.text = "$todaySteps 步"
        binding.tvTreeCount.text = "$treeCount 棵"

        // 跳转到今日步数详情
        binding.tvStepCount.setOnClickListener {
            val intent = Intent(this, FootprintActivity::class.java)
            startActivity(intent)
        }
        // 跳转到种树详情
        binding.tvTreeCount.setOnClickListener {
            val intent = Intent(this, PlantTreeActivity::class.java)
            startActivity(intent)
        }

        // 激励语和标题已在布局中静态设置

        // 排行榜数据
        val friendRankingList = listOf(
            FriendRanking(1, "小明", R.drawable.ic_profile, 12),
            FriendRanking(2, "小红", R.drawable.ic_profile, 10),
            FriendRanking(3, "小刚", R.drawable.ic_profile, 8),
            FriendRanking(4, "小美", R.drawable.ic_profile, 7)
        )
        val rankingAdapter = FriendRankingAdapter(friendRankingList.sortedByDescending { it.treeCount })
        binding.recyclerViewRanking.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRanking.adapter = rankingAdapter
    }
} 