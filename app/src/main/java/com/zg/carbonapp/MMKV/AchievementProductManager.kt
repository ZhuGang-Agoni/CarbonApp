package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.Repository.AchievementProductRepository

object AchievementProductManager {
    private const val KEY_ACHIEVEMENT_UNLOCKED_ITEM_IDS = "achievement_unlocked_item_ids_"
    private const val KEY_ACHIEVEMENT_REMINDER_IDS = "achievement_reminder_ids_" // 用于控制对话框只显示一次
    private val mmkv = MMKV.defaultMMKV()
    private val gson = Gson()
    private val lock = Any()

    // 保存通过任务解锁的物品（参数为物品ID）
    fun unlockAchievementItem(userId: String, itemId: Int) {
        synchronized(lock) {
            val unlockedIds = getAchievementUnlockedItemIds(userId).toMutableSet()
            unlockedIds.add(itemId)
            val json = gson.toJson(unlockedIds)
            mmkv.encode("${KEY_ACHIEVEMENT_UNLOCKED_ITEM_IDS}$userId", json)
        }
    }

    // 获取用户通过任务已解锁的"物品ID集合"
    private fun getAchievementUnlockedItemIds(userId: String): Set<Int> {
        synchronized(lock) {
            val json = mmkv.decodeString("${KEY_ACHIEVEMENT_UNLOCKED_ITEM_IDS}$userId", "")
            return if (json.isNullOrEmpty()) {
                emptySet()
            } else {
                try {
                    val type = object : TypeToken<Set<Int>>() {}.type
                    gson.fromJson<Set<Int>>(json, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptySet()
                }
            }
        }
    }

    // 获取用户通过任务已解锁的"完整商品列表"（从专门的成就仓库获取）
    fun getAchievementUnlockedProducts(userId: String): List<VirtualProduct> {
        val unlockedIds = getAchievementUnlockedItemIds(userId)
        // 从专门的成就仓库筛选已解锁商品
        return AchievementProductRepository.allAchievements.filter { it.id in unlockedIds }
    }

    // 检查通过任务获取的物品是否已解锁
    fun isAchievementItemUnlocked(userId: String, productId: Int): Boolean {
        synchronized(lock) {
            return productId in getAchievementUnlockedItemIds(userId)
        }
    }

    // 记录已提醒过的成就ID（确保对话框只显示一次）
    private fun saveReminderId(userId: String, productId: Int) {
        synchronized(lock) {
            val reminderIds = getReminderIds(userId).toMutableSet()
            reminderIds.add(productId)
            val json = gson.toJson(reminderIds)
            mmkv.encode("${KEY_ACHIEVEMENT_REMINDER_IDS}$userId", json)
        }
    }

    // 获取已提醒过的成就ID集合
    private fun getReminderIds(userId: String): Set<Int> {
        synchronized(lock) {
            val json = mmkv.decodeString("${KEY_ACHIEVEMENT_REMINDER_IDS}$userId", "")
            return if (json.isNullOrEmpty()) {
                emptySet()
            } else {
                try {
                    val type = object : TypeToken<Set<Int>>() {}.type
                    gson.fromJson<Set<Int>>(json, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptySet()
                }
            }
        }
    }

    // 检查是否需要显示解锁提醒（未提醒过才显示）
    fun needShowReminder(userId: String, productId: Int): Boolean {
        return productId !in getReminderIds(userId)
    }

    // 解锁并标记为已提醒
    fun unlockAndMarkReminder(userId: String, productId: Int) {
        unlockAchievementItem(userId, productId)
        saveReminderId(userId, productId)
    }
}