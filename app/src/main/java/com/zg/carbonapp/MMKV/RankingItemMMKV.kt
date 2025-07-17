package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.RankingItem

object RankingItemMMKV {
    private val mmkv = MMKV.mmkvWithID("ranking_item")
    private val gson = Gson()

    // 保存整个排行榜数据
    fun saveRankingItem(list: List<RankingItem>) {
        val json = gson.toJson(list)
        mmkv.encode("ranking", json)
    }

    // 获取排行榜数据，添加了空值检查和正确的类型解析
    fun getRankingItem(): List<RankingItem> {
        val json = mmkv.decodeString("ranking", null)
        return if (json != null) {
            try {
                // 使用 TypeToken 获取正确的类型信息
                val type = object : TypeToken<List<RankingItem>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // 清除排行榜数据
    fun clearRankingItem() {
        mmkv.removeValueForKey("ranking")
    }

    // 更新单个排行榜项目
    fun updateRankingItem(item: RankingItem) {
        val list = getRankingItem().toMutableList()

        // 查找是否已存在该项目
        val index = list.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            // 存在则更新
            list[index] = item
        } else {
            // 不存在则添加
            list.add(item)
        }

        // 保存更新后的列表
        saveRankingItem(list)
    }

    // 删除单个排行榜项目
    fun deleteRankingItem(id: String) {
        val list = getRankingItem().toMutableList()
        list.removeIf { it.id == id }
        saveRankingItem(list)
    }
}