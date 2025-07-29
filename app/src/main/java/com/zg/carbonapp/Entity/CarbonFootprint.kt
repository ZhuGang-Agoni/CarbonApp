package com.zg.carbonapp.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "carbon_footprint")
data class CarbonFootprint(
    @PrimaryKey val barcode: String,
    val name: String,
    val carbonEmission: Double,
    val lifecycle: String,
    val category: String,
    val source: String,
    val suggestion: String,
    val updateTime: Long = System.currentTimeMillis()
) : Serializable