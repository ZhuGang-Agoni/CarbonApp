package com.zg.carbonapp.Repository

import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.R


object LowCarbonKnowledgeRepository {
    // 模拟真实的低碳知识数据
    fun getLowCarbonKnowledgeList(): List<LowCarbonKnowledge> {
        return listOf(
            LowCarbonKnowledge(
                "5个简单的家庭节能小窍门",
                "1. 随手关灯，避免长明灯；\n2. 使用节能灯泡，降低耗电量；\n3. 合理设置空调温度；\n4. 减少待机能耗；\n5. 用洗菜水浇花、冲厕所。",
                R.drawable.go
            ),
            LowCarbonKnowledge(
                "绿色出行的选择与环保效益",
                "选择步行、骑行或公共交通出行，能减少汽车尾气排放。\n公交车人均碳排放仅为私家车的1/8；\n骑行和步行几乎零排放，还能锻炼身体。",
                R.drawable.go
            ),
            LowCarbonKnowledge(
                "如何正确进行垃圾分类",
                "可回收物（纸、塑料、玻璃等）单独收集；\n有害垃圾（电池、药品等）密封投放；\n厨余垃圾沥干后投放；\n其他垃圾归入相应容器。",
                R.drawable.go
            ),
            LowCarbonKnowledge(
                "节能家电的选购与使用",
                "选购一级能效的冰箱、空调等；\n使用时，冰箱避免频繁开关门，空调定期清洗滤网。",
                R.drawable.go
            ),
            LowCarbonKnowledge(
                "低碳饮食的小妙招",
                "减少肉类消费（尤其是牛肉）；\n多吃本地应季蔬果；\n避免食物浪费，践行“光盘行动”。",
                R.drawable.go
            )
        )
    }
}