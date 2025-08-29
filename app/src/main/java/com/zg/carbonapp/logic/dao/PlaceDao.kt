package com.zg.carbonapp.logic.dao

import android.content.Context
import com.google.gson.Gson
import com.zg.carbonapp.Tool.AppApplication
import com.zg.carbonapp.logic.model.Place


object PlaceDao {
//   存放JSON
    fun savePlace(place: Place){
        val e= sharedPreferences().edit()
        e.putString("place", Gson().toJson(place))
    }
//    获取数据

    fun getSavedPlace():Place {
        val placeJson=  sharedPreferences().getString("place","")
        return Gson().fromJson(placeJson,Place::class.java)
    }

    fun isPlaceSaved()= sharedPreferences().contains("place")




    private fun sharedPreferences()=AppApplication.context.getSharedPreferences("sunny_weather",
        Context.MODE_PRIVATE)
}