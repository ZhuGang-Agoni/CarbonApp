package com.zg.carbonapp.MMKV

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.User

object UserMMKV {
    private const val USER_INFO_KEY = "user_info"
    private val mmkv: MMKV by lazy { MMKV.mmkvWithID("user") }
    private val gson: Gson by lazy { GsonBuilder().create() }

    private val _userLiveData = MutableLiveData<User?>()
    val userLiveData: LiveData<User?> = _userLiveData

    // 保存用户数据时，同步更新LiveData
    fun saveUser(user: User) {
        // 原有的保存逻辑（例如序列化存储）
        val json = Gson().toJson(user)
        mmkv.encode("current_user", json)

        // 关键：更新LiveData，通知所有观察者
        _userLiveData.postValue(user)
    }

    // 获取用户数据时，同时初始化LiveData
    fun getUser(): User? {
        val json = mmkv.decodeString("current_user")
        return if (json.isNullOrEmpty()) {
            null
        } else {
            val user = Gson().fromJson(json, User::class.java)
            // 初始化LiveData
            if (_userLiveData.value == null) {
                _userLiveData.postValue(user)
            }
            user
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