package com.zg.carbonapp.Tool

import android.content.Context
import android.content.Intent

object IntentHelper {
    fun <T>goIntent(context : Context,aimActivity:Class<T>){
         val intent= Intent(context,aimActivity)
         context.startActivity(intent)

    }
}