package com.zg.carbonapp.Tool

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.zg.carbonapp.Dao.TravelOption
import com.zg.carbonapp.Fragment.BarChartFragment
import com.zg.carbonapp.Fragment.RadarChartFragment
import com.zg.carbonapp.Fragment.RecommendationFragment
import com.zg.carbonapp.R
import com.zg.carbonapp.logic.model.Weather
import kotlin.math.roundToInt

class TravelRecommendationDialog(
    private val weather: Weather,
    private val distance: Float, // 距离（公里）
    private val isNoon: Boolean,
    private val isSameCity: Boolean,  // 是否同城
    private val hasSubway: Boolean,    // 是否有地铁
    private val onConfirm: () -> Unit
) : DialogFragment() {

    private lateinit var options: List<TravelOption>
    private lateinit var bestOption: TravelOption

    // 出行方式距离阈值（公里）
    private val MAX_WALK_DISTANCE = 10f    // 步行最大距离
    private val MAX_BIKE_DISTANCE = 30f    // 骑行最大距离
    private val MIN_DRIVE_DISTANCE = 5f    // 自驾最小距离
    private val MIN_PUBLIC_DISTANCE = 1f   // 公共交通最小距离
    private val MAX_LOCAL_PUBLIC = 50f     // 市内公交/地铁最大距离
    private val MIN_HSR_DISTANCE = 50f     // 高铁最小距离
    private val MAX_HSR_DISTANCE = 1000f   // 高铁最大距离
    private val MIN_TRAIN_DISTANCE = 30f   // 普通火车最小距离

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_travel_suggestion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTravelOptions()
        initViewPager(view)

        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            dismiss()
            onConfirm()
        }
    }

    /**
     * 初始化所有出行选项
     */
    private fun initTravelOptions() {
        val realtime = weather.realtime
        val isRaining = realtime.skycon.contains("RAIN", true)
        val temp = realtime.temperature
        val aqi = realtime.airQuality.aqi.chn

        val optionList = mutableListOf<TravelOption>()

        // 1. 步行
        optionList.add(
            TravelOption(
                type = "步行",
                iconRes = R.drawable.walk,
                weatherSuitability = calculateWalkScore(isRaining, temp, aqi.roundToInt(), distance, isNoon),
                carbonFootprint = 0f,
                healthBenefit = 5f,
                timeEfficiency = calculateWalkTimeEfficiency(distance),
                color = ContextCompat.getColor(requireContext(), R.color.green),
                recommendation = generateWalkRecommendation(isRaining, temp, distance)
            )
        )

        // 2. 骑行
        optionList.add(
            TravelOption(
                type = "骑行",
                iconRes = R.drawable.bike,
                weatherSuitability = calculateBikeScore(isRaining, temp, aqi.roundToInt(), distance, isNoon),
                carbonFootprint = 0f,
                healthBenefit = 4.5f,
                timeEfficiency = calculateBikeTimeEfficiency(distance),
                color = ContextCompat.getColor(requireContext(), R.color.yellow),
                recommendation = generateBikeRecommendation(isRaining, temp, distance)
            )
        )

        // 3. 自驾
        optionList.add(
            TravelOption(
                type = "自驾",
                iconRes = R.drawable.drive_eta,
                weatherSuitability = calculateCarScore(isRaining, temp, aqi.roundToInt(), distance),
                carbonFootprint = calculateDriveCarbon(distance),
                healthBenefit = 0.5f,
                timeEfficiency = calculateCarTimeEfficiency(distance),
                color = ContextCompat.getColor(requireContext(), R.color.red),
                recommendation = generateCarRecommendation(aqi.roundToInt(), distance)
            )
        )

        // 4. 公共交通（根据同城/跨城动态命名）
        val publicType = getSuitablePublicType(distance)
        optionList.add(
            TravelOption(
                type = if (isSameCity) "公共交通" else "跨城公交",  // 修复命名问题
                iconRes = getPublicTransitIcon(publicType),
                weatherSuitability = calculatePublicTransitScore(isRaining, temp, aqi.roundToInt(), publicType),
                carbonFootprint = calculatePublicCarbon(distance, publicType),
                healthBenefit = 1.5f,
                timeEfficiency = calculatePublicTimeEfficiency(distance, publicType),
                color = ContextCompat.getColor(requireContext(), R.color.purple),
                recommendation = generatePublicRecommendation(aqi.roundToInt(), distance, publicType)
            )
        )

        // 计算最佳选项
        bestOption = optionList.maxByOrNull {
            it.weatherSuitability * 0.3f +
                    it.timeEfficiency * 0.3f +
                    it.healthBenefit * 0.2f +
                    (5 - it.carbonFootprint / 100) * 0.2f
        } ?: optionList.firstOrNull() ?: TravelOption.empty()
        options = optionList
    }

    /**
     * 修复：优化公共交通类型判断逻辑，同城有地铁时优先推荐
     */
    private fun getSuitablePublicType(distance: Float): String {
        if (isSameCity) {
            // 同城逻辑：有地铁则优先推荐地铁（放宽距离限制到5公里）
            if (hasSubway) {
                return "地铁"
            } else {
                return "公交"
            }
        }
        // 跨城逻辑
        return when {
            distance >= MIN_HSR_DISTANCE && distance <= MAX_HSR_DISTANCE -> "高铁"
            distance >= MIN_TRAIN_DISTANCE -> "火车"
            else -> "公交"
        }
    }

    private fun getPublicTransitIcon(publicType: String): Int {
        return when (publicType) {
            "高铁" -> R.drawable.hsr
            "火车" -> R.drawable.train
            "地铁" -> R.drawable.subway
            else -> R.drawable.bus
        }
    }

    /**
     * 初始化ViewPager
     */
    private fun initViewPager(view: View) {
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        val fragments = listOf(
            RadarChartFragment.newInstance(options, bestOption, distance),
            BarChartFragment.newInstance(options, distance),
            RecommendationFragment.newInstance(options, distance)
        )

        val adapter = ViewPagerAdapter(childFragmentManager, lifecycle, fragments)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "综合评估"
                1 -> "碳排放"
                else -> "详细建议"
            }
        }.attach()
    }

    // ==================== 碳排放计算（修复对比逻辑） ====================
    // 自驾：燃油车0.17kg/km，电车0.03kg/km（按1人计算）
    private fun calculateDriveCarbon(distance: Float) = if (isFuelCar()) distance * 0.17f else distance * 0.03f

    // 公共交通碳排放（人均）
    private fun calculatePublicCarbon(distance: Float, publicType: String): Float {
        return when (publicType) {
            "高铁" -> distance * 0.03f    // 高铁人均碳排放
            "火车" -> distance * 0.04f    // 普通火车人均碳排放
            "地铁" -> distance * 0.02f    // 地铁人均碳排放
            else -> distance * 0.05f      // 公交人均碳排放
        }
    }

    private fun isFuelCar() = true

    // ==================== 天气适配评分 ====================
    private fun calculateWalkScore(
        isRaining: Boolean,
        temp: Float,
        aqi: Int,
        distance: Float,
        isNoon: Boolean
    ): Float {
        var score = 4f

        // 距离惩罚
        when {
            distance > 20f -> score = 0.5f  // 超过20公里完全不推荐
            distance > 10f -> score = 1.0f  // 10-20公里极不推荐
            distance > 5f -> score -= 1.5f
            distance > 3f -> score -= 0.8f
        }

        // 天气惩罚
        if (isRaining) score -= 2f
        when {
            temp < -5 -> score -= 2.5f
            temp > 35 -> score -= 2.5f
            temp > 30 -> score -= 1.5f
        }

        // 空气质量惩罚
        when (aqi) {
            in 151..200 -> score -= 0.8f
            in 201..300 -> score -= 1.8f
            in 301..500 -> score -= 2.8f
        }

        // 正午高温惩罚
        if (isNoon && temp > 28) score -= 1.2f

        return score.coerceIn(0f, 5f)
    }

    private fun calculateBikeScore(
        isRaining: Boolean,
        temp: Float,
        aqi: Int,
        distance: Float,
        isNoon: Boolean
    ): Float {
        var score = 3.8f

        // 距离惩罚
        when {
            distance > 100f -> score = 0.5f  // 超过100公里完全不推荐
            distance > 50f -> score = 1.0f    // 50-100公里极不推荐
            distance > 30f -> score = 1.5f    // 30-50公里不推荐
            distance > 15f -> score -= 2.5f
            distance > 10f -> score -= 1.5f
        }

        // 天气惩罚（骑行更怕雨）
        if (isRaining) score -= 2.5f
        when {
            temp < -5 -> score -= 2.2f
            temp > 35 -> score -= 2.2f
            temp > 30 -> score -= 1.2f
        }

        // 空气质量惩罚
        when (aqi) {
            in 151..200 -> score -= 0.8f
            in 201..300 -> score -= 1.8f
            in 301..500 -> score -= 2.8f
        }

        // 正午惩罚
        if (isNoon && temp > 28) score -= 1.0f

        return score.coerceIn(0f, 5f)
    }

    private fun calculateCarScore(
        isRaining: Boolean,
        temp: Float,
        aqi: Int,
        distance: Float
    ): Float {
        var score = 3.5f

        // 距离奖励
        when {
            distance > 200f -> score += 1.5f  // 超长距离适合自驾
            distance > 100f -> score += 1.2f
            distance > 50f -> score += 0.7f
        }

        // 近距离惩罚
        if (distance < 5f) score -= 1.8f

        // 雨天奖励（自驾不受影响）
        if (isRaining) score += 0.8f

        // 极端温度惩罚
        when {
            temp < -15 -> score -= 1.0f
            temp > 42 -> score -= 0.8f
        }

        // 空气质量惩罚
        if (aqi > 200) score -= 1.0f

        return score.coerceIn(0f, 5f)
    }

    private fun calculatePublicTransitScore(
        isRaining: Boolean,
        temp: Float,
        aqi: Int,
        publicType: String
    ): Float {
        var score = 4.2f

        // 室外停留比例：公交 > 地铁 > 火车 > 高铁
        val outdoorFactor = when (publicType) {
            "公交" -> 1.0f
            "地铁" -> 0.3f
            "火车" -> 0.2f
            else -> 0.1f // 高铁
        }

        // 雨天惩罚（按室外比例）
        if (isRaining) score -= 1.5f * outdoorFactor

        // 极端温度惩罚
        when {
            temp < -15 -> score -= 0.8f * outdoorFactor
            temp > 42 -> score -= 0.7f * outdoorFactor
        }

        // 空气质量惩罚
        when (aqi) {
            in 201..300 -> score -= 0.8f
            in 301..500 -> score -= 1.5f
        }

        return score.coerceIn(0f, 5f)
    }

    // ==================== 时间效率计算（修复自驾速度一致性） ====================
    private fun calculateWalkTimeEfficiency(distance: Float): Float {
        val timeHours = distance / 5f // 步行速度5km/h
        return when {
            timeHours > 48 -> 0.2f   // 超过2天
            timeHours > 24 -> 0.5f   // 超过1天
            timeHours > 12 -> 1.0f    // 超过12小时
            timeHours > 6 -> 1.5f     // 超过6小时
            timeHours > 3 -> 2.0f     // 超过3小时
            timeHours > 1.5 -> 2.5f   // 超过1.5小时
            timeHours > 1 -> 3.0f     // 超过1小时
            else -> 4.5f
        }
    }

    private fun calculateBikeTimeEfficiency(distance: Float): Float {
        val timeHours = distance / 15f // 骑行速度15km/h
        return when {
            timeHours > 48 -> 0.2f   // 超过2天
            timeHours > 24 -> 0.5f   // 超过1天
            timeHours > 12 -> 1.0f    // 超过12小时
            timeHours > 6 -> 1.8f     // 超过6小时
            timeHours > 3 -> 2.5f     // 超过3小时
            timeHours > 1 -> 3.5f     // 超过1小时
            timeHours > 0.5 -> 4.0f   // 超过0.5小时
            else -> 4.5f
        }
    }

    // 修复：统一自驾速度为80km/h，与推荐语中的计算一致
    private fun calculateCarTimeEfficiency(distance: Float): Float {
        val timeHours = distance / 80f // 统一为高速80km/h
        return when {
            timeHours > 24 -> 2.0f    // 超过1天
            timeHours > 12 -> 3.0f     // 超过12小时
            timeHours > 6 -> 3.8f      // 超过6小时
            timeHours > 3 -> 4.2f      // 超过3小时
            timeHours > 1.5 -> 4.5f    // 超过1.5小时
            timeHours > 0.5 -> 4.8f    // 超过0.5小时
            else -> 5.0f
        }
    }

    private fun calculatePublicTimeEfficiency(distance: Float, publicType: String): Float {
        val speed = when (publicType) {
            "高铁" -> 300f    // 高铁速度300km/h
            "火车" -> 120f    // 普通火车120km/h
            "地铁" -> 40f     // 地铁40km/h
            else -> 25f       // 公交25km/h
        }
        val timeHours = distance / speed
        return when {
            timeHours > 24 -> 1.0f    // 超过1天
            timeHours > 12 -> 2.0f     // 超过12小时
            timeHours > 6 -> 3.5f      // 超过6小时
            timeHours > 3 -> 4.0f      // 超过3小时
            timeHours > 1.5 -> 4.3f    // 超过1.5小时
            timeHours > 0.5 -> 4.5f    // 超过0.5小时
            else -> 4.8f
        }
    }

    // ==================== 推荐语生成（修复碳排放对比） ====================
    private fun generateWalkRecommendation(isRaining: Boolean, temp: Float, distance: Float): String {
        val timeHours = distance / 5f
        val days = (timeHours / 24).toInt()
        val hours = (timeHours % 24).toInt()
        val minutes = ((timeHours * 60) % 60).toInt()

        val sb = StringBuilder("步行是零碳排放的绿色出行方式，适合短途出行。\n\n")
        sb.append("📏 距离：${"%.1f".format(distance)}公里\n")
        sb.append("⏱️ 预计时间：")

        when {
            days > 0 -> sb.append("${days}天${hours}小时${minutes}分钟")
            hours > 0 -> sb.append("${hours}小时${minutes}分钟")
            else -> sb.append("${minutes}分钟")
        }
        sb.append("\n\n")

        // 详细分析
        sb.append("🔍 详细分析：\n")
        when {
            distance > 100f ->
                sb.append("❌ 距离过远：步行${"%.1f".format(distance)}公里完全不现实\n" +
                        "    - 相当于从北京到天津的距离\n" +
                        "    - 建议选择高铁或飞机等交通工具")
            distance > 50f ->
                sb.append("⚠️ 距离过长：步行${"%.1f".format(distance)}公里极不推荐\n" +
                        "    - 相当于绕标准跑道125圈\n" +
                        "    - 需要连续步行超过24小时")
            distance > 20f ->
                sb.append("⚠️ 距离较远：步行${"%.1f".format(distance)}公里不推荐\n" +
                        "    - 需要步行${(distance/5).toInt()}小时以上\n" +
                        "    - 建议分多天完成或选择其他交通方式")
            distance > 10f ->
                sb.append("⚠️ 距离较远：步行${"%.1f".format(distance)}公里需谨慎\n" +
                        "    - 需要步行${(distance/5).toInt()}小时左右\n" +
                        "    - 建议携带足够水和食物")
            distance > 5f ->
                sb.append("⏱️ 距离适中：步行${"%.1f".format(distance)}公里可行\n" +
                        "    - 需要步行${(distance/5*60).toInt()}分钟\n" +
                        "    - 建议穿舒适鞋子")
            distance > 2f ->
                sb.append("✅ 距离合适：步行${"%.1f".format(distance)}公里非常适合\n" +
                        "    - 需要步行${(distance/5*60).toInt()}分钟\n" +
                        "    - 建议作为日常锻炼")
            else ->
                sb.append("✅ 距离完美：步行${"%.1f".format(distance)}公里理想选择\n" +
                        "    - 仅需${(distance/5*60).toInt()}分钟\n" +
                        "    - 最环保的出行方式")
        }

        // 天气建议
        sb.append("\n\n🌦️ 天气建议：\n")
        if (isRaining) sb.append("    - 雨天建议穿防滑鞋和雨衣\n")
        if (temp > 30) sb.append("    - 高温天气请携带饮用水和防晒用品\n")
        if (temp < 5) sb.append("    - 低温天气请注意保暖，穿戴防风衣物\n")

        // 环保效益
        sb.append("\n♻️ 环保效益：\n")
        sb.append("    - 零碳排放，最环保的出行方式\n")
        sb.append("    - 每公里可减少约0.17kg碳排放\n")
        sb.append("    - 强身健体，有益健康")

        return sb.toString()
    }

    private fun generateBikeRecommendation(isRaining: Boolean, temp: Float, distance: Float): String {
        val timeHours = distance / 15f
        val days = (timeHours / 24).toInt()
        val hours = (timeHours % 24).toInt()
        val minutes = ((timeHours * 60) % 60).toInt()

        val sb = StringBuilder("骑行是高效低碳的出行方式，兼顾环保与健康。\n\n")
        sb.append("📏 距离：${"%.1f".format(distance)}公里\n")
        sb.append("⏱️ 预计时间：")

        when {
            days > 0 -> sb.append("${days}天${hours}小时${minutes}分钟")
            hours > 0 -> sb.append("${hours}小时${minutes}分钟")
            else -> sb.append("${minutes}分钟")
        }
        sb.append("\n\n")

        // 详细分析
        sb.append("🔍 详细分析：\n")
        when {
            distance > 200f ->
                sb.append("❌ 距离过远：骑行${"%.1f".format(distance)}公里完全不现实\n" +
                        "    - 相当于从北京到石家庄的距离\n" +
                        "    - 专业自行车运动员也需要2天以上")
            distance > 100f ->
                sb.append("⚠️ 距离过长：骑行${"%.1f".format(distance)}公里极不推荐\n" +
                        "    - 需要连续骑行超过8小时\n" +
                        "    - 建议选择火车或长途汽车")
            distance > 50f ->
                sb.append("⚠️ 距离较远：骑行${"%.1f".format(distance)}公里需谨慎\n" +
                        "    - 需要骑行${(distance/15).toInt()}小时以上\n" +
                        "    - 建议专业骑行爱好者分两天完成")
            distance > 30f ->
                sb.append("⏱️ 距离适中：骑行${"%.1f".format(distance)}公里可行\n" +
                        "    - 需要骑行${(distance/15).toInt()}小时左右\n" +
                        "    - 建议携带修车工具和补给")
            distance > 15f ->
                sb.append("✅ 距离合适：骑行${"%.1f".format(distance)}公里推荐\n" +
                        "    - 需要骑行${(distance/15*60).toInt()}分钟\n" +
                        "    - 中等强度锻炼")
            distance > 5f ->
                sb.append("✅ 距离理想：骑行${"%.1f".format(distance)}公里非常适合\n" +
                        "    - 需要骑行${(distance/15*60).toInt()}分钟\n" +
                        "    - 日常通勤好选择")
            else ->
                sb.append("✅ 距离完美：骑行${"%.1f".format(distance)}公里最佳选择\n" +
                        "    - 仅需${(distance/15*60).toInt()}分钟\n" +
                        "    - 高效环保的短途出行")
        }

        // 天气建议
        sb.append("\n\n🌦️ 天气建议：\n")
        if (isRaining)
            sb.append("    - 雨天骑行危险，建议穿戴专业防水装备\n" +
                    "    - 注意路面湿滑，减速慢行")
        if (temp > 35)
            sb.append("    - 高温天气禁止长时间骑行，易中暑\n" +
                    "    - 建议避开正午时段，选择清晨或傍晚")
        if (temp < 0)
            sb.append("    - 严寒天气注意保暖，佩戴防风手套\n" +
                    "    - 小心路面结冰")


        // 健康与环保
        sb.append("\n\n💪 健康与环保：\n")
        sb.append("    - 零碳排放，环保出行\n")
        sb.append("    - 每公里可减少约0.15kg碳排放\n")
        sb.append("    - 中等强度有氧运动，锻炼心肺功能\n")
        sb.append("    - 每小时消耗300-600卡路里")

        return sb.toString()
    }

    private fun generateCarRecommendation(aqi: Int, distance: Float): String {
        val timeHours = distance / 80f // 统一为高速80km/h
        val hours = timeHours.toInt()
        val minutes = ((timeHours * 60) % 60).toInt()
        val carbon = calculateDriveCarbon(distance)

        val sb = StringBuilder("自驾出行灵活便捷，适合中长距离旅行。\n\n")
        sb.append("📏 距离：${"%.1f".format(distance)}公里\n")
        sb.append("⏱️ 预计时间：${hours}小时${minutes}分钟\n")
        sb.append("♻️ 碳排放：约${"%.1f".format(carbon)}kg（${if (isFuelCar()) "燃油车" else "电动车"}）\n\n")

        // 详细分析
        sb.append("🔍 详细分析：\n")
        when {
            distance > 1000f ->
                sb.append("✅ 超长距离：自驾${"%.1f".format(distance)}公里适合\n" +
                        "    - 行程自由度高，可沿途游览\n" +
                        "    - 建议2人以上轮换驾驶")
            distance > 500f ->
                sb.append("✅ 长距离：自驾${"%.1f".format(distance)}公里推荐\n" +
                        "    - 约需${hours}小时${minutes}分钟\n" +
                        "    - 建议中途休息2-3次")
            distance > 200f ->
                sb.append("✅ 中长距离：自驾${"%.1f".format(distance)}公里合适\n" +
                        "    - 约需${hours}小时${minutes}分钟\n" +
                        "    - 建议中途休息1-2次")
            distance > 50f ->
                sb.append("⏱️ 中等距离：自驾${"%.1f".format(distance)}公里可行\n" +
                        "    - 约需${hours}小时${minutes}分钟\n" +
                        "    - 建议1人驾驶")
            distance > 10f ->
                sb.append("⚠️ 短距离：自驾${"%.1f".format(distance)}公里不环保\n" +
                        "    - 约需${hours}小时${minutes}分钟\n" +
                        "    - 碳排放较高，建议选择公共交通")
            else ->
                sb.append("❌ 超短距离：自驾${"%.1f".format(distance)}公里不推荐\n" +
                        "    - 约需${hours}小时${minutes}分钟\n" +
                        "    - 碳排放高，停车困难")
        }

        // 成本估算
        val fuelCost = if (isFuelCar()) distance * 0.8f else distance * 0.2f
        sb.append("\n\n💰 成本估算：\n")
        sb.append("    - ${if (isFuelCar()) "燃油" else "电力"}成本：约${"%.1f".format(fuelCost)}元\n")
        sb.append("    - 高速费用：约${"%.1f".format(distance * 0.5f)}元\n")
        sb.append("    - 车辆损耗：约${"%.1f".format(distance * 0.3f)}元\n")
        sb.append("    - 总成本：约${"%.1f".format(fuelCost + distance * 0.5f + distance * 0.3f)}元")

        // 优化建议
        sb.append("\n\n🚗 优化建议：\n")
        sb.append("    - 拼车可减少50%人均碳排放\n")
        if (isFuelCar())
            sb.append("    - 保持经济时速(80-100km/h)节省燃油\n")
        else
            sb.append("    - 提前规划充电站位置\n")
        sb.append("    - 定期保养车辆，保持胎压正常\n")
        if (aqi > 150)
            sb.append("    - 空气质量差，建议开启内循环\n")

        // 环保替代方案
        if (distance > 200) {
            val trainTime = distance / 250f
            val trainHours = trainTime.toInt()
            val trainMinutes = ((trainTime * 60) % 60).toInt()
            sb.append("\n🚄 环保替代方案：\n")
            sb.append("    - 高铁：约${trainHours}小时${trainMinutes}分钟，碳排放减少70%\n")
            sb.append("    - 长途大巴：时间相近，碳排放减少40%")
        }

        return sb.toString()
    }

    // 修复：碳排放对比逻辑（按1人自驾计算）
    private fun generatePublicRecommendation(aqi: Int, distance: Float, publicType: String): String {
        val speed = when (publicType) {
            "高铁" -> 300f
            "火车" -> 120f
            "地铁" -> 40f
            else -> 25f
        }
        val timeHours = distance / speed
        val hours = timeHours.toInt()
        val minutes = ((timeHours * 60) % 60).toInt()
        val carbon = calculatePublicCarbon(distance, publicType)
        val driveCarbonPerPerson = calculateDriveCarbon(distance) // 1人自驾的碳排放
        val reducePercent = if (driveCarbonPerPerson > 0) {
            (100 - (carbon / driveCarbonPerPerson) * 100).toInt().coerceAtLeast(0)
        } else 100

        val sb = StringBuilder("${if (isSameCity) "公共交通" else "跨城公交"}是经济环保的集体出行方式，推荐选择${publicType}。\n\n")
        sb.append("📏 距离：${"%.1f".format(distance)}公里\n")
        sb.append("⏱️ 预计时间：${hours}小时${minutes}分钟\n")
        sb.append("♻️ 碳排放：约${"%.1f".format(carbon)}kg（人均）\n\n")

        // 详细分析
        sb.append("🔍 详细分析：\n")
        when (publicType) {
            "高铁" ->
                sb.append("✅ 高铁是${"%.1f".format(distance)}公里最佳选择\n" +
                        "    - 速度快：300公里/小时\n" +
                        "    - 准点率高：超过95%\n" +
                        "    - 舒适安全：宽敞座位，平稳行驶\n" +
                        "    - 建议提前1小时到车站安检")
            "火车" ->
                sb.append("✅ 普通火车是${"%.1f".format(distance)}公里经济选择\n" +
                        "    - 性价比高：票价约为高铁1/2\n" +
                        "    - 覆盖广：更多车站选择\n" +
                        "    - 建议选择卧铺长途旅行更舒适")
            "地铁" ->
                sb.append("✅ 地铁是${"%.1f".format(distance)}公里市内最佳选择\n" +
                        "    - 准时高效：不受交通拥堵影响\n" +
                        "    - 频次高：3-5分钟一班\n" +
                        "    - 建议避开早晚高峰")
            else ->
                sb.append("✅ 公交是${"%.1f".format(distance)}公里短途接驳选择\n" +
                        "    - 覆盖广：站点密集\n" +
                        "    - 经济：票价最低\n" +
                        "    - 建议使用实时公交APP查询到站时间")
        }

        // 成本估算
        val costPerKm = when (publicType) {
            "高铁" -> 0.5f
            "火车" -> 0.2f
            "地铁" -> 0.3f
            else -> 0.1f
        }
        val totalCost = distance * costPerKm
        sb.append("\n\n💰 成本估算：\n")
        sb.append("    - 票价：约${"%.1f".format(totalCost)}元\n")
        sb.append("    - 时间成本：${hours}小时${minutes}分钟\n")
        if (publicType == "高铁" || publicType == "火车")
            sb.append("    - 建议提前购票享受折扣")

        // 优势分析
        sb.append("\n\n🏆 优势分析：\n")
        sb.append("    - 人均碳排放比自驾低${reducePercent}%\n")  // 使用修复后的比例
        sb.append("    - 无需担心停车问题\n")
        sb.append("    - 途中可休息或工作\n")
        if (publicType == "高铁")
            sb.append("    - 提供免费WiFi和充电插座")

        // 优化建议
        sb.append("\n\n💡 优化建议：\n")
        if (aqi > 150)
            sb.append("    - 空气质量差，建议佩戴专业防护口罩\n")
        if (publicType == "公交" || publicType == "地铁")
            sb.append("    - 使用交通卡或手机支付可享受折扣\n")
        if (distance > 50)
            sb.append("    - 长途旅行建议携带颈枕和眼罩\n")

        // 环保效益
        sb.append("\n♻️ 环保效益：\n")
        sb.append("    - 人均碳排放仅${"%.2f".format(carbon)}kg\n")
        sb.append("    - 比自驾减少${reducePercent}%碳排放\n")  // 使用修复后的比例
        sb.append("    - 是最环保的长途出行方式之一")

        // 特别提示 - 长距离替代方案
        if (distance > 800) {
            val flightTime = (distance / 800f * 60).toInt()
            sb.append("\n\n✈️ 超长距离替代方案：\n")
            sb.append("    - 飞机：约${flightTime}分钟，适合${distance}公里以上行程\n")
            sb.append("    - 建议提前2小时到机场")
        }

        return sb.toString()
    }

    // ViewPager适配器
    inner class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}
