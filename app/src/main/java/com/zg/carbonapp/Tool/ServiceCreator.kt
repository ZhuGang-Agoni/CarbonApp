package com.zg.carbonapp.Tool

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create


object ServiceCreator {
    private const val baseUrl="http://127.0.0.1/"//笔者这里写的地址是自己的本机地址

    private val retrofit= Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
//有参构造
    fun <T> create(serviceClass:Class<T>):T= retrofit.create(serviceClass)
//无参构造
     inline fun <reified T> create():T= create(T::class.java)

}