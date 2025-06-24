package com.zg.carbonapp.Dao

data class ChatResponse(val id: String,
                        val objectType: String,
                        val created: Long,
                        val choices: List<Choice>)
