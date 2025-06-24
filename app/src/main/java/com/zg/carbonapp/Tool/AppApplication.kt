package com.zg.carbonapp.Tool

import com.tencent.mmkv.MMKV
import android.app.Application

class AppApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化MMKV
        MMKV.initialize(this)
        // 其他全局初始化代码（如网络库、数据库等）
    }
}