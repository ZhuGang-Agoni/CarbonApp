package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.Dao.TravelRecord
import java.util.Calendar

object TravelRecordManager {
    private val mmkv by lazy {
        // 确保MMKV已初始化（添加错误处理）
        try {
            MMKV.mmkvWithID("travel_record1") ?: throw Exception("MMKV初始化失败")
        } catch (e: Exception) {
            Log.e("TravelRecordManager", "MMKV初始化失败: ${e.message}")
            throw e
        }
    }
    private val gson = Gson()

//    fun getLastSevenDaysRecords(): List<ItemTravelRecord> {
//        val calendar = Calendar.getInstance().apply {
//            add(Calendar.DAY_OF_YEAR, -6) // 获取最近7天（包括今天）
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//        val startTime = calendar.timeInMillis
//
//        return getRecords().list.filter { record ->
//            record.time >= startTime
//        }
//    }

    // 保存出行记录
    fun saveRecord(records: TravelRecord) {
        try {
            val json = gson.toJson(records)
            mmkv.encode("record_list", json)
            Log.d("TravelRecordManager", "记录保存成功: $json")

            // 用户碳积分更新（保留原有逻辑，但添加错误处理）
            updateUserCarbonPoints(records.carbonPoint)
        } catch (e: Exception) {
            Log.e("TravelRecordManager", "保存记录失败: ${e.message}", e)
            throw e
        }
    }

    // 获取出行记录
    fun getRecords(): TravelRecord {
        try {
            val json = mmkv.decodeString("record_list")
            Log.d("TravelRecordManager", "获取记录: $json")

            return if (json.isNullOrEmpty()) {
                // 返回默认记录（userId设为默认值，避免空值）
                TravelRecord(userId = "default_user", list = emptyList())
            } else {
                gson.fromJson(json, TravelRecord::class.java)
            }
        } catch (e: Exception) {
            Log.e("TravelRecordManager", "解析记录失败: ${e.message}", e)
            // 返回默认记录，确保不会崩溃
            return TravelRecord(userId = "default_user", list = emptyList())
        }
    }

    // 清除所有记录
    fun clearRecords() {
        mmkv.remove("record_list")
        Log.d("TravelRecordManager", "记录已清除")
    }

    // 独立方法更新用户碳积分（减少耦合），这里假设 UserMMKV 有对应的获取和保存用户方法
    private fun updateUserCarbonPoints(carbonPoints: String) {
        try {
            val user = UserMMKV.getUser()
            user?.let {
                val newCarbonCount = carbonPoints.toIntOrNull() ?: 0
                val newUser = it.copy(carbonCount = newCarbonCount)
                UserMMKV.saveUser(newUser)
                Log.d("TravelRecordManager", "用户碳积分更新为: $newCarbonCount")
            }
        } catch (e: Exception) {
            Log.e("TravelRecordManager", "更新用户碳积分失败: ${e.message}", e)
            // 积分更新失败不影响主流程
        }
    }
}
