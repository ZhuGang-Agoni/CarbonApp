package com.zg.carbonapp.Dao

//这个是进行排名
data class RankingItem(
    val id:String,
    val userName: String,
    var userEvator:String,
    val carbonCount: Double,// 这个是碳积分
    val rank: Int,
    val isCurrentUser: Boolean = true)

