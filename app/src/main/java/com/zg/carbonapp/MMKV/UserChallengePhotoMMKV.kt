package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zg.carbonapp.Dao.UserChallengePhoto

object UserChallengePhotoMMKV {
    private val mmkv by lazy { MMKV.mmkvWithID("user_challenge_photo") }
    private val gson = Gson()

    fun save(record: UserChallengePhoto) {
        val list = getAll().toMutableList()
        list.add(record)
        mmkv.encode("user_challenge_photos", gson.toJson(list))
        // TODO: 后端同步（可选，建议异步处理）
        // ApiService.uploadUserChallengePhoto(record)
    }

    fun getAll(): List<UserChallengePhoto> {
        val json = mmkv.decodeString("user_challenge_photos") ?: return emptyList()
        val type = object : TypeToken<List<UserChallengePhoto>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        mmkv.remove("user_challenge_photos")
    }

    fun delete(record: UserChallengePhoto) {
        val list = getAll().toMutableList()
        list.remove(record)
        mmkv.encode("user_challenge_photos", gson.toJson(list))
        // TODO: 后端同步（可选，建议异步处理）
        // ApiService.deleteUserChallengePhoto(record)
    }
} 