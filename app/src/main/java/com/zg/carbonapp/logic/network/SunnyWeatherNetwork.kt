package com.zg.carbonapp.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object SunnyWeatherNetwork {
//    获取palceService
      private val placeService=ServiceCreator.create<PlaceService>()
//    实现一个简单的重载功能 并且吧该函数挂起
      suspend fun searchPlaces(query: String)= placeService.searchPlaces(query).await()
//    协程 简化回调方法

      private val weatherService=ServiceCreator.create<WeatherService>()

     suspend fun getDailyWeather(lng:String,lat:String)= weatherService.getDailyWeather(lng,lat).await()

    suspend fun getRealtimeWeather(lng:String,lat:String)= weatherService.getRealtimeWeather(lng,lat).await()

      private suspend fun <T> Call<T>.await():T{
          return suspendCoroutine {continuation ->
              enqueue(object : Callback<T> {
                  override fun onResponse(call: Call<T>,response :Response<T>){
                      val body=response.body()

                      if (body!=null){
                            continuation.resume(body)
                      }
                      else {
                          continuation.resumeWithException(
                              RuntimeException("response is null")
                          )
                      }
                  }

                  override fun onFailure(call: Call<T>, t: Throwable) {
                      continuation.resumeWithException(t)
                  }
              })
          }
      }



}