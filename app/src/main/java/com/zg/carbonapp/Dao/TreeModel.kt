package com.zg.carbonapp.Dao

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

// 树木数据模型（仅保留核心字段）
@Parcelize
data class TreeModel(
    val id: String = UUID.randomUUID().toString(), // 唯一ID
    val treeType: String = "普通树", // 树木类型
    val plantTime: Long = System.currentTimeMillis(), // 种植时间戳
    val carbonReduction: Double = 20.0, // 单棵树对应碳减排（kg）
    var positionX: Float = 0f, // 3D场景X坐标
    var positionZ: Float = 0f  // 3D场景Z坐标
) : Parcelable