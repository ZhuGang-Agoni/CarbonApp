
package com.zg.carbonapp.Service

import com.zg.carbonapp.Dao.ChatRequest
import com.zg.carbonapp.Dao.ChatResponse
import com.zg.carbonapp.Dao.ChatStreamResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DeepSeekApiService {
    // 原有非流式接口（已适配修正后的ChatResponse）
    @POST("chat/completions")
    fun sendMessage(
        @Header("Authorization") authToken: String,
        @Body request: ChatRequest
    ): Call<ChatResponse>

    // 新增流式接口（适配修正后的ChatStreamResponse）
    // 流式接口（关键修改：返回原始响应体）
    @POST("chat/completions")
    fun sendMessageStream(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): Call<ResponseBody> // 用ResponseBody接收原始流
}