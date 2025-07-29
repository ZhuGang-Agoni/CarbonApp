package com.zg.carbonapp.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carbon_action")
data class CarbonAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val action: String,
    val reducedCarbon: Double,
    val actionTime: Long = System.currentTimeMillis()
)