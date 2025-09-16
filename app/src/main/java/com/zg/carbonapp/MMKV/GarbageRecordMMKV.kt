package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.ChallengeRecord
import com.zg.carbonapp.Dao.RecognitionRecord
import java.util.*

object GarbageRecordMMKV {
    private const val KEY_RECOGNITION_RECORDS = "recognition_records"

    private const val KEY_CHALLENGE_RECORDS = "challenge_records"
    private const val KEY_LAST_REPORT_SHOW_TIME = "last_report_show_time"
    private val mmkv by lazy { MMKV.mmkvWithID("garbage_record") }
    private val gson = Gson()

    // 保存挑战记录
    fun saveChallengeRecord(record: ChallengeRecord) {
        val records = getChallengeRecords().toMutableList()
        records.add(record)
        mmkv.encode("challenge_records", gson.toJson(records))
    }

    // 获取挑战记录
    fun getChallengeRecords(): List<ChallengeRecord> {
        val json = mmkv.decodeString("challenge_records") ?: return emptyList()
        val type = object : TypeToken<List<ChallengeRecord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("GarbageRecordMMKV", "解析challenge_records失败: ${e.message}")
            emptyList()
        }
    }

    fun clearChallengeRecords() {
        mmkv.remove(KEY_CHALLENGE_RECORDS)
    }


    // 保存识别记录
    fun saveRecognitionRecord(record: RecognitionRecord) {
        val records = getRecognitionRecords().toMutableList()
        records.add(record)
        mmkv.encode("recognition_records", gson.toJson(records))
    }

    // 获取识别记录
    fun getRecognitionRecords(): List<RecognitionRecord> {
        val json = mmkv.decodeString("recognition_records") ?: return emptyList()
        val type = object : TypeToken<List<RecognitionRecord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("GarbageRecordMMKV", "解析recognition_records失败: ${e.message}")
            emptyList()
        }
    }


    // 清空识别记录
    fun clearRecognitionRecords() {
        mmkv.remove(KEY_RECOGNITION_RECORDS)
    }

    // 获取指定时间段内的记录
    fun getRecordsInTimeRange(startTime: Long, endTime: Long): List<RecognitionRecord> {
        return getRecognitionRecords().filter { it.timestamp in startTime..endTime }
    }

    // 保存上次报告显示时间
    fun saveLastReportShowTime(time: Long) {
        mmkv.encode(KEY_LAST_REPORT_SHOW_TIME, time)
    }

    // 获取上次报告显示时间
    fun getLastReportShowTime(): Long {
        return mmkv.decodeLong(KEY_LAST_REPORT_SHOW_TIME, 0)
    }


    // 获取所有记录（挑战记录 + 识别记录）
    fun getAllRecords(): List<Any> {
        val allRecords = mutableListOf<Any>()
        allRecords.addAll(getChallengeRecords())
        return allRecords.sortedByDescending {
            when (it) {
                is ChallengeRecord -> it.timestamp
                else -> 0L
            }
        }
    }

    // 获取分类统计数据
    fun getCategoryStats(): Map<String, Int> {
        val stats = mutableMapOf(
            "可回收物" to 0,
            "有害垃圾" to 0,
            "厨余垃圾" to 0,
            "其他垃圾" to 0
        )

        // 只统计识别记录中的分类
        getRecognitionRecords().forEach { record ->
            val category = record.category
            if (stats.containsKey(category)) {
                stats[category] = stats[category]!! + 1
            }
        }

        return stats
    }

    // 获取总记录数
    fun getTotalRecordCount(): Int {
        return getAllRecords().size
    }

    // 获取最近3条记录
    fun getRecentRecords(): List<Any> {
        return getAllRecords().take(3)
    }

    // 检查今日挑战次数
    fun getTodayChallengeCount(): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return getChallengeRecords().count {
            it.timestamp >= today && it.isFinished // 只统计完整挑战
        }
    }

    // 遍历本地所有挑战记录 调用后端API上传数据
    fun syncChallengeRecordsToServer(clearLocalAfterSync: Boolean = false) {
        val records = getChallengeRecords()
        for (record in records) {
            // TODO: 调用后端API上传单条挑战记录（建议协程/异步）
            // ApiService.uploadChallengeRecord(record)
        }
        if (clearLocalAfterSync) {
            mmkv.remove("challenge_records")
        }
    }

    // 清空所有垃圾分类记录
    fun clearRecords() {
        mmkv.clearAll()
    }
}