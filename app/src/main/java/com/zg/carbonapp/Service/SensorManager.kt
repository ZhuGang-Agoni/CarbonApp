package com.zg.carbonapp.Service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.zg.carbonapp.MMKV.CarbonFootprintDataMMKV
import java.text.SimpleDateFormat
import java.util.*

/**
 * SensorManager
 * 统一管理本地步数传感器和Google Fit步数数据
 * 支持实时监听、历史步数获取、数据自动存取
 */
class SensorManager(private val context: Context) {
    companion object {
        private const val TAG = "SensorManager"
    }
    // Android系统的传感器服务
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    // Google Fit服务，用于优先获取更准确的步数
    private val googleFitService = GoogleFitService(context)
    // 步数传感器对象
    private var stepSensor: Sensor? = null
    // 当前步数（从传感器获取的原始值）
    private var stepCount = 0
    // 上次记录的步数（用于计算当天步数）
    private var lastStepCount = 0
    // 是否已初始化传感器
    private var isInitialized = false
    // 步数变化回调（用于实时UI刷新）
    private var onStepChanged: ((Int) -> Unit)? = null

    /**
     * 设置步数变化监听器（UI可注册此回调实现实时刷新）
     * @param listener 步数变化回调，参数为当天步数
     */
    fun setOnStepChangedListener(listener: (Int) -> Unit) {
        onStepChanged = listener
    }

    /**
     * 步数传感器监听器，实时监听步数变化
     * 步数变化时自动保存到本地缓存，并通知UI
     */
    private val stepSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val newStepCount = it.values[0].toInt()
                    
                    // 初始化时记录当前步数作为基准
                    if (!isInitialized) {
                        lastStepCount = newStepCount
                        stepCount = newStepCount
                        isInitialized = true
                        Log.d(TAG, "传感器初始化完成，基准步数: $stepCount")
                    } else {
                        stepCount = newStepCount
                    }
                    
                    Log.d(TAG, "步数传感器更新: $stepCount")
                    // 计算今天步数
                    val todaySteps = stepCount - lastStepCount
                    // 自动保存到本地MMKV缓存
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    CarbonFootprintDataMMKV.saveStep(today, if (todaySteps > 0) todaySteps else stepCount)
                    // 实时通知UI
                    onStepChanged?.invoke(if (todaySteps > 0) todaySteps else stepCount)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 精度变化处理（可选实现）
        }
    }
    
    /**
     * 初始化本地步数传感器（如有）
     * 注册监听器，开始监听步数变化
     */
    fun initializeSensors() {
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(
                stepSensorListener,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "步数传感器注册成功")
        } else {
            Log.w(TAG, "设备不支持步数传感器")
        }
    }
    
    /**
     * 获取今天的步数（优先Google Fit，其次本地传感器）
     * @param callback 回调函数，返回步数Int
     */
    fun getTodaySteps(callback: (Int) -> Unit) {
        if (googleFitService.isSignedIn()) {
            // 优先使用Google Fit
            googleFitService.getTodaySteps { steps ->
                if (steps > 0) {
                    // 自动保存Google Fit步数到本地缓存
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    CarbonFootprintDataMMKV.saveStep(today, steps)
                    callback(steps)
                } else {
                    // Google Fit没有数据，使用本地传感器
                    getLocalSteps(callback)
                }
            }
        } else {
            // 未登录Google Fit，使用本地传感器
            getLocalSteps(callback)
        }
    }
    
    /**
     * 获取指定日期的步数（Google Fit支持历史，本地传感器仅支持当天）
     * @param date 日期字符串，格式yyyy-MM-dd
     * @param callback 回调函数，返回步数Int
     */
    fun getStepsForDate(date: String, callback: (Int) -> Unit) {
        if (googleFitService.isSignedIn()) {
            googleFitService.getStepsForDate(date) { steps ->
                // 自动保存Google Fit步数到本地缓存
                CarbonFootprintDataMMKV.saveStep(date, steps)
                callback(steps)
            }
        } else {
            // 本地传感器只能获取今天的实时数据
            if (isToday(date)) {
                getLocalSteps(callback)
            } else {
                callback(0)
            }
        }
    }
    
    /**
     * 获取本地传感器的步数（仅支持当天）
     * @param callback 回调函数，返回步数Int
     */
    private fun getLocalSteps(callback: (Int) -> Unit) {
        if (stepSensor != null && isInitialized) {
            // 计算今天的步数（总步数减去上次记录的步数）
            val todaySteps = stepCount - lastStepCount
            callback(if (todaySteps > 0) todaySteps else stepCount)
        } else {
            Log.w(TAG, "本地传感器不可用或未初始化")
            callback(0)
        }
    }
    
    /**
     * 判断传入日期是否为今天
     * @param date 日期字符串
     * @return 是否为今天
     */
    private fun isToday(date: String): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return date == today
    }
    
    /**
     * 获取Google Fit服务对象
     */
    fun getGoogleFitService(): GoogleFitService {
        return googleFitService
    }
    
    /**
     * 检查设备是否支持步数传感器
     */
    fun isStepSensorAvailable(): Boolean {
        return stepSensor != null
    }
    
    /**
     * 检查是否已登录Google Fit
     */
    fun isGoogleFitSignedIn(): Boolean {
        return googleFitService.isSignedIn()
    }
    
    /**
     * 释放传感器资源，防止内存泄漏
     */
    fun releaseSensors() {
        if (stepSensor != null) {
            sensorManager.unregisterListener(stepSensorListener, stepSensor)
        }
    }
    
    /**
     * 更新步数记录（用于计算每日步数，通常在每天0点调用）
     */
    fun updateStepRecord() {
        lastStepCount = stepCount
        Log.d(TAG, "更新步数记录: lastStepCount = $lastStepCount")
    }
    
    /**
     * 重置传感器初始化状态（用于重新初始化）
     */
    fun resetInitialization() {
        isInitialized = false
        lastStepCount = 0
        stepCount = 0
        Log.d(TAG, "重置传感器初始化状态")
    }
} 