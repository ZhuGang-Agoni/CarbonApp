package com.zg.carbonapp.Service

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Google Fit服务类
 * 
 * 功能说明：
 * 1. 管理Google Fit账号登录状态
 * 2. 读取用户步数数据
 * 3. 提供步数统计相关API
 * 
 * 数据来源：
 * - Google Fit History API
 * - 支持读取历史步数数据
 * - 支持按日期查询步数
 */
class GoogleFitService(private val context: Context) {
    companion object {
        private const val TAG = "GoogleFitService"
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }

    /**
     * 构建Google Fit权限选项
     * 
     * 声明需要读取的数据类型：
     * - TYPE_STEP_COUNT_DELTA: 步数增量数据
     * - AGGREGATE_STEP_COUNT_DELTA: 聚合步数数据
     * 
     * 权限级别：ACCESS_READ（只读权限）
     */
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    /**
     * 获取Google Fit账号
     * 
     * @return GoogleSignInAccount 当前登录的Google账号
     * 用于后续所有API调用时的身份验证
     */
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

    /**
     * 检查当前应用是否已获得Google Fit权限
     * 
     * @return true 已授权，false 未授权
     * 
     * 使用场景：
     * - 在调用步数API前检查权限状态
     * - 判断是否需要引导用户登录Google Fit
     */
    fun isSignedIn(): Boolean {
        return GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)
    }

    /**
     * 获取FitnessOptions对象
     * 
     * @return FitnessOptions 权限配置对象
     * 
     * 使用场景：
     * - 在Activity中请求权限时传递配置
     * - 检查权限状态时使用
     */
    fun getFitnessOptions(): FitnessOptions = fitnessOptions

    /**
     * 获取指定日期的步数（异步回调）
     * 
     * @param date 日期字符串，格式yyyy-MM-dd
     * @param callback 回调函数，返回步数Int
     * 
     * 实现说明：
     * 1. 检查用户登录状态
     * 2. 解析日期，设置时间区间为当天0点到次日0点
     * 3. 构建步数读取请求（使用聚合步数数据类型）
     * 4. 通过Google Fit History API异步读取数据
     * 5. 累加所有数据集中的步数并回调结果
     */
    fun getStepsForDate(date: String, callback: (Int) -> Unit) {
        // 检查用户是否已登录Google Fit
        if (!isSignedIn()) {
            Log.w(TAG, "用户未登录Google Fit")
            callback(0)
            return
        }

        try {
            // 解析日期，设置时间区间为当天0点到次日0点
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = sdf.parse(date) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = targetDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val startTime = calendar.timeInMillis
            val endTime = startTime + TimeUnit.DAYS.toMillis(1)

            // 构建步数读取请求（使用聚合步数数据类型）
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            // 通过Google Fit History API异步读取步数数据
            Fitness.getHistoryClient(context, getGoogleAccount())
                .readData(readRequest)
                .addOnSuccessListener { response ->
                    // 累加所有数据集中的步数
                    val totalSteps = response.dataSets.sumOf { dataSet ->
                        dataSet.dataPoints.sumOf { dataPoint ->
                            dataPoint.getValue(Field.FIELD_STEPS).asInt()
                        }
                    }
                    Log.d(TAG, "获取到步数: $totalSteps for date: $date")
                    callback(totalSteps)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "获取步数失败: ${exception.message}")
                    callback(0)
                }

        } catch (e: Exception) {
            Log.e(TAG, "解析日期失败: ${e.message}")
            callback(0)
        }
    }

    /**
     * 获取今天的步数（异步回调）
     * 
     * @param callback 回调函数，返回步数Int
     * 
     * 实现说明：
     * 1. 获取今天的日期字符串
     * 2. 调用getStepsForDate获取今日步数
     */
    fun getTodaySteps(callback: (Int) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        getStepsForDate(today, callback)
    }

    /**
     * 获取本周总步数（异步回调，自动累加本周7天步数）
     * 
     * @param callback 回调函数，返回步数Int
     * 
     * 实现说明：
     * 1. 检查用户登录状态
     * 2. 计算本周的起始日期（周一）
     * 3. 依次获取本周7天的步数
     * 4. 累加所有天数的步数
     * 5. 当所有天数都获取完成后，回调总步数
     */
    fun getWeekSteps(callback: (Int) -> Unit) {
        // 检查用户是否已登录Google Fit
        if (!isSignedIn()) {
            callback(0)
            return
        }

        // 计算本周的起始日期（周一）
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dayOfWeek)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var totalSteps = 0
        var completedDays = 0

        // 依次获取本周7天的步数并累加
        for (i in 0..6) {
            val date = sdf.format(calendar.time)
            getStepsForDate(date) { steps ->
                totalSteps += steps
                completedDays++

                // 当所有7天都获取完成后，回调总步数
                if (completedDays == 7) {
                    callback(totalSteps)
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
} 