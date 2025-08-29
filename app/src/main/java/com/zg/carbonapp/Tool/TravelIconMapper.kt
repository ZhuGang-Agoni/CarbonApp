package com.zg.carbonapp.Tool

import android.content.Context
import com.zg.carbonapp.R

object TravelIconMapper {
    // 根据标识字符串获取资源ID（运行时动态获取，避免ID变化问题）
    fun getIconResId(context: Context, tag: String): Int {
        return when (tag) {
            "walk" -> R.drawable.walk
            "bike" -> R.drawable.bike
            "bus" -> R.drawable.bus
            "drive" -> R.drawable.drive_eta
            else -> R.drawable.destination
        }
    }
}