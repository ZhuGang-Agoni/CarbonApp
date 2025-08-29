package com.zg.carbonapp.model

// 垃圾类型枚举
enum class GarbageType {
    KITCHEN,      // 厨余垃圾
    RECYCLABLE,   // 可回收物
    HAZARDOUS,    // 有害垃圾
    OTHER         // 其他垃圾
}

// 垃圾数据类
data class Garbage(
    val imageRes: Int,    // 图片资源ID
    val type: GarbageType // 垃圾类型
)
