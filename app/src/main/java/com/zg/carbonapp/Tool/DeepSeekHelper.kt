package com.zg.carbonapp.Tool

import android.util.Log
import com.google.gson.Gson
import com.zg.carbonapp.Dao.ChatRequest
import com.zg.carbonapp.Dao.ChatStreamResponse
import com.zg.carbonapp.Dao.Message
import com.zg.carbonapp.Service.DeepSeekApiService
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
//把结果都存在这里面 很细节捏 askFragment里面就可以直接那个啥调用了
class DeepSeekHelper {
    private val TAG = "DeepSeekHelper"
    private val apiService: DeepSeekApiService
    private val client: OkHttpClient
    private val BASE_KEY = "sk-a514d2f7941b4b4f91e1900e209e004d"
    private val gson = Gson()

    init {
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "text/event-stream")
                    .header("Connection", "keep-alive")
                    .header("Cache-Control", "no-cache")
                chain.proceed(requestBuilder.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(DeepSeekApiService::class.java)
    }

    // 流式请求（强制按字符拆分并逐个回调）
    fun sendMessageStream(
        prompt: String,
        charDelay: Long = 50, // 字符显示间隔（毫秒）
        onChar: (Char) -> Unit, // 每收到一个字符回调一次
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = ChatRequest(
            model = "deepseek-chat",
            messages = listOf(
                Message("system", "回答尽量简洁明了"),
                Message("user", prompt)
            ),
            stream = true
        )

        val authToken = "Bearer $BASE_KEY"
        val call = apiService.sendMessageStream(authToken, request)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    val body = response.body() ?: run {
                        withContext(Dispatchers.Main) { onError("响应体为空") }
                        return@launch
                    }

                    BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmedLine = line?.trim() ?: continue
                            if (trimmedLine.isEmpty() || trimmedLine.startsWith("event:")) continue
                            if (trimmedLine == "data: [DONE]") break

                            if (trimmedLine.startsWith("data: ")) {
                                val dataJson = trimmedLine.removePrefix("data: ").trim()
                                if (dataJson.isNotEmpty()) {
                                    try {
                                        val streamResponse = gson.fromJson(dataJson, ChatStreamResponse::class.java)
                                        val content = streamResponse.choices.firstOrNull()?.delta?.content ?: ""

                                        // 按字符拆分并逐个发送
                                        for (char in content) {
                                            withContext(Dispatchers.Main) {
                                                onChar(char) // 回调单个字符
                                            }
                                            delay(charDelay) // 控制字符显示速度
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "解析错误: $dataJson, ${e.message}")
                                    }
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) { onComplete() }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "未知错误"
                    withContext(Dispatchers.Main) { onError("请求失败(${response.code()}): $errorMsg") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("异常: ${e.message}") }
            } finally {
                if (!call.isCanceled) call.cancel()
            }
        }
    }
}
//package com.zg.carbonapp.Tool
//
//import android.util.Log
//import com.google.gson.Gson
//import com.zg.carbonapp.Dao.ChatRequest
//import com.zg.carbonapp.Dao.ChatStreamResponse
//import com.zg.carbonapp.Dao.Message
//import com.zg.carbonapp.Service.DeepSeekApiService
//import kotlinx.coroutines.*
//import okhttp3.*
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.io.BufferedReader
//import java.io.IOException
//import java.io.InputStreamReader
//import java.util.concurrent.TimeUnit
//
//class DeepSeekHelper {
//    private val TAG = "DeepSeekHelper"
//    private val apiService: DeepSeekApiService
//    private val client: OkHttpClient
//    private val BASE_KEY="sk-a514d2f7941b4b4f91e1900e209e004d"
//
//    init {
//        // 1. 配置OkHttp（解决超时问题）
//        client = OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)   // 连接超时
//            .readTimeout(120, TimeUnit.SECONDS)    // 流式响应读取超时延长
//            .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时
//            .retryOnConnectionFailure(true)        // 网络重试
//            .build()
//
//        // 2. 初始化Retrofit
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://api.deepseek.com/") // 替换为实际API地址
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        apiService = retrofit.create(DeepSeekApiService::class.java)
//    }
//
//    // 原有非流式方法（兼容旧逻辑）
//    fun sendMessage(prompt: String, callback: (String) -> Unit) {
//        val request = ChatRequest(
//            model = "deepseek-chat",
//            messages = listOf(Message("user", prompt))
//        )
//
//        val authToken = "Bearer $BASE_KEY" // 替换为实际API密钥
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = apiService.sendMessage(authToken, request).execute()
//                if (response.isSuccessful) {
//                    response.body()?.let { chatResponse ->
//                        val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
//                        withContext(Dispatchers.Main) {
//                            callback(content)
//                        }
//                    }
//                } else {
//                    Log.e(TAG, "API错误: ${response.code()} ${response.message()}")
//                    withContext(Dispatchers.Main) {
//                        callback("请求失败：${response.message()}")
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "异常: ${e.message}", e)
//                withContext(Dispatchers.Main) {
//                    callback("发生错误：${e.message}")
//                }
//            }
//        }
//    }
//
//
//    // 流式响应方法（修正版）
//    fun sendMessageStream(prompt: String, callback: (String) -> Unit) {
//        val request = ChatRequest(
//            model = "deepseek-chat", // 确认模型名称是否正确
//            messages = listOf(Message("user", prompt)),
//            stream = true // 必须开启流式
//        )
//
//        val authToken = "Bearer $BASE_KEY"
//        val call = apiService.sendMessageStream(authToken, request)
//
//        CoroutineScope(Dispatchers.IO).launch {
//            var fullResponse = ""
//            try {
//                val response = call.execute()
//                if (response.isSuccessful) {
//                    val body = response.body() ?: run {
//                        withContext(Dispatchers.Main) {
//                            callback("流式响应为空")
//                        }
//                        return@launch
//                    }
//
//                    // 读取原始流
//                    val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
//                    var line: String?
//                    while (reader.readLine().also { line = it } != null) {
//                        val trimmedLine = line?.trim() ?: continue
//                        // 过滤空行和非数据行
//                        if (trimmedLine.isEmpty()) continue
//                        if (trimmedLine.startsWith("event: ")) continue // 忽略事件行
//                        // 检查结束标志
//                        if (trimmedLine == "data: [DONE]") break
//
//                        // 提取data字段内容（去掉"data: "前缀）
//                        val dataJson = trimmedLine.removePrefix("data: ").trim()
//                        if (dataJson.isEmpty()) continue
//
//                        // 解析单条流式响应
//                        try {
//                            val streamResponse = Gson().fromJson(dataJson, ChatStreamResponse::class.java)
//                            val deltaContent = streamResponse.choices.firstOrNull()?.delta?.content ?: ""
//                            if (deltaContent.isNotEmpty()) {
//                                fullResponse += deltaContent
//                                // 主线程更新UI
//                                withContext(Dispatchers.Main) {
//                                    callback(fullResponse)
//                                }
//                            }
//                        } catch (e: Exception) {
//                            Log.e(TAG, "解析流式数据失败: $dataJson，错误: ${e.message}")
//                        }
//                    }
//                    reader.close()
//                } else {
//                    val errorBody = response.errorBody()?.string() ?: "未知错误"
//                    Log.e(TAG, "流式API错误: ${response.code()}，详情: $errorBody")
//                    withContext(Dispatchers.Main) {
//                        callback("流式请求失败（${response.code()}）：$errorBody")
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "流式请求异常: ${e.message}", e)
//                withContext(Dispatchers.Main) {
//                    callback("流式请求出错：${e.message ?: "未知错误"}")
//                }
//            }
//        }
//    }
//}