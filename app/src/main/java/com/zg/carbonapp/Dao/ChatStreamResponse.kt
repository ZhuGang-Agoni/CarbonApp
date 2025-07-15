package com.zg.carbonapp.Dao

import com.google.gson.annotations.SerializedName

data class ChatStreamResponse(
    val id: String,
    @SerializedName("object") val objectType: String, // 映射JSON的"object"字段
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
) {
    data class StreamChoice(
        val delta: StreamDelta,
        val index: Int,
        @SerializedName("finish_reason") val finishReason: String?
    ) {
        data class StreamDelta(
            val content: String?
        )
    }
}
