package com.zg.carbonapp.MMKV

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.UserComment
import com.zg.carbonapp.Dao.UserFeed

object MMKVManager {
    private val mmkv by lazy { MMKV.mmkvWithID("community_feed2") }
    private val gson = Gson()

    // 原有动态相关键
    private const val KEY_ALL_FEEDS = "all_feeds"         // 所有社区动态
    private const val KEY_LIKED_FEEDS = "liked_feeds"     // 点赞动态
    private const val KEY_SAVED_FEEDS = "saved_feeds"     // 收藏动态
    private const val KEY_COMMENTED_FEEDS = "commented_feeds" // 评论过的动态
    // 新增：评论相关键（用"COMMENTS_" + feedId作为键，实现单条动态对应评论列表）
    private const val KEY_COMMENTS_PREFIX = "COMMENTS_"    // 评论键前缀（如 COMMENTS_feed123）

    // ======================== 原有动态管理方法（不变） ========================
    fun saveAllFeeds(feeds: List<UserFeed>) {
        mmkv.encode(KEY_ALL_FEEDS, gson.toJson(feeds))
    }

    fun getAllFeeds(): List<UserFeed> {
        val json = mmkv.decodeString(KEY_ALL_FEEDS) ?: return emptyList()
        val type = object : TypeToken<List<UserFeed>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MMKV", "解析所有动态失败: ${e.message}")
            emptyList()
        }
    }

    fun saveLikedFeeds(feeds: List<UserFeed>) {
        mmkv.encode(KEY_LIKED_FEEDS, gson.toJson(feeds))
    }

    fun getLikedFeeds(): List<UserFeed> {
        val json = mmkv.decodeString(KEY_LIKED_FEEDS) ?: return emptyList()
        val type = object : TypeToken<List<UserFeed>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MMKV", "解析点赞动态失败: ${e.message}")
            emptyList()
        }
    }

    fun saveSavedFeeds(feeds: List<UserFeed>) {
        mmkv.encode(KEY_SAVED_FEEDS, gson.toJson(feeds))
    }

    fun getSavedFeeds(): List<UserFeed> {
        val json = mmkv.decodeString(KEY_SAVED_FEEDS) ?: return emptyList()
        val type = object : TypeToken<List<UserFeed>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MMKV", "解析收藏动态失败: ${e.message}")
            emptyList()
        }
    }

    fun saveCommentedFeeds(feeds: List<UserFeed>) {
        mmkv.encode(KEY_COMMENTED_FEEDS, gson.toJson(feeds))
    }

    fun getCommentedFeeds(): List<UserFeed> {
        val json = mmkv.decodeString(KEY_COMMENTED_FEEDS) ?: return emptyList()
        val type = object : TypeToken<List<UserFeed>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MMKV", "解析评论动态失败: ${e.message}")
            emptyList()
        }
    }

    // ======================== 新增：评论管理方法（核心） ========================
    /**
     * 1. 保存单条评论到指定动态的评论列表
     * @param feedId 动态ID（关联评论所属动态）
     * @param comment 要保存的评论
     */
    fun addComment(feedId: String, comment: UserComment) {
        try {
            // 1. 获取该动态已有的评论列表
            val existingComments = getCommentsByFeedId(feedId).toMutableList()
            // 2. 添加新评论（去重，避免重复提交）
            val isDuplicate = existingComments.any {
                it.userId == comment.userId && it.commentTime == comment.commentTime
            }
            if (!isDuplicate) {
                existingComments.add(comment)
                // 3. 按评论时间倒序（最新评论在上面）
                existingComments.sortByDescending { it.commentTime }
                // 4. 保存回MMKV（用前缀+feedId作为键）
                val commentKey = "${KEY_COMMENTS_PREFIX}$feedId"
                mmkv.encode(commentKey, gson.toJson(existingComments))
                // 5. 同步更新动态的评论计数
                updateFeedCommentCount(feedId, existingComments.size)
            }
        } catch (e: Exception) {
            Log.e("MMKV", "保存评论失败: ${e.message}")
        }
    }

    /**
     * 2. 通过feedId获取指定动态的所有评论
     * @param feedId 动态ID
     * @return 该动态的评论列表（倒序）
     */
    fun getCommentsByFeedId(feedId: String): List<UserComment> {
        try {
            val commentKey = "${KEY_COMMENTS_PREFIX}$feedId"
            val json = mmkv.decodeString(commentKey) ?: return emptyList()
            val type = object : TypeToken<List<UserComment>>() {}.type
            val comments = gson.fromJson<List<UserComment>>(json, type) ?: emptyList()
            // 按时间倒序返回（确保最新评论在顶部）
            return comments.sortedByDescending { it.commentTime }
        } catch (e: Exception) {
            Log.e("MMKV", "获取评论列表失败(feedId=$feedId): ${e.message}")
            return emptyList()
        }
    }

    /**
     * 3. 删除指定动态的单条评论
     * @param feedId 动态ID
     * @param comment 要删除的评论（通过userId+commentTime唯一匹配）
     */
    fun deleteComment(feedId: String, comment: UserComment) {
        try {
            // 1. 获取该动态的所有评论
            val existingComments = getCommentsByFeedId(feedId).toMutableList()
            // 2. 移除匹配的评论（userId+commentTime唯一标识）
            val removed = existingComments.removeAll {
                it.userId == comment.userId && it.commentTime == comment.commentTime
            }
            if (removed) {
                // 3. 保存更新后的评论列表
                val commentKey = "${KEY_COMMENTS_PREFIX}$feedId"
                mmkv.encode(commentKey, gson.toJson(existingComments))
                // 4. 同步更新动态的评论计数
                updateFeedCommentCount(feedId, existingComments.size)
            }
        } catch (e: Exception) {
            Log.e("MMKV", "删除评论失败: ${e.message}")
        }
    }

    /**
     * 4. 清空指定动态的所有评论
     * @param feedId 动态ID
     */
    fun clearCommentsByFeedId(feedId: String) {
        try {
            val commentKey = "${KEY_COMMENTS_PREFIX}$feedId"
            mmkv.removeValueForKey(commentKey)
            // 同步更新评论计数为0
            updateFeedCommentCount(feedId, 0)
        } catch (e: Exception) {
            Log.e("MMKV", "清空评论失败(feedId=$feedId): ${e.message}")
        }
    }

    /**
     * 辅助方法：同步更新动态的评论计数
     * @param feedId 动态ID
     * @param newCount 最新评论数
     */
    private fun updateFeedCommentCount(feedId: String, newCount: Int) {
        try {
            // 1. 获取所有动态
            val allFeeds = getAllFeeds().toMutableList()
            // 2. 找到对应的动态并更新评论数
            val feedIndex = allFeeds.indexOfFirst { it.feedId == feedId }
            if (feedIndex != -1) {
                val updatedFeed = allFeeds[feedIndex].copy(commentCount = newCount)
                allFeeds[feedIndex] = updatedFeed
                // 3. 保存更新后的所有动态
                saveAllFeeds(allFeeds)
                // 4. 若评论数从0变为>0，将动态加入"评论过的动态"列表；反之移除
                updateCommentedFeedsList(updatedFeed)
            }
        } catch (e: Exception) {
            Log.e("MMKV", "更新评论计数失败: ${e.message}")
        }
    }

    /**
     * 辅助方法：更新"评论过的动态"列表（确保评论页能找到该动态）
     * @param feed 最新状态的动态
     */
    private fun updateCommentedFeedsList(feed: UserFeed) {
        val commentedFeeds = getCommentedFeeds().toMutableList()
        if (feed.commentCount > 0) {
            // 评论数>0：加入列表（去重）
            if (!commentedFeeds.any { it.feedId == feed.feedId }) {
                commentedFeeds.add(feed)
                saveCommentedFeeds(commentedFeeds)
            }
        } else {
            // 评论数=0：从列表移除
            commentedFeeds.removeAll { it.feedId == feed.feedId }
            saveCommentedFeeds(commentedFeeds)
        }
    }

    // 清除所有数据（调试用，含评论）
    fun clearAllData() {
        mmkv.clearAll()
    }
}