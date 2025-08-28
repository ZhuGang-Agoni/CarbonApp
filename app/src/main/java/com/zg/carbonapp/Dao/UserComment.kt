package com.zg.carbonapp.Dao

import java.util.UUID

data class UserComment(
    val feedId: String , // 动态唯一ID，默认生成UUID
    val userId: String,
    val username: String = "",
    val avatar: String,
    val content: String,
    val commentTime: String
)
