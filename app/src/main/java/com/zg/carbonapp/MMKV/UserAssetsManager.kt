package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.Repository.VirtualProductRepository // 引入商品仓库

object UserAssetsManager {
    private const val KEY_UNLOCKED_ITEM_IDS = "unlocked_item_ids_" // 存储"物品ID"而非整个对象
    private const val KEY_CURRENT_BADGE_ID = "current_badge_id_"
    private const val KEY_CURRENT_FRAME_ID = "current_frame_id_"
    private const val KEY_CURRENT_ACCESSORY_ID = "current_accessory_id_"
    private val mmkv = MMKV.defaultMMKV()
    private val gson = Gson()
    private val lock = Any()

    // 保存解锁的物品（参数为物品ID）
    fun unlockItem(userId: String, itemId: Int) { // 注意：参数是itemId（Int），不是VirtualProduct
        synchronized(lock) {
            val unlockedIds = getUnlockedItemIds(userId).toMutableSet()
            unlockedIds.add(itemId)
            val json = gson.toJson(unlockedIds)
            mmkv.encode("${KEY_UNLOCKED_ITEM_IDS}$userId", json)
        }
    }


    // 获取用户已解锁的"物品ID集合"（核心修复：只存ID，不存对象）
    private fun getUnlockedItemIds(userId: String): Set<Int> {
        synchronized(lock) {
            val json = mmkv.decodeString("${KEY_UNLOCKED_ITEM_IDS}$userId", "")
            return if (json.isNullOrEmpty()) {
                emptySet()
            } else {
                try {
                    val type = object : TypeToken<Set<Int>>() {}.type // 解析为Int集合
                    gson.fromJson<Set<Int>>(json, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptySet()
                }
            }
        }
    }

    // 获取用户已解锁的"完整商品列表"（根据ID匹配仓库中的商品）
    fun getUnlockedProducts(userId: String): List<VirtualProduct> {
        val unlockedIds = getUnlockedItemIds(userId)
        // 从仓库中筛选出已解锁的商品（解决类型匹配问题）
        return VirtualProductRepository.allProducts.filter { it.id in unlockedIds }
    }

    // 检查物品是否已解锁
    fun isItemUnlocked(userId: String, productId: Int): Boolean {
        synchronized(lock) {
            return productId in getUnlockedItemIds(userId)
        }
    }
    // 设置当前使用的勋章ID
    fun setCurrentBadgeId(userId: String, badgeId: Int) {
        synchronized(lock) {
            mmkv.encode("${KEY_CURRENT_BADGE_ID}$userId", badgeId)
        }
    }


    // 获取当前使用的勋章ID
    fun getCurrentBadgeId(userId: String): Int {
        synchronized(lock) {
            return mmkv.decodeInt("${KEY_CURRENT_BADGE_ID}$userId", -1)
        }
    }


    fun setCurrentFrameId(userId: String, frameId: Int) {
        synchronized(lock) {
            mmkv.encode("${KEY_CURRENT_FRAME_ID}$userId", frameId)
        }
    }
    fun getCurrentFrameId(userId: String): Int {
        synchronized(lock) {
            return mmkv.decodeInt("${KEY_CURRENT_FRAME_ID}$userId", -1)
        }
    }

    // 新增：挂件逻辑（模仿勋章）
    fun setCurrentAccessoryId(userId: String, accessoryId: Int) {
        synchronized(lock) {
            mmkv.encode("${KEY_CURRENT_ACCESSORY_ID}$userId", accessoryId)
        }
    }
    fun getCurrentAccessoryId(userId: String): Int {
        synchronized(lock) {
            return mmkv.decodeInt("${KEY_CURRENT_ACCESSORY_ID}$userId", -1)
        }
    }
}
