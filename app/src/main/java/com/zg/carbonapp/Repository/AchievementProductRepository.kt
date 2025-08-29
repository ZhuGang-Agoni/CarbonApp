package com.zg.carbonapp.Repository

import com.zg.carbonapp.Dao.ProductSource
import com.zg.carbonapp.Dao.ProductType
import com.zg.carbonapp.Dao.Rarity
import com.zg.carbonapp.Dao.TimelineNode
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.R

object AchievementProductRepository {


    val allAchievements: List<VirtualProduct> = listOf(
        // 成就奖励商品（source = ACHIEVEMENT）
        VirtualProduct(
            id = 1,
            name = "低碳萌芽勋章",
            description = "迈出低碳生活第一步，开启环保之旅",
            points = 0,
            type = ProductType.BADGE,
            iconRes = R.drawable.ic_badge_sprout_lock,
            unlockRes = R.drawable.ic_badge_sprout_unlock,
            rarity = Rarity.COMMON,
            source = ProductSource.task
        ),
       VirtualProduct(
                id = 2,
                name = "绿色行者勋章",
                description = "坚持步行减排，成为绿色生活榜样",
                points = 0,
                type = ProductType.BADGE,
                iconRes = R.drawable.ic_badge_walker_unlock,
                unlockRes = R.drawable.ic_badge_walker_unlock,
                rarity = Rarity.RARE,
                source = ProductSource.task

            ),
        VirtualProduct(
                id = 3,
                name = "生态守护勋章",
                description = "累计减排贡献卓越，守护生态平衡",
                points = 0,
                type = ProductType.BADGE,
                iconRes = R.drawable.ic_badge_guardian_unlock,
                unlockRes = R.drawable.ic_badge_guardian_lock,
                rarity = Rarity.EPIC,
                source = ProductSource.task

            ),
       VirtualProduct(
                id = 4,
                name = "叶脉环框",
                description = "树叶脉络纹理，自然气息十足",
                points = 300,
                type = ProductType.AVATAR_FRAME,
                iconRes = R.drawable.ic_frame_vein_unlock,
                unlockRes = R.drawable.ic_frame_vein_lock,
                rarity = Rarity.COMMON,
                source = ProductSource.task

            ),
       VirtualProduct(
                id = 5,
                name = "极光环框",
                description = "蓝紫极光环绕，酷炫动态效果",
                points = 800,
                type = ProductType.AVATAR_FRAME,
                iconRes = R.drawable.ic_frame_aurora_unlock,
                unlockRes = R.drawable.ic_frame_aurora_lock,
                rarity = Rarity.EPIC,
                source = ProductSource.task

            ),

        // 挂件类
        VirtualProduct(
                id = 6,
                name = "萌芽挂件",
                description = "小树苗造型，见证你的环保成长",
                points = 150,
                type = ProductType.AVATAR_ITEM,
                iconRes = R.drawable.ic_accessory_bud_unlock,
                unlockRes = R.drawable.ic_accessory_bud_lock,
                rarity = Rarity.COMMON,
                source = ProductSource.task

            ),

        VirtualProduct(
                id = 7,
                name = "风车挂件",
                description = "环保风车造型，象征清洁能源",
                points = 600,
                type = ProductType.AVATAR_ITEM,
                iconRes = R.drawable.ic_accessory_windmill_unlock,
                unlockRes = R.drawable.ic_accessory_windmill_lock,
                rarity = Rarity.LEGENDARY,
                source = ProductSource.task
    )
    )
}