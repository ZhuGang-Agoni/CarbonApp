package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.Service.MusicService

class GameMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)

        // 1. 启动音乐服务（应用启动即播放，跨页面持续运行）
        startService(Intent(this, MusicService::class.java))

        // 2. 初始化按钮 + 动画
        initButtons()
        animateUIElements()
    }

    private fun initButtons() {
        // 开始游戏（跳转到游戏页，音乐继续播放）
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this, HigherSortGameActivity::class.java))
        }

        // 历史记录（跳转到历史页，音乐继续播放）
        findViewById<Button>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, GameHistoryActivity::class.java))
        }

        // 设置（跳转到设置页，音乐继续播放）
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            // startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 退出游戏（停止音乐 + 关闭应用）
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            stopService(Intent(this, MusicService::class.java)) // 停止音乐
            finish()
            System.exit(0)
        }
    }

    private fun animateUIElements() {
        // 标题 + 副标题 淡入动画
        val title = findViewById<TextView>(R.id.tv_title)
        val subtitle = findViewById<TextView>(R.id.tv_subtitle)

        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slow))
        subtitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slow))

        // 按钮 从底部滑入（依次延迟）
        val buttons = arrayOf(
            findViewById<Button>(R.id.btn_start),
            findViewById<Button>(R.id.btn_history),
            findViewById<Button>(R.id.btn_settings),
            findViewById<Button>(R.id.btn_exit)
        )

        for (i in buttons.indices) {
            val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slideUpAnim.startOffset = i * 150L  // 每个按钮延迟 150ms
            buttons[i].startAnimation(slideUpAnim)
        }
    }

    // 以下生命周期方法无需处理媒体，交给Service管理
    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}