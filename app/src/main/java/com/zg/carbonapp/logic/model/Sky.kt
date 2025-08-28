package com.zg.carbonapp.logic.model

import com.zg.carbonapp.R


class Sky (val info: String, val icon: Int, val bg: Int)
private val sky = mapOf(
    "CLEAR_DAY" to Sky("晴", R.drawable.sunny, R.drawable.sunny),
    "CLEAR_NIGHT" to Sky("晴", R.drawable.sunny, R.drawable.sunny),
    "PARTLY_CLOUDY_DAY" to Sky("多云", R.drawable.cloud__1_,
        R.drawable.cloud__1_),
    "PARTLY_CLOUDY_NIGHT" to Sky("多云", R.drawable.cloud__1_,
        R.drawable.cloud__1_),
    "CLOUDY" to Sky("阴", R.drawable.overcast__2_, R.drawable.overcast__2_),
    "WIND" to Sky("大风", R.drawable.wind, R.drawable.wind),
    "LIGHT_RAIN" to Sky("小雨", R.drawable.light_rain, R.drawable.light_rain),
    "MODERATE_RAIN" to Sky("中雨", R.drawable.moderate_rain, R.drawable.moderate_rain),
    "HEAVY_RAIN" to Sky("大雨", R.drawable.heavy_rain, R.drawable.heavy_rain),
    "STORM_RAIN" to Sky("暴雨", R.drawable.dabaoyu, R.drawable.dabaoyu),
    "THUNDER_SHOWER" to Sky("雷阵雨", R.drawable.thunder_shower, R.drawable.thunder_shower),
    "SLEET" to Sky("雨夹雪", R.drawable.cloud_sleet, R.drawable.cloud_sleet),
    "LIGHT_SNOW" to Sky("小雪", R.drawable.light_snow_o, R.drawable.light_snow_o),
    "MODERATE_SNOW" to Sky("中雪", R.drawable.moderate_snow, R.drawable.moderate_snow),
    "HEAVY_SNOW" to Sky("大雪", R.drawable.heavy_snow, R.drawable.heavy_snow),
    "STORM_SNOW" to Sky("暴雪", R.drawable.blizzard__1_, R.drawable.blizzard__1_),
    "HAIL" to Sky("冰雹", R.drawable.binbao, R.drawable.binbao),
    "LIGHT_HAZE" to Sky("轻度雾霾", R.drawable.light_haze, R.drawable.light_haze),
    "MODERATE_HAZE" to Sky("中度雾霾", R.drawable.zduwumai, R.drawable.zduwumai),
    "HEAVY_HAZE" to Sky("重度雾霾", R.drawable.zhongduwumai, R.drawable.zhongduwumai),
    "FOG" to Sky("雾", R.drawable.fog, R.drawable.fog),
    "DUST" to Sky("浮尘", R.drawable.dust, R.drawable.dust)
)
fun getSky(skycon: String): Sky {
    return sky[skycon] ?: sky["CLEAR_DAY"]!!
}