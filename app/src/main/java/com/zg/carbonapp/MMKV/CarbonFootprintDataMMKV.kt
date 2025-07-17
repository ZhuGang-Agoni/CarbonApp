package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.tencent.mmkv.MMKV

object CarbonFootprintDataMMKV {
    // 初始化MMKV实例，使用专用ID确保数据隔离
    private val mmkv by lazy { MMKV.mmkvWithID("carbonfootprint_data") }

    /**
     * 按天保存步数到本地存储
     *
     * 存储说明：
     * - 键名：step_yyyy-MM-dd
     * - 值：步数Int值
     * - 自动覆盖同日期数据
     */
    fun saveStep(date: String, step: Int) {
        mmkv.encode("step_$date", step)
    }
    
    /**
     * 按天获取本地存储的步数
     *
     * 获取说明：
     * - 优先从本地缓存获取
     * - 不存在时返回默认值0
     */
    fun getStep(date: String): Int {
        return mmkv.decodeInt("step_$date", 0)
    }
    
    /**
     * 检查指定日期是否有步数记录
     *
     * 使用场景：
     * - 判断是否需要从服务器获取数据
     * - 检查本地缓存是否有效
     */
    fun hasStep(date: String): Boolean {
        return mmkv.containsKey("step_$date")
    }

    /**
     * 清空所有步数缓存（调试用）
     * 
     * 功能说明：
     * - 删除所有以"step_"开头的键值对
     * - 用于测试或重置数据
     * - 谨慎使用，会丢失所有本地步数数据
     */
    fun clearAllSteps() {
        // 获取所有键，过滤出步数相关的键，然后删除
        mmkv.allKeys()?.filter { it.startsWith("step_") }?.forEach { mmkv.remove(it) }
    }

    /**
     * 同步本地所有步数到后端（后端API待实现）
     * 
     * 功能说明：
     * - 遍历本地所有步数记录
     * - 调用后端API上传数据
     * - 用于数据备份和同步
     * 
     * 实现说明：
     * 1. 获取所有以"step_"开头的键
     * 2. 解析日期和步数
     * 3. 调用后端API上传数据
     * 4. 上传成功后可以删除本地数据（可选）
     */
    fun syncAllStepsToServer() {
        // 获取所有步数相关的键
        val allKeys = mmkv.allKeys()?.filter { it.startsWith("step_") } ?: return
        
        // 遍历所有步数记录
        for (key in allKeys) {
            val date = key.removePrefix("step_") // 提取日期部分
            val steps = mmkv.decodeInt(key, 0) // 获取步数
            
            // TODO: 调用后端API上传 (date, steps)
            // 例如: ApiService.uploadStep(date, steps)
            // 上传成功后可以删除本地数据：mmkv.remove(key)
        }
    }
} 