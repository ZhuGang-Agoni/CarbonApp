package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.UserFeed

object MMKVManager {
    private val mmkv by lazy { MMKV.mmkvWithID("community_feed") }
   private val gson= Gson()

    // 保存动态列表
    fun saveFeeds(feeds: List<UserFeed>) {
        mmkv.encode("feeds", gson.toJson(feeds))
    }

    // 获取动态列表
    @Suppress("UNCHECKED_CAST")
    fun getFeeds(): List<UserFeed> {
        // 修正key名称为"feeds"，与保存时一致
        val json = mmkv.decodeString("feeds") ?: return emptyList()

        val type = object : TypeToken<List<UserFeed>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("MMKVManager", "解析feeds失败: ${e.message}")
            emptyList()
        }
    }

    // 清除所有数据
    fun clearFeeds() {
        mmkv.clearAll()
    }
}