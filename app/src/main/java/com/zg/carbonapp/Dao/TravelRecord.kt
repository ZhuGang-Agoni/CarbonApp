package com.zg.carbonapp.Dao

import java.util.Date

data class TravelRecord(
    val userId: String,        // 用户ID（关联用户）
    // 出行方式（公交/骑行等）
    val totalCarbon:String="0",//这个是总共的碳排放量哈
    val todayCarbon:String="0",// 这个是当天的碳排放量
    val carbonPoint:String="0",// 这是对应的一个碳积分
    val list:List<ItemTravelRecord>
)
