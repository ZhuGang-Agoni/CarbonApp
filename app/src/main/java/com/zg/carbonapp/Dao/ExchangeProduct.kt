package com.zg.carbonapp.Dao

import java.io.Serializable

// 实现Serializable方便Intent传递
data class ExchangeProduct(
    val id: Int,
    val name: String,
    val description: String,
    val points: Int, // 所需积分
    val imageRes: Int, // 商品图片资源ID
    val exchangeCount: Int = 0 // 已兑换数量（仅最新上架商品需要）
) : Serializable