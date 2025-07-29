package com.zg.carbonapp.Service

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.carbonlabel.com/"
    private const val MAX_RETRY = 3

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(RetryInterceptor(MAX_RETRY))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    class RetryInterceptor(private val maxRetry: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response: Response? = null
            var exception: Exception? = null

            // 尝试次数从0到maxRetry（共maxRetry+1次）
            for (i in 0..maxRetry) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) {
                        return response
                    } else {
                        // 记录非成功响应
                        Log.w("RetryInterceptor", "Request failed with code: ${response.code}")
                    }
                } catch (e: Exception) {
                    exception = e
                    Log.w("RetryInterceptor", "Attempt $i failed: ${e.message}")
                }

                // 如果不是最后一次尝试，等待后重试
                if (i < maxRetry) {
                    try {
                        Thread.sleep(1000L * (i + 1)) // 指数退避：1s, 2s, 3s...
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }

            throw exception ?: IOException("请求失败，重试 $maxRetry 次后仍不成功")
        }
    }

    val instance: CarbonService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CarbonService::class.java)
    }
}