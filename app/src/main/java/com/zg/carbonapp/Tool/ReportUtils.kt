package com.zg.carbonapp.Utils

import android.content.Context
import android.content.Intent
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Activity.GarbageReportActivity
import com.zg.carbonapp.Dao.RecognitionRecord
import java.text.SimpleDateFormat
import java.util.*

object ReportUtils {
    private const val KEY_LAST_REPORT_SHOWN = "last_report_shown_time"
    private val mmkv = MMKV.defaultMMKV()

    /**
     * 检查是否需要显示周报提醒
     * @return 是否需要显示
     */
    fun shouldShowWeeklyReport(records: List<RecognitionRecord>): Boolean {
        // 如果没有记录，不显示
        if (records.isEmpty()) return false

        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 只在周一检查上周报告（周日是1，周一是2，...，周六是7）
        if (currentDayOfWeek != Calendar.MONDAY) return false

        // 获取上周记录
        val lastWeekRecords = getLastWeekRecords(records)
        if (lastWeekRecords.isEmpty()) return false

        // 检查是否已经显示过本周报告
        val lastShownTime = mmkv.getLong(KEY_LAST_REPORT_SHOWN, 0)
        val lastShownCalendar = Calendar.getInstance()
        lastShownCalendar.timeInMillis = lastShownTime

        // 判断上次显示是否是本周
        return !isSameWeek(calendar.timeInMillis, lastShownTime)
    }

    /**
     * 获取上周的记录
     */
    fun getLastWeekRecords(records: List<RecognitionRecord>): List<RecognitionRecord> {
        val calendar = Calendar.getInstance()
        // 设置为上周日
        calendar.add(Calendar.DAY_OF_YEAR, -calendar.get(Calendar.DAY_OF_WEEK) + 1 - 7)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val lastWeekSunday = calendar.timeInMillis

        // 设置为本周日（上周的结束）
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val currentSunday = calendar.timeInMillis

        // 筛选出上周的记录（上周周一至周日）
        return records.filter { record ->
            record.timestamp > lastWeekSunday && record.timestamp <= currentSunday
        }
    }

    /**
     * 标记报告已显示
     */
    fun markReportShown() {
        mmkv.putLong(KEY_LAST_REPORT_SHOWN, System.currentTimeMillis())
    }

    /**
     * 判断两个时间是否在同一周
     */
    private fun isSameWeek(time1: Long, time2: Long): Boolean {
        if (time2 == 0L) return false

        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar1.timeInMillis = time1
        calendar2.timeInMillis = time2

        // 同一年且同一周
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * 启动周报Activity
     */
    fun startReportActivity(context: Context, records: List<RecognitionRecord>) {
        val intent = Intent(context, GarbageReportActivity::class.java)
        intent.putExtra("weekly_records", ArrayList(records))
        context.startActivity(intent)
        markReportShown()
    }

    // 判断当前周是否已过完（是否已过本周日）
    fun isCurrentWeekCompleted(): Boolean {
        val calendar = Calendar.getInstance()
        // 周日为一周的最后一天（1=周日，7=周六）
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    // 获取上周日期范围（上周一至上周日）
    fun getLastWeekDateRange(): Pair<String, String> {
        val sdf = SimpleDateFormat("MM月dd日", Locale.CHINA)
        val calendar = Calendar.getInstance()

        // 计算上周一
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val lastMonday = sdf.format(calendar.time)

        // 计算上周日
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val lastSunday = sdf.format(calendar.time)

        return Pair(lastMonday, lastSunday)
    }

    // 获取本周日期范围（本周一至本周日）
    fun getCurrentWeekDateRange(): Pair<String, String> {
        val sdf = SimpleDateFormat("MM月dd日", Locale.CHINA)
        val calendar = Calendar.getInstance()

        // 计算本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val currentMonday = sdf.format(calendar.time)

        // 计算本周日
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val currentSunday = sdf.format(calendar.time)

        return Pair(currentMonday, currentSunday)
    }
}
