package com.zg.carbonapp.Repository

import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.R
import kotlin.random.Random

object LowCarbonKnowledgeRepository {


    // 在您的Activity或Fragment中

    fun getLottieList():List<LowCarbonKnowledge>{
        return knowledgeList.shuffled().take(3)
    }
   private val knowledgeList = listOf(

           LowCarbonKnowledge(
               title = "减少一次性用品的方法",
               content = "1. 使用可重复使用的购物袋替代塑料袋\n2. 携带自己的水杯替代一次性塑料杯\n3. 使用可重复使用的餐具替代一次性餐具\n4. 选择可重复使用的咖啡胶囊\n5. 使用硅胶保鲜盖替代保鲜膜",
               lottieResId = R.raw.recycle
           ),
    LowCarbonKnowledge(
    title = "冬季取暖节能方法",
    content = "1. 合理设置室内温度（18-20℃）\n2. 使用智能温控器自动调节温度\n3. 确保门窗密封良好减少热量流失\n4. 使用厚窗帘保暖\n5. 定期清洁暖气片提高效率",
    lottieResId = R.raw.energy_saving
    ),
    LowCarbonKnowledge(
    title = "垃圾分类指南",
    content = "1. 可回收物：纸张、塑料、玻璃、金属\n2. 厨余垃圾：食物残渣、果皮\n3. 有害垃圾：电池、药品、化学品\n4. 其他垃圾：无法归类的废弃物\n5. 大件垃圾：家具、家电需单独处理",
    lottieResId = R.raw.garbage_sort
    ),
    LowCarbonKnowledge(
    title = "节水技巧",
    content = "1. 安装节水型淋浴喷头\n2. 收集雨水用于浇花\n3. 洗菜水用于冲厕所\n4. 刷牙时关闭水龙头\n5. 使用洗碗机代替手洗（满负荷时更节水）",
    lottieResId = R.raw.water
    ),
    LowCarbonKnowledge(
    title = "绿色出行方式",
    content = "1. 步行或骑行短途出行\n2. 使用公共交通工具\n3. 拼车或使用共享汽车服务\n4. 选择电动汽车或混合动力车\n5. 规划高效路线减少行驶里程",
    lottieResId = R.raw.green_bike
    ),
    LowCarbonKnowledge(
    title = "纸张节约与回收",
    content = "1. 使用电子文档替代纸质文件\n2. 双面使用打印纸\n3. 设立废纸回收箱\n4. 使用再生纸产品\n5. 减少不必要的外包装",
    lottieResId = R.raw.save_paper
    ),
    LowCarbonKnowledge(
    title = "绿色园艺技巧",
    content = "1. 种植本地耐旱植物\n2. 使用有机肥料\n3. 收集雨水灌溉\n4. 自制堆肥处理厨余垃圾\n5. 避免使用化学农药",
    lottieResId = R.raw.gardening
    ),
       LowCarbonKnowledge(
           title = "节能灯泡使用指南",
           content = "1. 选择LED灯泡替代传统白炽灯\n2. 合理控制照明时间\n3. 使用智能感应开关\n4. 定期清洁灯罩提高亮度\n5. 购买高能效等级产品",
           lottieResId = R.raw.light// 需自行下载或制作
       ),
       LowCarbonKnowledge(
           title = "太阳能利用技巧",
           content = "1. 安装太阳能热水器\n2. 使用太阳能充电板\n3. 选择太阳能路灯\n4. 太阳能烤箱使用指南\n5. 太阳能水泵应用场景",
           lottieResId = R.raw.solar // 推荐搜索关键词：solar energy
       )
    )

}
