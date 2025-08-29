package com.zg.carbonapp.Tool

import com.zg.carbonapp.Dao.GrowthStage
import com.zg.carbonapp.Dao.Tree
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object TreeUtils {
    // 计算树的生长天数（考虑浇水加速：浇水后24小时内生长速度×1.5）
    fun calculateGrowthDays(tree: Tree): Int {
        val currentTime = System.currentTimeMillis()
        val plantTime = tree.plantTime
        val totalMs = currentTime - plantTime

        // 计算浇水加速的时间（最近24小时内浇水则加速）
        val isWatered = currentTime - tree.lastWaterTime < TimeUnit.HOURS.toMillis(24)
        val speed = if (isWatered) tree.growthSpeed * 1.5f else tree.growthSpeed

        return (TimeUnit.MILLISECONDS.toDays(totalMs) * speed).toInt()
    }

    // 格式化时间（种植时间）
    fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timeMs)
    }

    // 获取所有阶段的时间节点（用于时间轴）
    fun getStageTimeline(tree: Tree): List<Pair<GrowthStage, String>> {
        val growthDays = calculateGrowthDays(tree)
        return GrowthStage.values().map { stage ->
            // 计算该阶段的日期（种植时间 + 阶段所需天数）
            val stageTime = tree.plantTime + TimeUnit.DAYS.toMillis(stage.minDays.toLong())
            Pair(stage, formatTime(stageTime))
        }.sortedBy { it.first.minDays }
    }
}