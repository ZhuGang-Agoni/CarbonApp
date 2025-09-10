// TokenManager.kt
package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV

object TokenManager {
    private const val TOKEN_KEY = "token"
    private val mmkv = MMKV.mmkvWithID("user_token")

    // 获取用户token
    fun getToken(): String? {
        return try {
            mmkv.decodeString(TOKEN_KEY)
        } catch (e: Exception) {
            null
        }
    }

    // 保存用户token
    fun setToken(token: String) {
        mmkv.encode(TOKEN_KEY, token)
    }

    // 清除token
    fun clearToken() {
        mmkv.removeValueForKey(TOKEN_KEY)
    }

    // 检查是否已登录
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    // 清除所有token相关数据
    fun clearAll() {
        mmkv.clearAll()
    }
}