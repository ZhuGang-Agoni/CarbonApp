package com.zg.carbonapp.Dao

import java.io.Serializable
import java.util.UUID

data class UserFeed(
    val feedId: String = UUID.randomUUID().toString(), // 动态唯一ID，默认生成UUID
    val userId: String,
    val username: String = "",
    val avatar: String,
    val content: String,
    val images: List<String>,
    var likeCount: Int,
    var commentCount: Int, // 改为var，支持评论计数更新
    var shareCount: Int,  // 改为var，支持收藏计数更新（原shareClick实际是收藏）
    val createTime: String,
    var isLiked: Boolean = false,   // 点赞状态
    var isCommented: Boolean = false, // 评论状态（原isComment重命名，避免歧义）
    var isSaved: Boolean = false    // 收藏状态（原isSave重命名，规范命名）
) : Serializable