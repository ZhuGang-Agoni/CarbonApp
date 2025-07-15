package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.TravelRecord

object TravelRecordManager {
    private val mmkv by lazy { MMKV.mmkvWithID("travel_record") }  // 单独的存储空间
    private val gson = Gson()  // JSON解析工具

    // 保存出行记录 列表是封装在这里面的
    fun saveRecord(records: TravelRecord) {
        val json = gson.toJson(records)
        mmkv.encode("record_list", json)  // 存储JSON字符串
    }

    // 获取出行记录 出行纪录的列表是封装在这里面的
    fun getRecords(): TravelRecord {
        val json = mmkv.decodeString("record_list")
        return try {
            // 若 json 为 null，用空 JSON 字符串 "{}" 兜底
            gson.fromJson(json ?: "{}", TravelRecord::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            TravelRecord(userId="1234",list= emptyList()) // 解析失败时返回默认实例
        }
    }

    // 清除所有记录
    fun clearRecords() {
        mmkv.remove("record_list")
    }
}