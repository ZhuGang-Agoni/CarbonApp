package com.zg.carbonapp.logic.model

import com.google.gson.annotations.SerializedName
//这里面是数据模型 可以理解为大类
data class PlaceResponse(val status: String, val places: List<Place>)

data class Place(val name: String, val location: Location,
                 @SerializedName("formatted_address") val address: String)
data class Location(val lng: String, val lat: String)