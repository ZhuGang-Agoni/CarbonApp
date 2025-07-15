package com.zg.carbonapp.Dao

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 支持序列化，用于Activity间传递
@Parcelize
data class Product(
    val barcode: String, // 条码（可为"default"）
    val name: String,    // 产品名称
    val brand: String,   // 品牌名（新增字段，方便展示）
    val category: String,// 类别
    val carbonFootprint: Double, // 碳足迹（kgCO₂e/单位）
    val unit: String     // 单位（瓶/盒/升等）
) : Parcelable