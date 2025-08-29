package com.zg.carbonapp.MMKV

import android.content.Context
import com.tencent.mmkv.MMKV
//import com.zg.tttttttttttttt.model.GameRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zg.carbonapp.model.GameRecord
import java.util.*

object RecordManager {
    private const val KEY_RECORDS = "game_records"
    private lateinit var mmkv: MMKV
    private val gson = Gson()

    /**
     * 初始化MMKV
     */
    fun initialize(context: Context) {
        MMKV.initialize(context)
        mmkv = MMKV.mmkvWithID("game_records")
    }

    /**
     * 添加新记录（自动使用当前时间）
     */
    fun addRecord(score: Int, playerType: String, playerAvatar: Int) {
        val record = GameRecord(
            score = score,
            playerType = playerType,
            date = Date(),
            playerAvatar = playerAvatar
        )
        addRecord(record)
    }

    /**
     * 添加新记录（直接使用传入的记录）
     */
    fun addRecord(record: GameRecord) {
        try {
            val records = getRecords().toMutableList()
            records.add(record)
            saveRecords(records)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取所有记录（未排序）
     */
    fun getRecords(): List<GameRecord> {
        return try {
            val json = mmkv.getString(KEY_RECORDS, "[]")
            val type = object : TypeToken<List<GameRecord>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取按时间倒序排序的记录（最新的在前）
     */
    fun getRecordsByTime(): List<GameRecord> {
        return getRecords().sortedByDescending { it.date.time }
    }

    /**
     * 获取按分数降序排序的记录（最高分在前）
     */
    fun getRecordsByScore(): List<GameRecord> {
        return getRecords().sortedByDescending { it.score }
    }

    /**
     * 清除所有记录
     */
    fun clearRecords() {
        mmkv.remove(KEY_RECORDS)
    }

    /**
     * 保存记录列表到MMKV
     */
    private fun saveRecords(records: List<GameRecord>) {
        try {
            val json = gson.toJson(records)
            mmkv.putString(KEY_RECORDS, json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
