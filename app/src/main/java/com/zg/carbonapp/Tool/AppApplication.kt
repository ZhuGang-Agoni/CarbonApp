package com.zg.carbonapp.Tool

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.amap.api.location.AMapLocationClient
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.jakewharton.threetenabp.AndroidThreeTen
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.DB.CarbonDatabase
import com.zg.carbonapp.DB.PrepopulateCallback
import com.zg.carbonapp.MMKV.RecordManager
import com.zg.carbonapp.Service.MusicService
import com.zg.carbonapp.Service.NotificationScheduler

class AppApplication : Application(), Configuration.Provider {

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var context :Context
        const val TOKEN="SJrPcwY43EaLolw0"
    }





    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        RecordManager.initialize(this)
        // 初始化MMKV
        MMKV.initialize(this)
        AndroidThreeTen.init(this) // 初始化 ThreeTenBP
        // 初始化WorkManager
        initializeWorkManager()

        // 初始化通知调度
        NotificationScheduler.scheduleWeeklyNotification(this)

        // 高德地图隐私合规及初始化
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        AMapLocationClient.setApiKey("77760b774a262e67ef6ea8ce75a6701d")

        // 启动音乐服务
        startService(Intent(this, MusicService::class.java))

        // 百度地图初始化（关键修复：先同意隐私政策）
        SDKInitializer.setAgreePrivacy(this, true)  // 必须在initialize之前调用
        SDKInitializer.initialize(this)
        SDKInitializer.setCoordType(CoordType.BD09LL)  // 设置坐标类型

        // 数据库初始化
        val db = CarbonDatabase.getDatabase(this)
        val callback = PrepopulateCallback()
        callback.prepopulateData(db.carbonDao())
    }

    private fun initializeWorkManager() {
        try {
            // 手动初始化 WorkManager
            WorkManager.initialize(this, workManagerConfiguration)
            Log.d("AppApplication", "WorkManager initialized successfully")
        } catch (e: IllegalStateException) {
            // 如果已经初始化，忽略异常
            Log.d("AppApplication", "WorkManager already initialized")
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
}