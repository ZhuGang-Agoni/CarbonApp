package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.logic.model.Weather

object CurrentWeatherMMKV {
    private val mmkv = MMKV.mmkvWithID("current_weather")
    private val gson = Gson()

    fun saveWeatherInfo(weather: Weather) {
        val json = gson.toJson(weather)
        mmkv.encode("weather", json)
    }

    fun getWeatherInfo(): Weather? {
        val json = mmkv.decodeString("weather")
        return if (json != null) {
            gson.fromJson(json, Weather::class.java)
        } else {
            null
        }
    }
}