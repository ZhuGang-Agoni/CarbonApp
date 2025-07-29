package com.zg.carbonapp.DB

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zg.carbonapp.Entity.CarbonFootprint
import com.zg.carbonapp.Dao.CarbonDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrepopulateCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

    }

    // 添加一个方法来预填充数据，避免在回调中直接访问数据库
    fun prepopulateData(dao: CarbonDao) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertCarbonFootprint(
                CarbonFootprint(
                    barcode = "6921168560509",
                    name = "农夫山泉550ml矿泉水",
                    carbonEmission = 0.18,
                    lifecycle = "塑料瓶生产→水处理→灌装→运输",
                    category = "包装类",
                    source = "农夫山泉2023年碳足迹报告",
                    suggestion = "喝完剪开当笔筒，重复使用3次可减碳0.1kg"
                )
            )
        }
    }
}