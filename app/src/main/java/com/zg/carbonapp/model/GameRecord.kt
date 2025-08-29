package com.zg.carbonapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class GameRecord(
    val id: Long = System.currentTimeMillis(),
    val score: Int,
    val playerType: String,
    val date: Date,
    val playerAvatar: Int
) : Parcelable