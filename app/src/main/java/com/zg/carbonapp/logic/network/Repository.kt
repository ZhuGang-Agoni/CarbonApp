package com.zg.carbonapp.logic.network

import androidx.lifecycle.liveData
import com.zg.carbonapp.logic.dao.PlaceDao
import com.zg.carbonapp.logic.model.Place
import com.zg.carbonapp.logic.model.Weather


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object Repository {

    fun searchPlaces(query:String)= liveData(Dispatchers.IO){
         val result=try{
             val placeResponse= SunnyWeatherNetwork.searchPlaces(query)
             //这里如果不理解自己去model里面找数据模型
             if (placeResponse.status=="ok"){
                 val place=placeResponse.places
                 Result.success(place)
             }
             else {
                 Result.failure(RuntimeException("response status is ${placeResponse.status}"))
             }
         }catch (e:Exception){
             Result.failure<List<Place>>(e)
         }
        emit(result)
    }

    fun refreshWeather(lng:String,lat :String)=liveData(Dispatchers.IO){
        val result=try {
            coroutineScope {
                val deferredRealtime=async {
                    SunnyWeatherNetwork.getRealtimeWeather(lng,lat)
                }
                val deferredDaily=async{
                    SunnyWeatherNetwork.getDailyWeather(lng,lat)
                }
                val realtimeResponse=deferredRealtime.await()
                val dailyResponse=deferredDaily.await()
//????? ok达成0k NB。。。啊？？？ 你好厉害啊 这都被你发现了  笑死了 ok达成0k 这种错误再犯，我得开骂了。。自己调。。
//                你去调整啊。OK 剩下的交给我了 你辛苦了 你室友睡觉了，啊
                if (realtimeResponse.status=="ok"&&dailyResponse.status=="ok"){
                    val weather= Weather(realtimeResponse.result.realtime,dailyResponse.result.daily)

                    Result.success(weather)
                }
                else {
                    Result.failure(RuntimeException("realtime response statue is ${realtimeResponse.status}"+
                   " daily response statue is ${dailyResponse.status}" ))
                }
            }

        }catch (e:Exception){
            Result.failure<Weather>(e)
        }
        emit(result)
    }

    fun savePlace(place: Place) = PlaceDao.savePlace(place)
    fun getSavedPlace() = PlaceDao.getSavedPlace()
    fun isPlaceSaved() = PlaceDao.isPlaceSaved()



}