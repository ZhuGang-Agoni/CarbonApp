package com.zg.carbonapp.Dao


import java.util.Calendar

/**
 * 用电及碳排放记录数据类
 * @param id 用户id 唯一标识
 * @param date 日期（毫秒时间戳）
 * @param electricity 用电量（kWh）
 * @param carbon 碳排放量（kg，由用电量计算得出）
 * @param dayOfWeek 星期几（1-7，对应周一到周日）
 */
data class ElectricityRecord(
    val id:String,//id永远是和用户关联
    val date: Long,
    val electricity: Double,
    val carbon: Double,
    val dayOfWeek: Int
) {
    // 格式化日期为"MM-DD"
    val formatDate: String
        get() {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            return "${cal.get(Calendar.MONTH)+1}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
}
