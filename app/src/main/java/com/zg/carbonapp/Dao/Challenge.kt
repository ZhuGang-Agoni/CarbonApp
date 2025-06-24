package com.zg.carbonapp.Dao

data class Challenge(
    val id: Int,
    val title: String,
    val description: String,
    val target: String,
    var progress: Int = 0,
    var isJoined: Boolean = false,
    val total: Int,
    var lastCheckInDate: String? = null,
    var isCompleted: Boolean = false
)