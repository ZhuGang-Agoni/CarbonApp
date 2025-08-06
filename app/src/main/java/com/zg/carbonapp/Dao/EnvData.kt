package com.zg.carbonapp.Dao

data class EnvData(
    // 原有特征
    val batteryTemp: Float,        // 电池温度（℃）
    val batteryLevel: Int,         // 电池电量（%）
    val isCharging: Boolean,       // 是否充电
    val lightLevel: Float,         // 光线强度（lx）
    val tempChangeRate: Float,     // 温度变化率（℃/min）
    val isNight: Boolean,          // 是否夜间
    val predictedTemp: Float,      // 模型预测的真实温度（℃）
    val predictedHumidity: Float,  // 模型预测的真实湿度（%）
    val isAirconOn: Boolean,       // 空调是否开启（辅助判断）

    val pressure: Float,           // 气压（hPa）

)
