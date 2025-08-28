package com.zg.carbonapp.Tool


import com.zg.carbonapp.Dao.ActivityStatus
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val currentTime get() = System.currentTimeMillis()

    // 核心：根据活动时间计算状态
    fun getActivityStatus(startTime: String, endTime: String): ActivityStatus {
        return try {
            val start = sdf.parse(startTime)?.time ?: 0
            val end = sdf.parse(endTime)?.time ?: 0

            when {
                currentTime < start -> ActivityStatus.NOT_STARTED
                currentTime in start until end -> ActivityStatus.ON_GOING
                else -> ActivityStatus.ENDED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ActivityStatus.ENDED // 解析失败默认已结束
        }
    }
}
