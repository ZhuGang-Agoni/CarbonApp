package com.zg.carbonapp.logic.network



import com.zg.carbonapp.Tool.AppApplication
import com.zg.carbonapp.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
//定义一个接口
interface PlaceService {

    @GET("v2/place?token=${AppApplication.TOKEN}&lang=zh_CN")
    fun searchPlaces(@Query("query") query:String): Call<PlaceResponse>
}