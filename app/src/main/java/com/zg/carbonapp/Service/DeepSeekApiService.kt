package com.zg.carbonapp.Service

import com.zg.carbonapp.Dao.ChatRequest
import com.zg.carbonapp.Dao.ChatResponse
//import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DeepSeekApiService {//这个东西就我们而言  没有必要去纠结
    @POST("chat/completions")
    fun sendMessage(
        @Header("Authorization") authToken: String,
        @Body request: ChatRequest
    ): Call<ChatResponse>
}