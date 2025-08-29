package com.zg.carbonapp.Dao

import com.zg.carbonapp.R

data class LowCarbonKnowledge(
    val title: String,
    val content: String,
    val imageResId: Int= R.drawable.carbon_knowledge,
    val lottieResId: Int = 0  // 使用Lottie JSON动画资源
)