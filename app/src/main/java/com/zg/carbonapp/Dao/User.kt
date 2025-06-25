package com.zg.carbonapp.Dao

import com.google.gson.Gson
import java.security.Signature

data class User(val userId:String,
                var userName:String="碳助手",
                var userEvator:String,
                var userPassword:String,
                val userQQ:String,
                val userTelephone:String,
                var signature: String="",
                val carbonCount:Int
){
    fun copy(): User {
        return User(
            userId = userId,
            userName = userName,
            userEvator = userEvator,
            userPassword = userPassword,
            userQQ = userQQ,
            userTelephone=userTelephone,
            signature=signature,
            carbonCount = carbonCount
        )
    }
}
