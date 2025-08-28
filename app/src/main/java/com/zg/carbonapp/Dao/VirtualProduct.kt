package com.zg.carbonapp.Dao

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VirtualProduct(
    val id: Int,
    val name: String,
    val description: String,
    val points: Int,
    val type: ProductType, // 商品类型
    val iconRes: Int,      // 商品图标
    val unlockRes: Int,    // 解锁后使用的资源
    val rarity: Rarity,    // 稀有度
    val effectRes: Int = 0, // 特效资源（可选）
    val source: ProductSource, // 新增来源字段
) : Parcelable

enum class ProductType { BADGE, AVATAR_FRAME, AVATAR_ITEM }
enum class Rarity { COMMON, RARE, EPIC, LEGENDARY }
enum class ProductSource{mall,task}

data class TimelineNode(
    val product: VirtualProduct,
    var isUnlocked: Boolean = false,
    val requiredSteps:Int
)