package com.zg.carbonapp.Dao

import java.io.Serializable

data class Comment(
    val userId: String,
    val feedId: String,
    val content: String,
    val commentTime: String,
    val userName: String,
    val avatar: String
) : Serializable