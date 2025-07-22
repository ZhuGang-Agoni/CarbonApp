package com.zg.carbonapp.Dao

// 游戏记录数据类（和后端JSON字段一致）
data class GameHistoryRecord(
    val id: Long,
    val score: Int,
    val carbonReduction: Int,
    val date: String // 后端返回的时间字符串，如 "2024-05-20 15:30:00"
)

