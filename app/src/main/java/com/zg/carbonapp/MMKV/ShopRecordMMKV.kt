package com.zg.carbonapp.MMKV



import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.GameHistoryRecord
import com.zg.carbonapp.Dao.ShopRecord

object ShopRecordMMKV {
    private val mmkv = MMKV.mmkvWithID("shop_exchange")
    private val gson = Gson()
    private const val KEY_SHOP_RECORD = "shop_exchange_record"

    // 保存单个游戏记录
    fun saveShopRecord(record: ShopRecord) {
        val currentList = getShopRecordItem().toMutableList()
        currentList.add(record)
//        保存完了 立马 记得那个啥 存到列表里面去
        saveShopRecordList(currentList)
    }

    // 获取所有游戏记录
    fun getShopRecordItem(): List<ShopRecord> {
        val json = mmkv.decodeString(KEY_SHOP_RECORD, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<ShopRecord>>() {}.type
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
        mmkv.removeValueForKey(KEY_SHOP_RECORD)
    }


    // 私有方法：保存完整列表（内部使用）
    private fun saveShopRecordList(list: List<ShopRecord>) {
        val json = gson.toJson(list)
        mmkv.encode(KEY_SHOP_RECORD, json)
    }
}