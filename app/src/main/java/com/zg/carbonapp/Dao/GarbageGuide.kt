package com.zg.carbonapp.Dao

import androidx.annotation.DrawableRes

data class GarbageCategory(
    val id: Int,
    val name: String,
    @DrawableRes val icon: Int,
    val standard: String,
    val fullExamples: List<String>, // 完整例子（用于搜索）
    val displayExamples: List<String>, // 展示用例子（精简）
    val misunderstandings: List<String>
)