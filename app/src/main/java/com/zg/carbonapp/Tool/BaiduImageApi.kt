package com.zg.carbonapp.Tool

import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object BaiduImageApi {
    private const val API_KEY = "lJb2r1O5GEmAWtgiTQgPs1qa"
    private const val SECRET_KEY = "sgRXvImZ9g18yTxmP9RWCQGPKKmkqzAR"
    private var accessToken: String? = null
    private var tokenExpireTime: Long = 0L
    private const val TAG = "BaiduImageApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 获取access_token（自动缓存2小时）
    private fun getAccessToken(callback: (String?) -> Unit) {
        val now = System.currentTimeMillis()
        if (accessToken != null && now < tokenExpireTime) {
            Log.d(TAG, "使用缓存的accessToken: $accessToken")
            callback(accessToken)
            return
        }
        val url = "https://aip.baidubce.com/oauth/2.0/token"
        val body = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", API_KEY)
            .add("client_secret", SECRET_KEY)
            .build()
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "accessToken获取失败: ${e.message}")
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()
                if (body == null) {
                    Log.e(TAG, "accessToken响应体为空")
                    callback(null)
                    return
                }
                val json = body.string()
                Log.d(TAG, "accessToken返回: $json")
                val obj = JSONObject(json)
                accessToken = obj.optString("access_token")
                tokenExpireTime = now + 1000L * obj.optLong("expires_in", 7200)
                callback(accessToken)
            }
        })
    }

    // 图片转Base64
    private fun encodeImageToBase64(imagePath: String): String {
        val bytes = File(imagePath).readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // 调用百度物体识别API，返回物体名称（keyword）
    fun recognizeImage(imagePath: String, callback: (String?) -> Unit) {
        getAccessToken { token ->
            if (token == null) {
                Log.e(TAG, "accessToken为null，无法识别")
                callback(null)
                return@getAccessToken
            }
            val url = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general?access_token=$token"
            val imageBase64 = encodeImageToBase64(imagePath)
            val body = FormBody.Builder()
                .add("image", imageBase64)
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "识别请求失败: ${e.message}")
                    callback(null)
                }
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body()
                    if (body == null) {
                        Log.e(TAG, "识别响应体为空")
                        callback(null)
                        return
                    }
                    val json = body.string()
                    Log.d(TAG, "识别API返回: $json")
                    val obj = JSONObject(json)
                    if (obj.has("error_code")) {
                        Log.e(TAG, "API错误: ${obj.optString("error_msg")}")
                        callback(null)
                        return
                    }
                    val resultArr = obj.optJSONArray("result")
                    val keyword = if (resultArr != null && resultArr.length() > 0) {
                        resultArr.getJSONObject(0).optString("keyword")
                    } else null
                    Log.d(TAG, "识别结果keyword: $keyword")
                    callback(keyword)
                }
            })
        }
    }
}
