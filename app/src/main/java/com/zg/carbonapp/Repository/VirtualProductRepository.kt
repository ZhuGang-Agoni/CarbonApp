package com.zg.carbonapp.Repository

import com.zg.carbonapp.Dao.ProductSource
import com.zg.carbonapp.Dao.ProductType
import com.zg.carbonapp.Dao.Rarity
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.R

object VirtualProductRepository {

    // 所有虚拟商品 (使用实际设计的drawable资源)
    val allProducts = listOf(
        // 勋章类
        VirtualProduct(
            id = 8,
            name = "森林守护者",
            description = "累计减排超过2000kg解锁",
            points = 0,
            type = ProductType.BADGE,
            // 替换为实际勋章的静态图标资源
            iconRes = R.drawable.ic_badge_forest_static,
            // 替换为解锁后展示的资源（若有，也可用静态图标）
            unlockRes = R.drawable.ic_badge_forest_rare,
            rarity = Rarity.EPIC,
            source = ProductSource.mall
        ),
        VirtualProduct(
            id = 9,
            name = "减碳先锋",
            description = "连续30天完成减排任务",
            points = 500,
            type = ProductType.BADGE,
            // 替换为减碳先锋勋章静态图标
            iconRes = R.drawable.ic_badge_pioneer_common,
            unlockRes = R.drawable.ic_badge_pioneer_common,
            rarity = Rarity.RARE,
            source = ProductSource.mall
         ),

        // 头像框类
        VirtualProduct(
            id = 10,
            name = "绿叶边框",
            description = "环保认证用户专属",
            points = 300,
            type = ProductType.AVATAR_FRAME,
            // 绿叶边框静态图标
            iconRes = R.drawable.ic_frame_leaf_common,
            unlockRes = R.drawable.ic_frame_leaf_common,
            rarity = Rarity.COMMON,
            source = ProductSource.mall

        ),
        VirtualProduct(
            id = 11,
            name = "极光特效框",
            description = "稀有动态特效",
            points = 800,
            type = ProductType.AVATAR_FRAME,
            // 极光特效框静态图标
            iconRes = R.drawable.ic_frame_aurora_static,
            unlockRes = R.drawable.ic_frame_aurora_static, // 解锁后展示动态或更炫酷资源
            rarity = Rarity.EPIC,
            // 若有特效资源，这里赋值，根据实际需求决定是否使用
            effectRes = R.drawable.ic_frame_aurora_static,
            source = ProductSource.mall

        ),

        // 头像挂件类
        VirtualProduct(
            id = 12,
            name = "小树苗挂件",
            description = "每兑换一棵树获得",
            points = 150,
            type = ProductType.AVATAR_ITEM,
            // 小树苗挂件静态图标
            iconRes = R.drawable.ic_accessory_seedling_common,
            unlockRes = R.drawable.ic_accessory_seedling_common,
            rarity = Rarity.COMMON,
            source = ProductSource.mall

        ),
        VirtualProduct(
            id = 13,
            name = "太阳能卫星",
            description = "高科技环保装备",
            points = 600,
            type = ProductType.AVATAR_ITEM,
            // 太阳能卫星挂件静态图标
            iconRes = R.drawable.ic_accessory_satellite_legend,
            unlockRes = R.drawable.ic_accessory_satellite_legend,
            rarity = Rarity.RARE,
            source = ProductSource.mall
        )
    )

    // 按类型筛选
    fun getProductsByType(type: ProductType) = allProducts.filter { it.type == type }

    // 热门推荐（按稀有度排序，ordinal值越大稀有度越高，这里EPIC > RARE > COMMON  ）
    val hotProducts = allProducts.sortedByDescending { it.rarity.ordinal }.take(4)

    // 最新上架（这里简单取最后3个，实际可根据上架时间等逻辑调整）
    val newProducts = allProducts.takeLast(3)

}