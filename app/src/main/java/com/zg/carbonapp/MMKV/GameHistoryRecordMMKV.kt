package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.GameHistoryRecord

object GameHistoryRecordMMKV {
    private val mmkv = MMKV.mmkvWithID("game_history")
    private val gson = Gson()
    private const val KEY_GAME_RECORD = "game_record"

    // 保存单个游戏记录
    fun saveGameRecord(record: GameHistoryRecord) {
        val currentList = getGameRecordItem().toMutableList()
        currentList.add(record)
//        保存完了 立马 记得那个啥 存到列表里面去
        saveGameRecordList(currentList)
    }

    // 获取所有游戏记录
    fun getGameRecordItem(): List<GameHistoryRecord> {
        val json = mmkv.decodeString(KEY_GAME_RECORD, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<GameHistoryRecord>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // 清除所有游戏记录
    fun clearAllRecords() {
        mmkv.removeValueForKey(KEY_GAME_RECORD)
    }


    // 私有方法：保存完整列表（内部使用）
    private fun saveGameRecordList(list: List<GameHistoryRecord>) {
        val json = gson.toJson(list)
        mmkv.encode(KEY_GAME_RECORD, json)
    }
}