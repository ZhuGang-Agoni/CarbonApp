package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.tencent.mmkv.MMKV

object TokenManager {
    private val mmkv= MMKV.mmkvWithID("user_token")
    private val gson= Gson()

//获取用户的一个token
     fun getToken(): String? {
        return try {
              mmkv.decodeString("token")
        }
        catch (e:Exception){
            null
        }
    }
    //存用户的一个token
    fun setToken(token :String){
           mmkv.encode("token",token)
    }

    fun clearToken(){
        mmkv.clearAll()
    }

}