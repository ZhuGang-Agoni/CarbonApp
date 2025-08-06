package com.zg.carbonapp.Repository

import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.R
import kotlin.random.Random

object LowCarbonKnowledgeRepository {
    // 扩展后的低碳知识数据集合
    private val allKnowledgeList = listOf(
        LowCarbonKnowledge(
            "5个简单的家庭节能小窍门",
            "1. 随手关灯，避免长明灯；\n2. 使用节能灯泡，降低耗电量；\n3. 合理设置空调温度（夏季不低于26℃，冬季不高于20℃）；\n4. 减少待机能耗，长期不用的电器拔掉插头；\n5. 用洗菜水浇花、冲厕所，实现水资源二次利用。",
            R.drawable.family_energy
        ),
        LowCarbonKnowledge(
            "绿色出行的选择与环保效益",
            "选择步行、骑行或公共交通出行，能减少汽车尾气排放。\n公交车人均碳排放仅为私家车的1/8；\n地铁能耗是公交车的1/3；\n骑行和步行几乎零排放，还能锻炼身体。",
            R.drawable.green_go_navigation
        ),
        LowCarbonKnowledge(
            "如何正确进行垃圾分类",
            "可回收物（纸、塑料、玻璃、金属、布料）单独收集；\n有害垃圾（电池、药品、化妆品、油漆桶）密封投放；\n厨余垃圾沥干水分后用可降解袋包装投放；\n其他垃圾（烟头、卫生纸、塑料袋）归入相应容器。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "节能家电的选购与使用",
            "选购一级能效的冰箱、空调、洗衣机等；\n冰箱避免频繁开关门，存放物品不超过80%容量；\n空调定期清洗滤网，提高散热效率；\n洗衣机选择强档洗涤，减少洗涤时间。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳饮食的小妙招",
            "减少肉类消费（尤其是牛肉，生产1kg牛肉排放39.2kg二氧化碳）；\n多吃本地应季蔬果，减少运输能耗；\n避免食物浪费，践行“光盘行动”；\n使用可重复餐具容器，减少一次性餐具使用。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "家庭节水实用技巧",
            "安装节水龙头和淋浴头，节水率可达30%；\n盆浴改淋浴，淋浴时间控制在5分钟内；\n洗衣机满负荷再洗，节水又省电；\n收集雨水用于浇花、拖地。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳办公小习惯",
            "打印纸双面使用，设置默认双面打印；\n下班前关闭电脑、打印机等设备电源；\n使用可重复使用的水杯，减少一次性杯子；\n通过邮件沟通，减少纸质文件传递。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "衣物护理的低碳方式",
            "衣物自然晾干，减少烘干机使用；\n集中洗涤衣物，避免小批量多次洗涤；\n选择冷水洗涤，可节约90%的能源；\n减少化学洗涤剂使用，选择环保型替代品。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "绿色购物指南",
            "自带购物袋，拒绝一次性塑料袋；\n购买简包装商品，减少包装废弃物；\n优先选择本地、应季产品，减少运输碳排放；\n按需购买，避免冲动消费和闲置浪费。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "家庭垃圾分类进阶技巧",
            "废纸应抖落干净、铺平叠好；\n塑料瓶洗净晾干、拧盖分类；\n玻璃瓶去除标签、冲洗干净；\n旧衣物洗净叠好，可捐赠或回收。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳装修注意事项",
            "选择环保建材，减少甲醛等有害物质；\n合理设计采光，减少照明能耗；\n使用保温材料，提高室内温度稳定性；\n选用节水型卫浴设备，降低水资源消耗。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "电子产品的低碳使用",
            "手机、电脑等设备充满电后及时拔掉充电器；\n降低屏幕亮度，缩短自动锁屏时间；\n旧设备可维修后继续使用或正规回收；\n开启设备节能模式，延长续航同时减少能耗。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "节气养生与低碳生活",
            "夏季自然通风降温，减少空调使用；\n冬季通过增添衣物保暖，调低空调温度；\n饮用温水，减少饮水机加热能耗；\n顺应季节饮食，减少反季节食材消费。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "绿色种植与低碳生活",
            "在家中种植绿植，净化空气同时降温；\n利用阳台种植蔬菜，体验低碳食材；\n使用厨余垃圾堆肥，减少垃圾排放；\n选择本地植物品种，降低养护能耗。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "减少一次性用品的方法",
            "用可重复使用的餐具替代一次性餐具；\n用布手帕替代纸巾；\n用充电电池替代一次性电池；\n用玻璃罐替代保鲜膜和保鲜袋。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳旅行小贴士",
            "选择火车、长途汽车等低碳交通方式；\n住宿选择绿色环保酒店；\n自带洗漱用品，拒绝一次性用品；\n尊重当地生态，不随意丢弃垃圾。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "儿童低碳教育小方法",
            "通过游戏教会孩子垃圾分类；\n带孩子参与植树等环保活动；\n和孩子一起制作废旧物品手工；\n培养孩子随手关灯、节约用水的习惯。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "厨房低碳小技巧",
            "合理使用抽油烟机，做完饭后再开5分钟即可关闭；\n使用高压锅烹饪，可节约30%以上的能源；\n食材提前解冻，减少烹饪时间；\n及时清理冰箱，保持制冷效率。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "冬季取暖节能方法",
            "保持室内温度在18-20℃，每降低1℃可节能10%；\n使用厚窗帘减少热量流失；\n密封门窗缝隙，防止冷风进入；\n采用电暖器局部取暖，替代全屋供暖。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "夏季降温节能方法",
            "使用遮阳帘减少太阳直射，可使室内温度降低3-5℃；\n夜间开窗通风，白天关闭门窗保持凉爽；\n风扇与空调配合使用，可提高制冷效率；\n定期清洗空调滤网，提高散热效果。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "纸张节约小窍门",
            "文件编辑尽量在电脑上完成，减少打印；\n打印错误的纸张可翻面作为草稿纸；\n订阅电子刊物，减少纸质报刊；\n使用再生纸制品，支持循环经济。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳节日庆祝方式",
            "减少烟花爆竹燃放，选择电子鞭炮；\n节日装饰选用可重复使用的材料；\n赠送礼物选择实用物品，减少包装；\n家庭聚会按需准备食物，避免浪费。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "废旧电池的正确处理",
            "碱性电池（普通干电池）可随生活垃圾处理；\n充电电池、纽扣电池属于有害垃圾，需单独投放；\n手机电池、笔记本电池可交由专业回收点处理；\n避免电池挤压、高温和拆解，防止污染。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "水资源循环利用技巧",
            "洗菜水沉淀后可用于浇花；\n洗衣水可用于拖地、冲厕所；\n收集空调冷凝水用于清洁；\n鱼缸换水可用于浇灌植物（富含养分）。",
            R.drawable.go
        ),
        LowCarbonKnowledge(
            "低碳生活与健康的关系",
            "步行、骑行等低碳出行方式有助于锻炼身体；\n减少加工食品消费，有益身体健康；\n室内绿植增多，改善空气质量，有益呼吸系统；\n节约用电减少电磁辐射，改善睡眠质量。",
            R.drawable.go
        )
    )

    private val usedIndices = mutableSetOf<Int>()

    fun getRandomLowCarbonKnowledge(): List<LowCarbonKnowledge> {
        // 重置机制：当所有数据都展示过后清空记录
        if (usedIndices.size == allKnowledgeList.size) {
            usedIndices.clear()
        }

        // 获取未使用的随机索引
        val availableIndices = allKnowledgeList.indices.filter { it !in usedIndices }

        // 如果剩余数据不足5条，直接返回全部未使用的
        if (availableIndices.size <= 5) {
            usedIndices.addAll(availableIndices)
            return availableIndices.map { allKnowledgeList[it] }
        }

        // 随机选取5个未使用的索引
        val selectedIndices = availableIndices.shuffled().take(5).toMutableSet()
        usedIndices.addAll(selectedIndices)

        return selectedIndices.map { allKnowledgeList[it] }
    }
}
