package com.zg.carbonapp.model

import android.os.Parcel
import android.os.Parcelable
import androidx.versionedparcelable.ParcelField
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
    val name: String,
    val type: GarbageType,
    val imageRes: Int // 头像等资源id
) : Parcelable
