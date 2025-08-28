package com.zg.carbonapp.Dao

import java.io.Serializable

// 操作类型枚举
enum class ActionType {
    LIKE,    // 点赞
    SAVE,    // 收藏
    COMMENT  // 评论
}

// 用户操作记录数据类
data class UserAction(
    val userId: String,         // 用户ID
    val feedId: String,         // 动态ID（需要给UserFeed添加feedId字段）
    val actionType: ActionType, // 操作类型
    val timestamp: Long = System.currentTimeMillis(), // 操作时间戳
    val commentContent: String? = null // 评论内容（仅评论时有值）
) : Serializable
