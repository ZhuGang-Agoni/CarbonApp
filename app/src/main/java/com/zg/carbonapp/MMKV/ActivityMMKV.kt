package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zg.carbonapp.Dao.Activity

object ActivityMMKV {
    private const val KEY_JOINED_ACTIVITIES = "joined_activities_" // 格式: KEY_JOINED_ACTIVITIES + userId
    private val mmkv = MMKV.defaultMMKV()
    private val gson = Gson()
    private val lock = Any()

    // 保存用户报名的活动ID
    fun saveJoinedActivity(userId: String, activityId: Int) {
        synchronized(lock) {
            val joinedIds = getJoinedActivities(userId).toMutableSet()
            joinedIds.add(activityId)
            val json = gson.toJson(joinedIds)
            mmkv.encode("${KEY_JOINED_ACTIVITIES}$userId", json)
        }
    }

    // 获取用户已报名的活动ID集合
    fun getJoinedActivities(userId: String): Set<Int> {
        synchronized(lock) {
            val json = mmkv.decodeString("${KEY_JOINED_ACTIVITIES}$userId", "")
            return if (json.isNullOrEmpty()) {
                emptySet()
            } else {
                try {
                    val type = object : TypeToken<Set<Int>>() {}.type
                    gson.fromJson<Set<Int>>(json, type) ?: emptySet()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptySet()
                }
            }
        }
    }

    // 检查活动是否已报名
    fun isActivityJoined(userId: String, activityId: Int): Boolean {
        return activityId in getJoinedActivities(userId)
    }

    // 取消活动报名
    fun cancelActivityJoin(userId: String, activityId: Int) {
        synchronized(lock) {
            val joinedIds = getJoinedActivities(userId).toMutableSet()
            joinedIds.remove(activityId)
            val json = gson.toJson(joinedIds)
            mmkv.encode("${KEY_JOINED_ACTIVITIES}$userId", json)
        }
    }

    // 新增方法：获取报名指定活动的所有用户ID列表
    fun getJoinedUserIds(activityId: Int): List<String>? {
        synchronized(lock) {
            val allUsers = mmkv.allKeys()?.filter { it.startsWith(KEY_JOINED_ACTIVITIES) }
            val userIds = mutableListOf<String>()
            allUsers?.forEach { key ->
                // 解析出 userId，key 格式是 "joined_activities_${userId}"
                val userId = key.replace(KEY_JOINED_ACTIVITIES, "")
                val joinedActivities = getJoinedActivities(userId)
                if (joinedActivities.contains(activityId)) {
                    userIds.add(userId)
                }
            }
            return if (userIds.isNotEmpty()) userIds else null
        }
    }
}