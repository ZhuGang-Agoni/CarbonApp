package com.zg.carbonapp.Service

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    // 可以根据需要切换协议
    private const val BASE_URL_HTTP = "http://204.141.229.223:8080/"
    private const val BASE_URL_HTTPS = "https://204.141.229.223:8080/"

    // 默认使用 HTTP（根据实际情况调整）
    private const val BASE_URL = BASE_URL_HTTP

    private const val MAX_RETRY = 3

    // 创建一个不验证证书的 OkHttpClient（用于 HTTPS）
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // 创建一个信任所有证书的 TrustManager
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // 安装 TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // 创建不验证主机名的 ssl socket factory
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // 不验证主机名
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .addInterceptor(RetryInterceptor(MAX_RETRY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // 创建一个普通的 OkHttpClient（用于 HTTP）
    private fun getSafeOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(RetryInterceptor(MAX_RETRY))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 根据 BASE_URL 的协议选择不同的 OkHttpClient
    private val okHttpClient by lazy {
        if (BASE_URL.startsWith("https")) {
            getUnsafeOkHttpClient()
        } else {
            getSafeOkHttpClient()
        }
    }

    class RetryInterceptor(private val maxRetry: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response: Response? = null
            var exception: Exception? = null

            // 尝试次数从0到maxRetry（共maxRetry+1次）
            for (i in 0..maxRetry) {
                // 关闭上一次的响应（如果存在）
                response?.close()

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

            // 最终失败前确保关闭响应
            response?.close()
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


//package com.zg.carbonapp.Service
//
//import android.util.Log
//import okhttp3.Interceptor
//import okhttp3.OkHttpClient
//import okhttp3.Response
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.io.IOException
//import java.security.cert.X509Certificate
//import java.util.concurrent.TimeUnit
//import javax.net.ssl.SSLContext
//import javax.net.ssl.TrustManager
//import javax.net.ssl.X509TrustManager
//
//object RetrofitClient {
//    // 可以根据需要切换协议
//    private const val BASE_URL_HTTP = "http://204.141.229.223:8080/"
//    private const val BASE_URL_HTTPS = "https://204.141.229.223:8080/"
//
//    // 默认使用 HTTP（根据实际情况调整）
//    private const val BASE_URL = BASE_URL_HTTP
//
//    private const val MAX_RETRY = 3
//
//    // 创建一个不验证证书的 OkHttpClient（用于 HTTPS）
//    private fun getUnsafeOkHttpClient(): OkHttpClient {
//        try {
//            // 创建一个信任所有证书的 TrustManager
//            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//                override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
//                override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
//                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
//            })
//
//            // 安装 TrustManager
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
//
//            // 创建不验证主机名的 ssl socket factory
//            val sslSocketFactory = sslContext.socketFactory
//
//            return OkHttpClient.Builder()
//                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//                .hostnameVerifier { _, _ -> true } // 不验证主机名
//                .addInterceptor(HttpLoggingInterceptor().apply {
//                    level = HttpLoggingInterceptor.Level.BODY
//                })
//                .addInterceptor(RetryInterceptor(MAX_RETRY))
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .build()
//        } catch (e: Exception) {
//            throw RuntimeException(e)
//        }
//    }
//
//    // 创建一个普通的 OkHttpClient（用于 HTTP）
//    private fun getSafeOkHttpClient(): OkHttpClient {
//        return OkHttpClient.Builder()
//            .addInterceptor(HttpLoggingInterceptor().apply {
//                level = HttpLoggingInterceptor.Level.BODY
//            })
//            .addInterceptor(RetryInterceptor(MAX_RETRY))
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .build()
//    }
//
//    // 根据 BASE_URL 的协议选择不同的 OkHttpClient
//    private val okHttpClient by lazy {
//        if (BASE_URL.startsWith("https")) {
//            getUnsafeOkHttpClient()
//        } else {
//            getSafeOkHttpClient()
//        }
//    }
//
//    class RetryInterceptor(private val maxRetry: Int) : Interceptor {
//        override fun intercept(chain: Interceptor.Chain): Response {
//            val request = chain.request()
//            var response: Response? = null
//            var exception: Exception? = null
//
//            // 尝试次数从0到maxRetry（共maxRetry+1次）
//            for (i in 0..maxRetry) {
//                try {
//                    response = chain.proceed(request)
//                    if (response.isSuccessful) {
//                        return response
//                    } else {
//                        // 记录非成功响应
//                        Log.w("RetryInterceptor", "Request failed with code: ${response.code}")
//                    }
//                } catch (e: Exception) {
//                    exception = e
//                    Log.w("RetryInterceptor", "Attempt $i failed: ${e.message}")
//                }
//
//                // 如果不是最后一次尝试，等待后重试
//                if (i < maxRetry) {
//                    try {
//                        Thread.sleep(1000L * (i + 1)) // 指数退避：1s, 2s, 3s...
//                    } catch (e: InterruptedException) {
//                        Thread.currentThread().interrupt()
//                        break
//                    }
//                }
//            }
//
//            throw exception ?: IOException("请求失败，重试 $maxRetry 次后仍不成功")
//        }
//    }
//
//    val instance: CarbonService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(CarbonService::class.java)
//    }
//}