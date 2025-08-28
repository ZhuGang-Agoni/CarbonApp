package com.zg.carbonapp.logic.model

import com.google.gson.annotations.SerializedName
//天气模型model  至于这些东西完全都是按照那个啥彩云天气api返回的JSON格式的数据来编写的
data class RealtimeResponse(val status: String, val result: Result) {
    data class Result(val realtime: Realtime)
    data class Realtime(val skycon: String, val temperature: Float,
                        @SerializedName("air_quality") val airQuality: AirQuality)
    data class AirQuality(val aqi: AQI)
    data class AQI(val chn: Float)
}