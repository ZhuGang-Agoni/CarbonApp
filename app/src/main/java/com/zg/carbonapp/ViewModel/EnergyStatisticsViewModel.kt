package com.zg.carbonapp.ViewModel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zg.carbonapp.Dao.EnvData
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.Tool.TorchModelHelper
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnergyStatisticsViewModel : ViewModel() {
    // 环境数据（对外暴露不可变LiveData）
    private val _envData = MutableLiveData<EnvData>()
    val envData: LiveData<EnvData> = _envData

    // AI节能建议
    private val _aiSuggestion = MutableLiveData<String>()
    val aiSuggestion: LiveData<String> = _aiSuggestion

    // 数据更新提示
    private val _updateNotification = MutableLiveData<String?>()
    val updateNotification: LiveData<String?> = _updateNotification

    // DeepSeek助手
    private val deepSeekHelper = DeepSeekHelper()

    // 传感器和模型相关
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null       // 光线传感器
    private var pressureSensor: Sensor? = null    // 气压传感器
    private lateinit var torchModel: TorchModelHelper
    private var isModelInitialized = false        // 模型是否初始化成功
    private var isGeneratingSuggestion = false    // 避免重复生成建议

    // 温度变化率计算变量
    private var lastPredictedTemp = 0f
    private var lastTempUpdateTime = 0L
    private var currentTempChangeRate = 0f

    // 用于判断是否需要发送更新提示
    private var isFirstUpdate = true

    // 初始化模型（在IO线程执行，避免阻塞UI）
    fun initModel(context: Context) {
        torchModel = TorchModelHelper(context)
        CoroutineScope(Dispatchers.IO).launch {
            isModelInitialized = torchModel.init()
            withContext(Dispatchers.Main) {
                if (!isModelInitialized) {
                    _aiSuggestion.value = "模型初始化失败，请检查assets中的模型文件"
                } else {
                    _aiSuggestion.value = "模型准备就绪，点击生成建议获取节能方案"
                    // 初始化成功后加载电池数据
                    loadBatteryData(context)
                }
            }
        }
    }

    // 启动传感器监听
    fun startSensors(context: Context) {
        if (!::sensorManager.isInitialized) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }

        // 注册光线传感器
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL  // 约200ms一次更新
            )
        }

        // 注册气压传感器
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        pressureSensor?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    // 停止传感器（生命周期管理）
    fun stopSensors() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // 加载电池数据并更新环境数据
    fun loadBatteryData(context: Context) {
        if (!isModelInitialized) return  // 模型未初始化则不执行

        // 获取电池状态广播
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return

        // 解析电池数据
        val batteryTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) / 10f  // 转为℃
        val batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 50)             // 电量%
        val isCharging = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0         // 是否充电

        // 当前环境数据（首次加载用默认值）
        val currentData = _envData.value ?: EnvData(
            batteryTemp = 30f,
            batteryLevel = 50,
            isCharging = false,
            lightLevel = 500f,
            tempChangeRate = 0f,
            isNight = false,
            predictedTemp = 0f,  // 初始默认值设为0
            predictedHumidity = 0f,  // 初始默认值设为0
            isAirconOn = false,
            pressure = 1013.25f
        )

        // 复用当前光线强度和气压（传感器可能尚未更新）
        val lightLevel = currentData.lightLevel
        val pressure = currentData.pressure

        // 判断是否夜间（21:00-6:00）
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 21 || hour <= 6

        // 调用模型预测温度和湿度（预测失败返回0）
        val predictionResult = torchModel.predict(
            batteryTemp = batteryTemp,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            lightLevel = lightLevel,
            tempChangeRate = currentData.tempChangeRate,
            isNight = isNight,
            pressure = pressure
        )

        // 处理预测结果（失败则设为0）
        val (predictedTemp, predictedHumidity) = if (predictionResult != null) {
            predictionResult
        } else {
            Log.e("预测失败", "loadBatteryData中模型预测返回null，使用0值")
            Pair(0f, 0f)
        }

        // 计算温度变化率（℃/min）
        calculateTempChangeRate(predictedTemp)

        // 判断空调状态
        val isAirconOn = judgeAirconStatus(lightLevel, predictedTemp, currentTempChangeRate, isNight)

        // 创建新的环境数据对象
        val newEnvData = EnvData(
            batteryTemp = batteryTemp,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            lightLevel = lightLevel,
            tempChangeRate = currentTempChangeRate,
            isNight = isNight,
            predictedTemp = predictedTemp,
            predictedHumidity = predictedHumidity,
            isAirconOn = isAirconOn,
            pressure = pressure
        )

        // 更新环境数据
        _envData.value = newEnvData

        // 发送更新提示（首次更新不提示）
        if (!isFirstUpdate && hasSignificantChange(currentData, newEnvData)) {
            _updateNotification.value = "环境数据已更新，已为您预测出新值"
        } else {
            isFirstUpdate = false
        }
    }

    // 判断数据是否有显著变化
    private fun hasSignificantChange(oldData: EnvData, newData: EnvData): Boolean {
        return Math.abs(oldData.predictedTemp - newData.predictedTemp) > 1.0f ||
                Math.abs(oldData.predictedHumidity - newData.predictedHumidity) > 5.0f ||
                oldData.batteryLevel != newData.batteryLevel ||
                oldData.isCharging != newData.isCharging ||
                Math.abs(oldData.lightLevel - newData.lightLevel) > 100
    }

    // 清除通知
    fun clearNotification() {
        _updateNotification.value = null
    }

    // 计算温度变化率
    private fun calculateTempChangeRate(newTemp: Float) {
        // 预测失败（温度为0）时不更新变化率
        if (newTemp == 0f) {
            currentTempChangeRate = 0f
            return
        }

        val currentTime = SystemClock.elapsedRealtime()  // 毫秒级时间
        if (lastPredictedTemp != 0f && lastTempUpdateTime != 0L) {
            val timeDiffMinutes = (currentTime - lastTempUpdateTime) / (1000f * 60f)  // 转换为分钟
            if (timeDiffMinutes > 0.1f) {  // 至少间隔6秒才计算（避免波动）
                currentTempChangeRate = (newTemp - lastPredictedTemp) / timeDiffMinutes
                currentTempChangeRate = currentTempChangeRate.coerceIn(-2f, 2f)  // 限制范围
            }
        }
        // 更新历史数据
        lastPredictedTemp = newTemp
        lastTempUpdateTime = currentTime
    }

    // 判断空调是否开启
    private fun judgeAirconStatus(
        lightLevel: Float,
        predictedTemp: Float,
        tempChangeRate: Float,
        isNight: Boolean
    ): Boolean {
        // 预测失败时默认空调关闭
        if (predictedTemp == 0f) {
            return false
        }

        return when {
            // 快速降温且温度低 → 空调开启
            tempChangeRate <= -0.3f && predictedTemp <= 26f -> true
            // 夜间低温 → 空调可能开启
            isNight && predictedTemp <= 24f -> true
            // 光线暗（室内）且温度低 → 空调可能开启
            lightLevel < 300f && predictedTemp <= 25f -> true
            else -> false
        }
    }

    // 传感器监听（处理光线和气压变化）
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_LIGHT -> handleLightChange(event.values[0])  // 光线强度变化
                Sensor.TYPE_PRESSURE -> handlePressureChange(event.values[0])  // 气压变化
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}  // 忽略精度变化
    }

    // 处理光线强度变化
    private fun handleLightChange(newLightLevel: Float) {
        val currentData = _envData.value ?: return  // 无数据则返回

        // 重新判断是否夜间
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 21 || hour <= 6

        // 模型预测
        val predictionResult = if (isModelInitialized) {
            torchModel.predict(
                batteryTemp = currentData.batteryTemp,
                batteryLevel = currentData.batteryLevel,
                isCharging = currentData.isCharging,
                lightLevel = newLightLevel,
                tempChangeRate = currentData.tempChangeRate,
                isNight = isNight,
                pressure = currentData.pressure
            )
        } else {
            null
        }

        // 处理预测结果（失败则设为0）
        val (predictedTemp, predictedHumidity) = if (predictionResult != null) {
            predictionResult
        } else {
            Log.e("预测失败", "handleLightChange中模型预测返回null，使用0值")
            Pair(0f, 0f)
        }

        // 更新温度变化率
        calculateTempChangeRate(predictedTemp)

        // 更新空调状态
        val isAirconOn = judgeAirconStatus(newLightLevel, predictedTemp, currentTempChangeRate, isNight)

        // 创建新的环境数据对象
        val newEnvData = currentData.copy(
            lightLevel = newLightLevel,
            isNight = isNight,
            predictedTemp = predictedTemp,
            predictedHumidity = predictedHumidity,
            tempChangeRate = currentTempChangeRate,
            isAirconOn = isAirconOn
        )

        // 更新环境数据
        _envData.value = newEnvData

        // 发送更新提示
        if (hasSignificantChange(currentData, newEnvData)) {
            _updateNotification.value = "光线数据变化，已为您预测出新值"
        }
    }

    // 处理气压变化
    private fun handlePressureChange(newPressure: Float) {
        val currentData = _envData.value ?: return

        // 重新判断是否夜间
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 21 || hour <= 6

        // 模型预测
        val predictionResult = if (isModelInitialized) {
            torchModel.predict(
                batteryTemp = currentData.batteryTemp,
                batteryLevel = currentData.batteryLevel,
                isCharging = currentData.isCharging,
                lightLevel = currentData.lightLevel,
                tempChangeRate = currentData.tempChangeRate,
                isNight = isNight,
                pressure = newPressure
            )
        } else {
            null
        }

        // 处理预测结果（失败则设为0）
        val (predictedTemp, predictedHumidity) = if (predictionResult != null) {
            predictionResult
        } else {
            Log.e("预测失败", "handlePressureChange中模型预测返回null，使用0值")
            Pair(0f, 0f)
        }

        // 更新温度变化率
        calculateTempChangeRate(predictedTemp)

        // 更新空调状态
        val isAirconOn = judgeAirconStatus(currentData.lightLevel, predictedTemp, currentTempChangeRate, isNight)

        // 创建新的环境数据对象
        val newEnvData = currentData.copy(
            pressure = newPressure,
            isNight = isNight,
            predictedTemp = predictedTemp,
            predictedHumidity = predictedHumidity,
            tempChangeRate = currentTempChangeRate,
            isAirconOn = isAirconOn
        )

        // 更新环境数据
        _envData.value = newEnvData

        // 发送更新提示
        if (hasSignificantChange(currentData, newEnvData)) {
            _updateNotification.value = "气压数据变化，已为您预测出新值"
        }
    }

    // 生成节能建议（使用DeepSeek API）
    fun generateSuggestion() {
        if (isGeneratingSuggestion || _envData.value == null) return
        isGeneratingSuggestion = true
        _aiSuggestion.value = "正在分析节能建议..."

        val data = _envData.value!!

        // 构建发送给DeepSeek的提示信息
        val prompt = buildString {
            append("根据以下环境和设备数据，给出简洁的节能建议：\n")
            append("1. 电池状态：温度 ${String.format("%.1f", data.batteryTemp)}℃，电量 ${data.batteryLevel}%，${if (data.isCharging) "正在充电" else "未充电"}\n")
            append("2. 环境数据：预测温度 ${String.format("%.1f", data.predictedTemp)}℃，预测湿度 ${String.format("%.1f", data.predictedHumidity)}%\n")
            append("3. 其他信息：光线强度 ${data.lightLevel.toInt()}lx，${if (data.isNight) "夜间" else "日间"}，${if (data.isAirconOn) "空调已开启" else "空调未开启"}\n")
            append("请给出3-4条实用的节能建议，每条建议详细点。")
        }

        // 使用DeepSeek API生成建议（流式返回）
        deepSeekHelper.sendMessageStream(
            prompt = prompt,
            charDelay = 30,
            onChar = { char ->
                val currentText = _aiSuggestion.value ?: ""
                _aiSuggestion.value = currentText + char
            },
            onComplete = {
                isGeneratingSuggestion = false
                // 清除更新提示
                clearNotification()
            },
            onError = { error ->
                _aiSuggestion.value = "获取建议失败：$error"
                isGeneratingSuggestion = false
            }
        )
    }

    // 清理资源（ViewModel销毁时调用）
    override fun onCleared() {
        super.onCleared()
        stopSensors()
        if (::torchModel.isInitialized) {
            torchModel.destroy()  // 释放模型资源
        }
    }
}
