package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.DailyStepData
import java.text.SimpleDateFormat
import java.util.*

object StepCarbonMMKV {
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 保存当前设备总步数（用于计算每日基准）
    fun saveCurrentTotalSteps(total: Long) {
        mmkv.encode("current_total_steps", total)
    }

    // 获取当前设备总步数
    fun getCurrentTotalSteps() = mmkv.decodeLong("current_total_steps", 0L)

    // 保存每日步数基准（当天0点的总步数）
    fun saveDailyStepBase(date: String, base: Long) {
        mmkv.encode("daily_base_$date", base)
    }

    // 获取当日步数基准
    fun getDailyStepBase(date: String) = mmkv.decodeLong("daily_base_$date", 0L)

    // 保存最后一次记录步数的日期（用于判断是否跨天）
    fun saveLastStepDate(date: String) {
        mmkv.encode("last_step_date", date)
    }

    // 获取最后一次记录步数的日期
    fun getLastStepDate() = mmkv.decodeString("last_step_date", "") ?: ""

    // 保存今日数据（包含步数、碳减排等）
    fun saveTodayData(data: DailyStepData) {
        val key = "daily_${data.date}"
        data.calculateCarbon() // 保存前先计算碳减排和积分
        val dataJson = gson.toJson(data)
        mmkv.encode(key, dataJson)

        // 更新日期列表
        val dates = getDateList().toMutableList()
        if (!dates.contains(data.date)) {
            dates.add(data.date)
            mmkv.encode("date_list", gson.toJson(dates))
        }
    }

    // 获取今日数据
    fun getTodayData(): DailyStepData? {
        val date = dateFormat.format(Date())
        return getTodayData(date)
    }

    // 获取日期列表
    fun getDateList(): List<String> {
        val json = mmkv.decodeString("date_list", "[]")
        return gson.fromJson(json, stringListType) ?: emptyList()
    }

    // 获取一周数据
    fun getWeekData(): List<DailyStepData> {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }

        return (0..6).map { i ->
            val date = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            getTodayData(date) ?: DailyStepData(date)
        }
    }

    // 获取当月数据
    fun getMonthData(): List<DailyStepData> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)

        return getDateList()
            .filter { date ->
                val dateObj = dateFormat.parse(date) ?: return@filter false
                calendar.time = dateObj
                calendar.get(Calendar.MONTH) == currentMonth
            }
            .map { date ->
                getTodayData(date) ?: DailyStepData(date)
            }
    }

    // 根据日期获取数据
    fun getTodayData(date: String): DailyStepData? {
        val dataJson = mmkv.decodeString("daily_$date", "")
        return if (dataJson.isNullOrEmpty()) null else gson.fromJson(dataJson, DailyStepData::class.java)
    }
}
