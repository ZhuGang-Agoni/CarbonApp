package com.zg.carbonapp.Dao

import java.io.Serializable
import java.util.UUID

data class UserFeed(
    var feedId: String = UUID.randomUUID().toString(), // 动态唯一ID，默认生成UUID
    var userId: String,
    var username: String = "",
    var avatar: String,
    var content: String,
    var images: List<String>,
    var likeCount: Int,
    var commentCount: Int, // 改为var，支持评论计数更新
    var shareCount: Int,  // 改为var，支持收藏计数更新（原shareClick实际是收藏）
    var createTime: String,
    var isLiked: Boolean = false,   // 点赞状态
    var isSaved: Boolean = false,
    var isCommented: Boolean = false// 收藏状态（原isSave重命名，规范命名）
) : Serializable