package com.zg.carbonapp.Dao

import com.google.gson.annotations.SerializedName

data class ChatResponse(val id: String,
                       @SerializedName("object") val objectType: String,
                        val created: Long,
                        val choices: List<Choice>)
