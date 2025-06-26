package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.zg.carbonapp.Adapter.TreeIconAdapter
import com.zg.carbonapp.databinding.ActivityPlantTreeBinding
import com.zg.carbonapp.R

class PlantTreeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlantTreeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 模拟本周步数
        val stepList = generateStepData()
        val totalSteps = stepList.sumOf { it.steps }
        val totalCarbon = totalSteps * 0.00004
        val treeCount = (totalCarbon / 1800).toInt() // 1800g CO₂ = 1棵树

        binding.tvTotalSteps.text = "本周总步数：$totalSteps"
        binding.tvTotalCarbon.text = "累计碳吸收：${"%.2f".format(totalCarbon)}g"
        binding.tvTreeCount.text = "本周可种树：$treeCount 棵"

        // 可视化种树
        val treeList = List(treeCount) { R.drawable.ic_tree }
        val adapter = TreeIconAdapter(treeList)
        binding.recyclerViewTrees.layoutManager = GridLayoutManager(this, 5)
        binding.recyclerViewTrees.adapter = adapter
    }

    private fun generateStepData(): List<StepRecord> {
        val calendar = java.util.Calendar.getInstance()
        return (0..6).map {
            val steps = (5000..15000).random()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            StepRecord(steps)
        }
    }

    data class StepRecord(val steps: Int)
} 