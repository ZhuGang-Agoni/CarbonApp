package com.zg.carbonapp.Dao

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

// 单日步数与碳减排数据
data class DailyStepData(
    val date: String, // yyyy-MM-dd
    var steps: Int = 0,
    var carbonReduction: Float = 0f, // 单位：kg
    var carbonPoints: Int = 0 // 碳积分
) : Parcelable, Serializable {

    // 计算碳减排和积分（1000步=1公里=0.17kg减排=17积分）
    fun calculateCarbon() {
        val km = steps / 1000f
        carbonReduction = km * 0.17f
        carbonPoints = (carbonReduction * 100).toInt() // 1kg=100积分
    }

    // 获取格式化日期（MM-dd）
    fun getShortDate(): String {
        return SimpleDateFormat("MM-dd", Locale.getDefault()).format(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date()
        )
    }



    // Parcelable 接口实现
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(date)
        parcel.writeInt(steps)
        parcel.writeFloat(carbonReduction)
        parcel.writeInt(carbonPoints)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DailyStepData> {
        override fun createFromParcel(parcel: Parcel): DailyStepData {
            return DailyStepData(parcel)
        }

        override fun newArray(size: Int): Array<DailyStepData?> {
            return arrayOfNulls(size)
        }
    }
}

// 低碳区域数据
data class LowCarbonArea(
    val id: String,
    val name: String, // 如"城市绿道"、"无车区"
    val lat: Double,
    val lng: Double,
    val description: String
)