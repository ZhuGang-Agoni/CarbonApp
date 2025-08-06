package com.zg.carbonapp.Dao

//这个我就不解释了 这是那个啥 item
data class ItemTravelRecord(
    val travelModel:String,
    val travelRoute:String,
    val carbonCount:String,//这个是当天碳排放量
    val distance:String,
    val time:Long,
    val modelRavel:Int)
