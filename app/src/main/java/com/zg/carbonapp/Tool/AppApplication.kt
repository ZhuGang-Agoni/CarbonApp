package com.zg.carbonapp.Tool

import com.tencent.mmkv.MMKV
import android.app.Application
import android.content.Intent
import com.amap.api.location.AMapLocationClient
import com.zg.carbonapp.Service.MusicService

class AppApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化MMKV
        MMKV.initialize(this)

        // 其他全局初始化代码（如网络库、数据库等）
        // 🔥 必须！高德隐私合规（全局只调用一次）
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        // 🔥 设置API Key（替换成你在高德平台申请的Key）
        AMapLocationClient.setApiKey("77760b774a262e67ef6ea8ce75a6701d")
        startService(Intent(this, MusicService::class.java))
    }
}