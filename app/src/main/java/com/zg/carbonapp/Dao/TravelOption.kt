package com.zg.carbonapp.Dao

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TravelOption(
    val type: String,
    val iconRes: Int,
    val weatherSuitability: Float,  // 0-5分
    val carbonFootprint: Float,     // 碳排放(kg CO2/km)
    val healthBenefit: Float,       // 0-5分
    val timeEfficiency: Float,      // 0-5分
    val color: Int,
    val recommendation: String
):Parcelable {
    // 计算综合得分
    fun overallScore(): Float {
        return (weatherSuitability * 0.4f) +
                ((5 - carbonFootprint) * 0.3f) +
                (healthBenefit * 0.2f) +
                (timeEfficiency * 0.1f)
    }

    companion object {
        fun empty(): TravelOption {
            return TravelOption(
                type = "",
                iconRes = 0,
                weatherSuitability = 0f,
                carbonFootprint = 0f,
                healthBenefit = 0f,
                timeEfficiency = 0f,
                color = Color.TRANSPARENT,
                recommendation = ""
            )
        }
    }
}