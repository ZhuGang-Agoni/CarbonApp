package com.zg.carbonapp.Dao

data class FriendRanking(
    val id: Int,
    val nickname: String,
    val avatarResId: Int, // 头像资源id
    val treeCount: Int
) 