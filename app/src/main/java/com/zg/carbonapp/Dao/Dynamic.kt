// Dynamic.kt
package com.zg.carbonapp.Dao

import java.io.Serializable

data class Dynamic(
    val userId: String,
    val content: String,
    val feedId: String,
    val createTime: String,
    val avatar: String,
    val userName: String,
    val pics: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
) : Serializable