package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

object SignInManager {
    private val mmkv = MMKV.mmkvWithID("sign_in_records")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 执行签到
    fun isTodaySigned(): Boolean {
        val signedDates = getSignedDates()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return signedDates.contains(today)
    }
    fun signIn(): Boolean {
        val today = LocalDate.now().format(dateFormatter)
        val signedDates = getSignedDates().toMutableList()

        // 已签到返回false
        if (signedDates.contains(today)) {
            return false
        }

        // 新增签到记录
        signedDates.add(today)
        mmkv.encode("signed_dates", Gson().toJson(signedDates))
        return true
    }

    // 获取所有已签到日期
    fun getSignedDates(): List<String> {
        val json = mmkv.decodeString("signed_dates", "[]")
        return try {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 获取连续签到天数
    fun getStreak(): Int {
        val signedDates = getSignedDates().sorted()
        if (signedDates.isEmpty()) return 0

        var streak = 1
        var prevDate = LocalDate.parse(signedDates.last(), dateFormatter)

        // 从后往前检查连续日期
        for (i in signedDates.size - 2 downTo 0) {
            val currentDate = LocalDate.parse(signedDates[i], dateFormatter)
            if (prevDate.minusDays(1).isEqual(currentDate)) {
                streak++
                prevDate = currentDate
            } else {
                break
            }
        }
        return streak
    }
}