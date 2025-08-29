package com.zg.carbonapp.Dao

import com.zg.carbonapp.R

data class Tree(
    val id: String = "", // 唯一标识
    val treeType: String = "普通松树", // 树种
    val plantTime: Long = System.currentTimeMillis(), // 种植时间（毫秒）
    var lastWaterTime: Long = 0, // 最后浇水时间（用于加速生长）
    val growthSpeed: Float = 1.0f // 生长速度（默认1.0，浇水后暂时加快）
)
enum class GrowthStage(
    val minDays: Int,
    val title: String,
    val description: String,
    val iconRes: Int // 关联新图标
) {
    SEED(0, "种子", "今天种下了一颗饱满的种子，期待它的成长～", R.drawable.stage_0),
    SPROUT(3, "发芽", "种子破土而出，冒出了嫩绿的小芽，充满生机！", R.drawable.stage_1),
    SEEDLING(7, "幼苗", "经过一周的生长，幼苗长出了几片新叶，随风摆动～", R.drawable.stage_2),
    MATURE(30, "成树", "树干逐渐粗壮，枝叶繁茂，已经是一棵健康的小树了！", R.drawable.stage_3),
    GIANT(90, "参天大树", "经历了三个月的成长，它已长成参天大树，绿意盎然！", R.drawable.stage_4);

    companion object {
        fun getStage(growthDays: Int): GrowthStage {
            return when {
                growthDays >= GIANT.minDays -> GIANT
                growthDays >= MATURE.minDays -> MATURE
                growthDays >= SEEDLING.minDays -> SEEDLING
                growthDays >= SPROUT.minDays -> SPROUT
                else -> SEED
            }
        }
    }
}