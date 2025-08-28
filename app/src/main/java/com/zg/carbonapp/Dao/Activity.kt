// Activity.kt (数据类)
package com.zg.carbonapp.Dao

data class Activity(
    val id: Int,
    val name: String,
    val description: String,
    val imageRes: Int,
    val startTime: String, // 格式: yyyy-MM-dd HH:mm
    val endTime: String,   // 格式: yyyy-MM-dd HH:mm
    val location: String,
    val points: Int,
    var participantCount: Int,
    var joined: Boolean    // 仅记录是否报名，状态完全动态计算
)

// 状态枚举保持不变
enum class ActivityStatus {
    NOT_STARTED, ON_GOING, ENDED
}