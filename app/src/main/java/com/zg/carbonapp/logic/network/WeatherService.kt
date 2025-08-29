package com.zg.carbonapp.logic.network

import com.zg.carbonapp.Tool.AppApplication
import com.zg.carbonapp.logic.model.DailyResponse
import com.zg.carbonapp.logic.model.RealtimeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WeatherService {
//    获取当前的天气情况
    @GET("v2.5/${AppApplication.TOKEN}/{lng},{lat}/realtime.json")
    fun getRealtimeWeather(@Path("lng") lng: String, @Path("lat") lat: String):
            Call<RealtimeResponse>
//    获取那个啥未来的天气情况
    @GET("v2.5/${AppApplication.TOKEN}/{lng},{lat}/daily.json")
    fun getDailyWeather(@Path("lng") lng: String, @Path("lat") lat: String):
            Call<DailyResponse>
}