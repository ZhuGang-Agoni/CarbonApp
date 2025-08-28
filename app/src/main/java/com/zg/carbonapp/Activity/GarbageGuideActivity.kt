package com.zg.carbonapp.Activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.zg.carbonapp.Dao.GarbageCategory
import com.zg.carbonapp.R
import com.zg.carbonapp.Adapter.GarbageGuideAdapter

class GarbageGuideActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var rvGarbageGuide: RecyclerView
    private lateinit var emptyView: TextView

    // 四大类垃圾数据（包含完整例子用于搜索，展示时只显示部分）
    private val garbageCategories = listOf(
        GarbageCategory(
            id = 1,
            name = "可回收物",
            icon = R.drawable.ic_recyclable,
            standard = "适宜回收利用和资源化利用的生活废弃物，包括废纸、塑料、玻璃、金属和布料等。",
            // 完整例子（用于搜索）
            fullExamples = listOf(
                "废纸类：报纸、期刊、图书、快递盒、纸箱、包装纸、办公用纸、A4纸、信封、广告单、旧日历、笔记本、宣传单、牛皮纸、纸筒等（注意：纸巾、厕所纸不可回收）。",
                "塑料类：矿泉水瓶、饮料瓶、食用油桶、洗洁精瓶、洗衣液瓶、塑料盆、塑料桶、塑料凳、塑料玩具、塑料衣架、塑料袋、塑料泡沫、塑料餐盒（干净）等。",
                "玻璃类：玻璃瓶（红酒瓶、酱油瓶）、玻璃杯、玻璃罐、玻璃窗、玻璃镜子、玻璃碎片、灯泡（白炽灯除外）、暖瓶胆等。",
                "金属类：易拉罐、铁皮罐头盒、铝箔纸、铝制餐具、铁锅、铁钉、铜导线、铝合金门窗、金属瓶盖、旧钥匙、不锈钢餐具等。",
                "布料类：旧衣服、旧裤子、旧鞋子、旧帽子、旧床单、旧被套、旧窗帘、毛绒玩具、布制书包、抹布（干净）等。",
                "电子类：旧手机、旧电脑、旧平板电脑、旧充电器、旧耳机、旧鼠标、旧键盘、旧打印机等。"
            ),
            // 展示用例子（只显示2条核心内容）
            displayExamples = listOf(
                "废纸类：报纸、期刊、图书、快递盒、纸箱（注意：纸巾、厕所纸不可回收）。",
                "塑料/金属类：矿泉水瓶、饮料瓶、易拉罐、铁皮罐头、塑料盆、铁锅等。"
            ),
            misunderstandings = listOf(
                "误区：所有塑料都可回收 → 纠正：污染严重的塑料袋、油渍餐盒不可回收。",
                "误区：旧电池都属于有害垃圾 → 纠正：锂电池、充电电池属于可回收物。"
            )
        ),
        GarbageCategory(
            id = 2,
            name = "有害垃圾",
            icon = R.drawable.ic_hazardous,
            standard = "含有害物质，需要特殊安全处理的垃圾，若随意丢弃会对环境和人体健康造成危害。",
            // 完整例子（用于搜索）
            fullExamples = listOf(
                "电池类：纽扣电池、镍镉电池、铅酸电池、蓄电池、废电池（含汞、镉）、电动车电池、工业用电池等。",
                "灯管类：日光灯管、节能灯管（LED灯管除外）、荧光灯管、霓虹灯、紫外线灯管、灭蚊灯灯管等。",
                "药品类：过期感冒药、消炎药、抗生素、疫苗、口服液、废弃农药、兽药、老鼠药、蟑螂药、除草剂、杀虫剂等。",
                "化学品类：废油漆、油漆稀释剂、废颜料、废油墨、染发剂、烫发剂、卸甲水、指甲油、强腐蚀性清洁剂、机油、汽油等。",
                "医疗器械类：医用针头、针管、污染的医用纱布、过期医用口罩、血压计、体温计（含汞）、X光片等。",
                "其他类：废胶片、废相纸、硒鼓、墨盒、废弃水银温度计、荧光棒、过期化妆品（含化学添加剂）等。"
            ),
            // 展示用例子（只显示2条核心内容）
            displayExamples = listOf(
                "电池/灯管类：纽扣电池、镍镉电池、蓄电池、日光灯管、节能灯管等。",
                "药品/化学品类：过期药品、废农药、废油漆、染发剂、卸甲水、指甲油等。"
            ),
            misunderstandings = listOf(
                "误区：普通干电池是有害垃圾 → 纠正：现在的干电池多为无汞电池，属于其他垃圾。",
                "误区：LED灯管属于有害垃圾 → 纠正：LED灯管不含汞，属于可回收物。"
            )
        ),
        GarbageCategory(
            id = 3,
            name = "厨余垃圾",
            icon = R.drawable.ic_kitchen,
            standard = "居民日常生活及食品加工、饮食服务、单位供餐等活动中产生的垃圾，包括丢弃不用的菜叶、剩菜、剩饭、果皮、蛋壳、茶渣、骨头等。",
            // 完整例子（用于搜索）
            fullExamples = listOf(
                "蔬菜类：白菜叶、青菜叶、菠菜叶、韭菜、芹菜、萝卜缨、菜根、菜帮、西兰花梗、胡萝卜皮、土豆皮、红薯皮、洋葱皮、大蒜皮等。",
                "主食类：米饭、面条、馒头、包子、饺子、馄饨、粥、年糕、粽子、汤圆、烧饼、面包、蛋糕、饼干（未受潮）、油条等。",
                "肉类：猪肉、牛肉、羊肉、鸡肉、鸭肉、鱼肉、动物内脏、鸡骨头、鸭骨头、鱼骨头、虾壳、蟹壳、鱼鳞、鸡皮、瘦肉等。",
                "水果类：苹果皮、香蕉皮、橘子皮、橙子皮、柚子皮、西瓜皮、哈密瓜皮、葡萄皮、草莓蒂、菠萝皮、芒果核、苹果核、桃核等。",
                "其他类：鸡蛋壳、鸭蛋壳、鹌鹑蛋壳、茶叶渣、咖啡渣、中药渣、豆浆渣、豆腐渣、椰子壳（切碎）、甘蔗渣、玉米须等。"
            ),
            // 展示用例子（只显示2条核心内容）
            displayExamples = listOf(
                "蔬菜/水果类：菜叶、菜根、菜帮、果皮、果核、苹果皮、香蕉皮、鸡蛋壳等。",
                "主食/肉类：米饭、面条、馒头、饺子、剩菜、剩饭、鸡骨头、鱼骨头、虾壳等。"
            ),
            misunderstandings = listOf(
                "误区：骨头都是厨余垃圾 → 纠正：大骨头（猪筒骨、牛骨）质地坚硬，属于其他垃圾。",
                "误区：粽子叶属于厨余垃圾 → 纠正：粽子叶纤维坚韧，不易腐烂，属于其他垃圾。"
            )
        ),
        GarbageCategory(
            id = 4,
            name = "其他垃圾",
            icon = R.drawable.ic_other,
            standard = "除可回收物、有害垃圾、厨余垃圾以外的其他生活废弃物，包括难以回收的废弃物、尘土、污染较严重的纸等。",
            // 完整例子（用于搜索）
            fullExamples = listOf(
                "卫生用品：纸巾、卫生纸、厨房纸、尿不湿、卫生巾、卫生护垫、湿厕纸、一次性洗脸巾、化妆棉、棉签、牙线、口罩（非医用）、创可贴（用过的）等。",
                "污染废弃物：油渍塑料袋、脏污保鲜膜、用过的一次性餐盒、污染的纸尿布、发霉的面包、变质的食物（无法分类的）、被油污污染的报纸等。",
                "陶瓷玻璃类：陶瓷碗、陶瓷杯、陶瓷碎片、花盆碎片、玻璃杯（破损严重）、镜子碎片（镀膜层破损）、白炽灯灯泡等。",
                "其他类：烟头、烟灰、尘土、渣土、大骨头（猪骨、牛骨）、榴莲壳、菠萝蜜壳、椰子壳（完整）、粽子叶、玉米棒（整根）、头发、动物毛发等。"
            ),
            // 展示用例子（只显示2条核心内容）
            displayExamples = listOf(
                "卫生用品类：纸巾、卫生纸、尿不湿、卫生巾、湿厕纸、一次性洗脸巾、棉签等。",
                "其他类：烟头、尘土、陶瓷碎片、大骨头、榴莲壳、头发、动物毛发等。"
            ),
            misunderstandings = listOf(
                "误区：陶瓷碎片可回收 → 纠正：陶瓷硬度高，回收成本高，属于其他垃圾。",
                "误区：烟蒂属于有害垃圾 → 纠正：烟蒂无污染性，属于其他垃圾。"
            )
        )
    )

    private var currentCategory: GarbageCategory = garbageCategories[0]
    private lateinit var guideAdapter: GarbageGuideAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_guide)

        // 绑定控件
        tabLayout = findViewById(R.id.tabLayout)
        etSearch = findViewById(R.id.et_search)
        ivClearSearch = findViewById(R.id.iv_clear_search)
        rvGarbageGuide = findViewById(R.id.rv_garbage_guide)
        emptyView = findViewById(R.id.empty_view)

        // 初始化TabLayout
        initTabLayout()

        // 初始化列表（展示精简例子）
        guideAdapter = GarbageGuideAdapter(currentCategory)
        rvGarbageGuide.layoutManager = LinearLayoutManager(this)
        rvGarbageGuide.adapter = guideAdapter

        // 初始化搜索功能（搜索完整例子）
        initSearch()
    }

    private fun initTabLayout() {
        garbageCategories.forEach { category ->
            tabLayout.addTab(tabLayout.newTab().setText(category.name))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val position = tab.position
                currentCategory = garbageCategories[position]
                guideAdapter.updateCategory(currentCategory) // 切换时显示精简例子
                rvGarbageGuide.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                etSearch.text.clear()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun initSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()
                if (keyword.isEmpty()) {
                    ivClearSearch.visibility = View.GONE
                    guideAdapter.updateCategory(currentCategory) // 显示精简例子
                    rvGarbageGuide.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                } else {
                    ivClearSearch.visibility = View.VISIBLE
                    // 搜索时使用完整例子匹配（确保能搜到所有数据）
                    val filteredExamples = currentCategory.fullExamples.filter {
                        it.contains(keyword, ignoreCase = true)
                    }
                    if (filteredExamples.isEmpty()) {
                        rvGarbageGuide.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "未找到包含「$keyword」的垃圾指南~"
                    } else {
                        // 搜索结果展示匹配到的完整例子
                        guideAdapter.updateExamples(filteredExamples)
                        rvGarbageGuide.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }
}