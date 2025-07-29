package com.zg.carbonapp.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zg.carbonapp.Entity.CarbonAction
import com.zg.carbonapp.Entity.CarbonFootprint
import kotlinx.coroutines.flow.Flow

@Dao
interface CarbonDao {
    // ===================== CarbonFootprint 操作 =====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarbonFootprint(footprint: CarbonFootprint)

    @Update
    suspend fun updateCarbonFootprint(footprint: CarbonFootprint)

    // 单个对象查询：suspend + 返回具体类型
    @Query("SELECT * FROM carbon_footprint WHERE barcode = :barcode")
    suspend fun getCarbonByBarcode(barcode: String): CarbonFootprint?

    @Query("SELECT * FROM carbon_footprint WHERE name LIKE :name LIMIT 1")
    suspend fun searchCarbonByName(name: String): CarbonFootprint?

    // ===================== CarbonAction 操作 =====================
    @Insert
    suspend fun recordCarbonAction(action: CarbonAction)

    // 实时数据：Flow + 列表
    @Query("SELECT * FROM carbon_action ORDER BY actionTime DESC")
    fun getAllCarbonActions(): Flow<List<CarbonAction>>

    // 聚合查询：suspend + 可空Double
    @Query("SELECT SUM(reducedCarbon) FROM carbon_action")
    suspend fun getTotalReducedCarbon(): Double?
}