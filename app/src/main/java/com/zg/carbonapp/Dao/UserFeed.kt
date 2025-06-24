package com.zg.carbonapp.Dao

import java.io.Serializable

data class UserFeed(
    val userId: String,
    val username: String,
    val avatar: String,
    val content: String,
    val images: List<String>,
    var likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val createTime: String,
    var isLiked: Boolean = false
):Serializable
