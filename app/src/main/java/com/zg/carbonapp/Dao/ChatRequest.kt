package com.zg.carbonapp.Dao
//访问请求体
data class ChatRequest(val model:String,
                       val messages:List<Message>,
                       val stream:Boolean=false)
