package com.zg.carbonapp.Tool

import com.zg.carbonapp.Dao.ChatRequest
import com.zg.carbonapp.Dao.ChatResponse
import com.zg.carbonapp.Dao.Message
import com.zg.carbonapp.Service.DeepSeekApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DeepSeekHelper{

    private val BASE_URL="https://api.deepseek.com/"//地址
    private val BASE_KEY="sk-a514d2f7941b4b4f91e1900e209e004d"//密钥
    private val deepSeekService=Retrofit.Builder()
        .baseUrl(BASE_URL)//这里为什么不用那个啥 ServiceCreator呢 因为根路经不一样
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(DeepSeekApiService::class.java)




       fun sendMessage(message:String,callback:(String)->Unit){//用一个高阶函数
           //构建请求体
           val request=ChatRequest(
                model= "deepseek-chat",
                messages=listOf(
                    Message("system", "You are a helpful assistant."),
                    Message("user", message)
                )
           )
           val authToken="Bearer $BASE_KEY"//这一步很细节啊
           //发送请求
           deepSeekService.sendMessage(authToken,request).enqueue(object : Callback<ChatResponse> {
               override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    val result=t.message
                    callback(result.toString()+"原因: ${t.cause}")//把结果回调
               }

               override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                   if (response.isSuccessful) {
                       try {
                           val result = response.body()
                           if (result!=null) {
                               val resList=result.choices
                               val stringBuilder=StringBuilder()
                               for (i in resList){//直接把结果全部添加
                                   stringBuilder.append(i.message)
                               }
                               val res=stringBuilder.toString()

                               callback(res)//最后的一个结果
                           }
                       }catch (e:Exception){
                           e.printStackTrace()
                           callback("解析失败")
                       }
                   }
                   else{
                        callback("请求失败: ${response.code()}")
                   }
               }

           })

       }

}