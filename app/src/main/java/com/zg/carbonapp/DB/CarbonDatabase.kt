package com.zg.carbonapp.DB

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zg.carbonapp.Dao.CarbonDao
import com.zg.carbonapp.Entity.CarbonAction
import com.zg.carbonapp.Entity.CarbonFootprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CarbonFootprint::class, CarbonAction::class],
    version = 2, // 增加版本号
    exportSchema = false
)
abstract class CarbonDatabase : RoomDatabase() {
    abstract fun carbonDao(): CarbonDao

    companion object {
        @Volatile
        private var INSTANCE: CarbonDatabase? = null
        private const val TAG = "CarbonDatabase"

        fun getDatabase(context: Context): CarbonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonDatabase::class.java,
                    "carbon_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d(TAG, "Database created, prepopulating data")
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    prepopulateData(database.carbonDao())
                                }
                            } ?: Log.e(TAG, "INSTANCE is null during onCreate")
                        }
                    })
                    .fallbackToDestructiveMigration() // 允许破坏性迁移
                    .build()

                INSTANCE = instance

                // 确保预填充执行
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateData(instance.carbonDao())
                }

                instance
            }
        }

        private suspend fun prepopulateData(dao: CarbonDao) {
            try {
                val barcode = "6921168560509"
                if (dao.getCarbonByBarcode(barcode) == null) {
                    Log.d(TAG, "Inserting prepopulated data for barcode: $barcode")
                    dao.insertCarbonFootprint(
                        CarbonFootprint(
                            barcode = barcode,
                            name = "农夫山泉550ml矿泉水",
                            carbonEmission = 0.18,
                            lifecycle = "塑料瓶生产→水处理→灌装→运输",
                            category = "包装类",
                            source = "农夫山泉2023年碳足迹报告",
                            suggestion = "喝完剪开当笔筒，重复使用3次可减碳0.1kg"
                        )
                    )

                    // 验证插入
                    val inserted = dao.getCarbonByBarcode(barcode)
                    if (inserted != null) {
                        Log.d(TAG, "Prepopulated data verified: ${inserted.name}")
                    } else {
                        Log.e(TAG, "Failed to verify prepopulated data")
                    }
                } else {
                    Log.d(TAG, "Prepopulated data already exists")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prepopulation failed: ${e.message}", e)
            }
        }
    }
}