package com.zg.carbonapp.Service

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val WORK_NAME = "weekly_notification_work"
    private const val TAG = "NotificationScheduler"

    fun scheduleWeeklyNotification(context: Context) {
        try {
            // 每周一上午9点触发
            val request = PeriodicWorkRequestBuilder<WeeklyNotificationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "Weekly notification scheduled successfully")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "WorkManager not initialized: ${e.message}")
            // 这里可以添加重试逻辑或通知开发者
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification: ${e.message}")
        }
    }

    private fun calculateInitialDelay(): Long {
        try {
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK)

            // 如果今天是周一，检查是否已过9点
            if (today == Calendar.MONDAY) {
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                if (hour < 9) {
                    // 今天9点触发
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis - System.currentTimeMillis()
                }
            }

            // 计算到下周一的9点
            calendar.add(Calendar.DAY_OF_WEEK, (Calendar.MONDAY - today + 7) % 7)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return calendar.timeInMillis - System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating initial delay: ${e.message}")
            // 默认返回1分钟延迟，避免崩溃
            return 60_000L
        }
    }
}