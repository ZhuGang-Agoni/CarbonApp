package com.zg.carbonapp.Service

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zg.carbonapp.R

class MusicService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val binder = LocalBinder()

    // Binder 用于绑定服务（可选，此处主要用前台服务）
    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // 加载音乐（替换为你的音乐文件，如 res/raw/app_music.mp3）
            mediaPlayer = MediaPlayer.create(this, R.raw.music)
            mediaPlayer.isLooping = true // 循环播放
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ① 创建通知渠道（Android 8.0+ 必需）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "APP_MUSIC_CHANNEL", // 渠道ID（全局唯一）
                "App背景音乐",       // 渠道名称（用户可见）
                NotificationManager.IMPORTANCE_LOW // 低优先级，通知不弹窗
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // ② 启动前台服务（必须显示通知）
        startForeground(1, buildNotification())
    }

    // 构建前台服务的通知（极简设计，避免打扰用户）
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "APP_MUSIC_CHANNEL")
            .setContentTitle("App运行中")       // 通知标题
            .setContentText("背景音乐持续播放") // 通知内容
            .setSmallIcon(R.drawable.music) // 通知图标（必传，需放在 res/drawable）
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start() // 确保服务重启后音乐继续播放
        }
        return START_STICKY // 服务被杀死后，系统尝试重启
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()   // 停止播放
        mediaPlayer.release()// 释放资源
    }
}