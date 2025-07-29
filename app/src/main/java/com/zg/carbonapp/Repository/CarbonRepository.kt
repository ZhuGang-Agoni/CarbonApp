package com.zg.carbonapp.Repository

import android.util.Log
import com.zg.carbonapp.DB.CarbonDatabase
import com.zg.carbonapp.Entity.CarbonAction
import com.zg.carbonapp.Entity.CarbonFootprint
import com.zg.carbonapp.Service.RetrofitClient
import com.zg.carbonapp.Tool.AppApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class CarbonRepository {
    private val dao = CarbonDatabase.getDatabase(AppApplication.context).carbonDao()
    private val remoteService = RetrofitClient.instance
    private val footprintCache= mutableMapOf<String,CarbonFootprint>()
    private val TAG="CarbonRepository"

    suspend fun getCarbonFootprint(barcode: String): CarbonFootprint? {
        return try {
            // 1. 尝试从本地数据库获取
            var carbon = withContext(Dispatchers.IO) {
                dao.getCarbonByBarcode(barcode).also {
                    if (it != null) Log.d(TAG, "Found in DB: $barcode - ${it.name}")
                }
            }

            if (carbon != null) return carbon

            // 2. 从网络获取
            Log.d(TAG, "Querying remote API for barcode: $barcode")
            val response = remoteService.queryCarbonByBarcode(barcode)

            if (response.isSuccessful) {
                carbon = response.body()
                if (carbon != null) {
                    Log.d(TAG, "API response success: ${carbon.name}")

                    // 3. 保存到数据库
                    withContext(Dispatchers.IO) {
                        dao.insertCarbonFootprint(carbon)
                        Log.d(TAG, "Saved to DB: ${carbon.barcode}")
                    }
                    carbon
                } else {
                    Log.w(TAG, "API returned null body")
                    null
                }
            } else {
                Log.w(TAG, "API error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCarbonFootprint error: ${e.message}", e)
            null
        }
    }

    suspend fun searchCarbonByName(name: String): CarbonFootprint? {
        return try {
            // 1. 尝试从本地数据库获取
            var carbon = withContext(Dispatchers.IO) {
                dao.searchCarbonByName(name).also {
                    if (it != null) Log.d(TAG, "Found in DB: $name - ${it.name}")
                }
            }

            if (carbon != null) return carbon

            // 2. 从网络获取
            Log.d(TAG, "Querying remote API for name: $name")
            val response = remoteService.searchCarbonByName(name)

            if (response.isSuccessful) {
                carbon = response.body()
                if (carbon != null) {
                    Log.d(TAG, "API response success: ${carbon.name}")

                    // 3. 保存到数据库
                    withContext(Dispatchers.IO) {
                        dao.insertCarbonFootprint(carbon)
                        Log.d(TAG, "Saved to DB: ${carbon.barcode}")
                    }
                    carbon
                } else {
                    Log.w(TAG, "API returned null body")
                    null
                }
            } else {
                Log.w(TAG, "API error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchCarbonByName error: ${e.message}", e)
            null
        }
    }



    suspend fun searchCarbonFootprint(name: String): CarbonFootprint? {
        return withContext(Dispatchers.IO) {
            var carbon = dao.searchCarbonByName("%$name%")
            if (carbon != null) return@withContext carbon

            try {
                val response = remoteService.searchCarbonByName(name)
                if (response.isSuccessful) {
                    val remoteCarbon = response.body()
                    remoteCarbon?.let { dao.insertCarbonFootprint(it) }
                    remoteCarbon
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun recordCarbonAction(action: CarbonAction) {
        withContext(Dispatchers.IO) {
            dao.recordCarbonAction(action)
        }
    }

    fun getAllCarbonActions(): Flow<List<CarbonAction>> {
        return dao.getAllCarbonActions()
    }

    suspend fun getTotalReducedCarbon(): Double {
        return withContext(Dispatchers.IO) {
            dao.getTotalReducedCarbon() ?: 0.0 // 可空Double转非空
        }
    }
}