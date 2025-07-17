package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.ChallengeRecord
import com.zg.carbonapp.Dao.RecognitionRecord
import java.util.*

object GarbageRecordMMKV {
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

    // 获取最近3条记录
    fun getRecentRecords(): List<Any> {
        val challengeRecords = getChallengeRecords().take(3)
        val recognitionRecords = getRecognitionRecords().take(3)
        
        val allRecords = mutableListOf<Any>()
        allRecords.addAll(challengeRecords)
        allRecords.addAll(recognitionRecords)
        
        return allRecords.sortedByDescending { 
            when (it) {
                is ChallengeRecord -> it.timestamp
                is RecognitionRecord -> it.timestamp
                else -> 0L
            }
        }.take(3)
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

    // 遍历本地所有识别记录 调用后端API上传数据
    fun syncRecognitionRecordsToServer(clearLocalAfterSync: Boolean = false) {
        val records = getRecognitionRecords()
        for (record in records) {
            // TODO: 调用后端API上传单条识别记录（建议协程/异步）
            // ApiService.uploadRecognitionRecord(record)
        }
        if (clearLocalAfterSync) {
            mmkv.remove("recognition_records")
        }
    }

    // 批量保存挑战记录（覆盖本地） 后期可用于从后端拉取后覆盖本地
    fun saveChallengeRecords(records: List<ChallengeRecord>) {
        mmkv.encode("challenge_records", gson.toJson(records))
        // TODO: 可选：拉取后端数据后本地覆盖
    }

    // 批量保存识别记录（覆盖本地） 后期可用于从后端拉取后覆盖本地
    fun saveRecognitionRecords(records: List<RecognitionRecord>) {
        mmkv.encode("recognition_records", gson.toJson(records))
        // TODO: 可选：拉取后端数据后本地覆盖
    }

    // 从后端拉取挑战记录并存入本地（协程/异步环境下调用）
    suspend fun fetchAndSaveChallengeRecordsFromServer() {
        // TODO: 调用ApiService.getChallengeRecords()获取数据
        // val records = ApiService.getChallengeRecords()
        // saveChallengeRecords(records)
    }

    // 从后端拉取识别记录并存入本地（协程/异步环境下调用）
    suspend fun fetchAndSaveRecognitionRecordsFromServer() {
        // TODO: 调用ApiService.getRecognitionRecords()获取数据
        // val records = ApiService.getRecognitionRecords()
        // saveRecognitionRecords(records)
    }

    // 清空所有垃圾分类记录
    fun clearRecords() {
        mmkv.clearAll()
    }
} 