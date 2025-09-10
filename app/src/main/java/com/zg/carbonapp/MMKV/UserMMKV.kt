// UserMMKV.kt
package com.zg.carbonapp.MMKV

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.User

object UserMMKV {
    private const val USER_INFO_KEY = "user_info"
    private const val USER_ID_KEY = "user_id"
    private const val USER_TELEPHONE_KEY = "user_telephone"

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("user") }
    private val gson: Gson by lazy { GsonBuilder().create() }

    private val _userLiveData = MutableLiveData<User?>()
    val userLiveData: LiveData<User?> = _userLiveData

    // 保存用户数据时，同步更新LiveData
    fun saveUser(user: User) {
        val json = gson.toJson(user)
        mmkv.encode(USER_INFO_KEY, json)
        _userLiveData.postValue(user)
    }

    // 获取用户数据时，同时初始化LiveData
    fun getUser(): User? {
        val json = mmkv.decodeString(USER_INFO_KEY)
        return if (json.isNullOrEmpty()) {
            null
        } else {
            val user = gson.fromJson(json, User::class.java)
            // 初始化LiveData
            if (_userLiveData.value == null) {
                _userLiveData.postValue(user)
            }
            user
        }
    }

    // 保存用户ID
    fun setUserId(userId: String) {
        mmkv.encode(USER_ID_KEY, userId)
    }

    // 获取用户ID
    fun getUserId(): String? {
        return mmkv.decodeString(USER_ID_KEY)
    }

    // 保存用户电话号码
    fun setUserTelephone(userTelephone: String) {
        mmkv.encode(USER_TELEPHONE_KEY, userTelephone)
    }

    // 获取用户电话号码
    fun getUserTelephone(): String? {
        return mmkv.decodeString(USER_TELEPHONE_KEY)
    }

    // 清除所有用户数据
    fun clearUser() {
        mmkv.removeValueForKey(USER_INFO_KEY)
        mmkv.removeValueForKey(USER_ID_KEY)
        mmkv.removeValueForKey(USER_TELEPHONE_KEY)
        _userLiveData.postValue(null)
    }

    // 更新用户信息
    fun updateUser(updater: (User) -> Unit) {
        val currentUser = getUser() ?: return
        updater(currentUser)
        saveUser(currentUser)
    }
}