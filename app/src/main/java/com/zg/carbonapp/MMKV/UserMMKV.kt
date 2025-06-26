package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.User

object UserMMKV {
    private const val USER_INFO_KEY = "user_info"
    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("user") }
    private val gson: Gson by lazy { GsonBuilder().create() }

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        mmkv.encode(USER_INFO_KEY, userJson)
    }

    fun getUser(): User? {
        val userJson = mmkv.decodeString(USER_INFO_KEY, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun clearUser() {
        mmkv.removeValueForKey(USER_INFO_KEY)
    }

    fun updateUser(updater: (User) -> Unit) {
        val currentUser = getUser() ?: return
        updater(currentUser)
        saveUser(currentUser)
    }
}