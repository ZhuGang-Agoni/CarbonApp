package com.zg.carbonapp.logic.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceCreator {

    private const val BASE_url="https://api.caiyunapp.com/"

    private val retrofit=Retrofit.Builder()
        .baseUrl(BASE_url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
//    有参的构造
    fun <T>create(serviceClass:Class<T>):T= retrofit.create(serviceClass)
//    无参的构造
    inline fun <reified T> create():T=create(T::class.java)

}