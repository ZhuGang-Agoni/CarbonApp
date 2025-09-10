# 碳足迹代码逻辑流程文档

## 📋 概述

本文档详细描述了碳足迹应用的核心逻辑流程，包括数据获取、实时更新、计算逻辑、存储策略等各个环节。

## 🏗️ 系统架构

### 核心组件
- **SensorManager**: 统一管理所有数据源
- **GoogleFitService**: Google Fit API服务
- **CarbonFootprintDataMMKV**: 本地数据缓存
- **CarbonFootprintActivity**: 主界面
- **PlantTreeActivity**: 种树界面
- **FootprintActivity**: 详细统计界面

### 数据源优先级
1. 本地MMKV缓存 (最快)
2. 服务器API (网络)
3. Google Fit API (准确)
4. 本地传感器 (实时)

## 📊 数据获取流程

### 1. 应用启动流程

```
用户打开应用
    ↓
初始化传感器管理器 (SensorManager)
    ↓
检查本地缓存 (CarbonFootprintDataMMKV)
    ↓
有缓存数据？ → 是 → 显示缓存数据
    ↓ 否
检查服务器数据 (fetchStepFromServer)
    ↓
有服务器数据？ → 是 → 保存到缓存 → 显示数据
    ↓ 否
获取传感器数据 (Google Fit优先)
    ↓
保存到缓存 → 显示数据
```

### 2. 传感器初始化流程

```
SensorManager.initializeSensors()
    ↓
获取设备步数传感器 (Sensor.TYPE_STEP_COUNTER)
    ↓
传感器可用？ → 是 → 注册监听器
    ↓ 否
记录警告日志
    ↓
设置初始化标志 isInitialized = false
    ↓
等待传感器数据到达
    ↓
首次数据到达时设置基准值
    ↓
isInitialized = true
```

### 3. 实时数据更新流程

```
用户走路
    ↓
传感器检测到步数变化
    ↓
触发 stepSensorListener.onSensorChanged()
    ↓
检查是否已初始化
    ↓
未初始化？ → 是 → 设置基准值并初始化
    ↓ 否
更新当前步数 stepCount
    ↓
计算今日步数 = stepCount - lastStepCount
    ↓
保存到MMKV缓存
    ↓
调用 onStepChanged 回调
    ↓
重新计算本周总步数
    ↓
更新UI显示
```

## 🔄 实时更新机制

### 1. 监听器设置

```kotlin
// 在Activity中设置监听器
sensorManager.setOnStepChangedListener { newTodaySteps ->
    todaySteps = newTodaySteps
    // 重新异步统计一周步数
    getWeekSteps(weekDates) { total ->
        weekSteps = total
        updateUI(todaySteps, weekSteps)
    }
}
```

### 2. 传感器监听器

```kotlin
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
                } else {
                    stepCount = newStepCount
                }
                
                // 计算今天步数
                val todaySteps = stepCount - lastStepCount
                
                // 自动保存到本地MMKV缓存
                val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
                CarbonFootprintDataMMKV.saveStep(today, if (todaySteps > 0) todaySteps else stepCount)
                
                // 实时通知UI
                onStepChanged?.invoke(if (todaySteps > 0) todaySteps else stepCount)
            }
        }
    }
}
```

## 🧮 计算逻辑流程

### 1. 碳足迹计算

```
获取本周总步数
    ↓
计算碳吸收量 = 步数 × 0.00004g/步
    ↓
计算可种树数量 = 碳吸收量 ÷ 0.5g/棵
    ↓
更新界面显示
```

### 2. 计算代码实现

```kotlin
// 统一的计算逻辑
val carbonReduction = weekSteps * 0.00004 // 克(g)
val treeCount = (carbonReduction / 0.5).toInt()

// 显示格式
binding.tvStepCount.text = "$todaySteps 步"
binding.tvTreeCount.text = "$treeCount 棵"
```

### 3. 计算参数说明

- **每步碳减少量**: 0.00004克 (g)
- **每棵树碳吸收量**: 0.5克 (g)
- **单位**: 统一使用克(g)，避免单位混乱

## 💾 数据存储策略

### 1. MMKV缓存结构

```kotlin
// 键名格式: step_yyyy-MM-dd
// 值类型: Int (步数)
// 存储位置: MMKV专用存储空间

// 保存步数
fun saveStep(date: String, step: Int) {
    mmkv.encode("step_$date", step)
}

// 获取步数
fun getStep(date: String): Int {
    return mmkv.decodeInt("step_$date", 0)
}
```

### 2. 数据获取策略

```kotlin
private fun loadTodaySteps(today: String) {
    // 1. 检查本地缓存
    if (CarbonFootprintDataMMKV.hasStep(today)) {
        todaySteps = CarbonFootprintDataMMKV.getStep(today)
        updateUI(todaySteps, weekSteps)
    } else {
        // 2. 尝试从服务器获取
        val stepFromServer = fetchStepFromServer(today)
        if (stepFromServer > 0) {
            CarbonFootprintDataMMKV.saveStep(today, stepFromServer)
            todaySteps = stepFromServer
            updateUI(todaySteps, weekSteps)
        } else {
            // 3. 从传感器获取
            sensorManager.getTodaySteps { localStep ->
                CarbonFootprintDataMMKV.saveStep(today, localStep)
                todaySteps = localStep
                updateUI(todaySteps, weekSteps)
            }
        }
    }
}
```

## 📱 界面更新流程

### 1. UI更新触发点

- 应用启动时
- 传感器数据变化时
- 手动刷新时
- 页面切换时

### 2. 更新内容

```kotlin
private fun updateUI(todaySteps: Int, weekSteps: Int) {
    runOnUiThread {
        // 计算树木数量
        val carbonReduction = weekSteps * 0.00004
        val treeCount = (carbonReduction / 0.5).toInt()
        
        // 更新步数显示
        binding.tvStepCount.text = "$todaySteps 步"
        binding.tvTreeCount.text = "$treeCount 棵"
        
        // 设置点击事件
        binding.tvStepCount.setOnClickListener {
            val intent = Intent(this, FootprintActivity::class.java)
            startActivity(intent)
        }
        
        binding.tvTreeCount.setOnClickListener {
            val intent = Intent(this, PlantTreeActivity::class.java)
            startActivity(intent)
        }
        
        // 更新排行榜
        updateRankingList()
    }
}
```

## 🔧 错误处理机制

### 1. 传感器错误处理

```kotlin
// 检查传感器可用性
if (stepSensor != null && isInitialized) {
    // 正常处理
} else {
    Log.w(TAG, "本地传感器不可用或未初始化")
    callback(0)
}
```

### 2. 网络错误处理

```kotlin
// 服务器数据获取失败时的降级策略
private fun fetchStepFromServer(date: String): Int {
    return try {
        // TODO: 实现真实API调用
        12345 // 模拟数据
    } catch (e: Exception) {
        Log.e(TAG, "服务器数据获取失败: ${e.message}")
        0 // 返回0表示获取失败
    }
}
```

### 3. 数据验证

```kotlin
// 验证步数数据的合理性
private fun validateStepData(steps: Int): Boolean {
    return steps >= 0 && steps <= 100000 // 合理的步数范围
}
```

## 📈 性能优化策略

### 1. 缓存策略

- 优先使用本地缓存
- 异步更新缓存数据
- 定期清理过期缓存

### 2. 异步处理

- 所有网络请求异步执行
- UI更新在主线程执行
- 传感器数据异步处理

### 3. 资源管理

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // 释放传感器资源
    sensorManager.releaseSensors()
}
```

## 🔄 生命周期管理

### 1. Activity生命周期

```
onCreate() → 初始化传感器 → 获取数据 → 更新UI
    ↓
onResume() → 恢复传感器监听
    ↓
onPause() → 暂停传感器监听
    ↓
onDestroy() → 释放传感器资源
```

### 2. 传感器生命周期

```
initializeSensors() → 注册监听器 → 开始监听
    ↓
数据变化 → 处理数据 → 更新UI
    ↓
releaseSensors() → 注销监听器 → 释放资源
```

## 📊 数据流程图

```
用户操作 → 传感器检测 → 数据处理 → 缓存存储 → UI更新
    ↓
网络请求 → 服务器响应 → 数据验证 → 缓存更新 → UI刷新
    ↓
应用启动 → 缓存检查 → 数据获取 → 界面初始化 → 用户交互
```

## 🎯 关键特性

1. **实时更新**: 传感器数据变化时立即更新UI
2. **多数据源**: 支持Google Fit、本地传感器、服务器数据
3. **智能缓存**: 优先使用本地缓存，减少网络请求
4. **统一计算**: 所有界面使用相同的计算逻辑
5. **错误恢复**: 网络失败时自动降级到本地数据
6. **资源管理**: 正确的生命周期管理，防止内存泄漏

## 📝 注意事项

1. **单位统一**: 所有计算使用克(g)作为单位
2. **初始化检查**: 传感器使用前必须检查初始化状态
3. **线程安全**: UI更新必须在主线程执行
4. **错误处理**: 所有网络请求和传感器操作都要有错误处理
5. **资源释放**: Activity销毁时必须释放传感器资源

---

*本文档描述了碳足迹应用的核心逻辑流程，为开发和维护提供参考。* 