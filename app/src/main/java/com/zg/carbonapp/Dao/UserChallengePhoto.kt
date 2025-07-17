package com.zg.carbonapp.Dao

data class UserChallengePhoto(
    val imagePath: String,      // 本地图片路径
    val correctCategory: String,// 正确分类
    val explanation: String,    // 分类原由
    val timestamp: Long         // 加入时间
) 