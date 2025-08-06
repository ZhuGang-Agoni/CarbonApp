package com.zg.carbonapp.Service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.zg.carbonapp.R

class WeeklyNotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val channelId = "weekly_report_channel"
        val notificationId = 1001

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "每周报告通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每周碳足迹报告提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建通知
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_tree)
            .setContentTitle("新的一周开始了!")
            .setContentText("点击查看上周碳足迹分析报告")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}