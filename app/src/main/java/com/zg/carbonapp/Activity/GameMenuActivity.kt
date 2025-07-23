package com.zg.carbonapp.Activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R

class GameMenuActivity : AppCompatActivity() {

    // 仅保留背景音乐播放器
    private lateinit var bgmPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)


        initBackgroundMusic()

        initButtons()

        animateUIElements()
    }

    // ======================== 仅初始化背景音乐 ========================
    private fun initBackgroundMusic() {
        try {
            bgmPlayer = MediaPlayer.create(this, R.raw.music1)
            bgmPlayer.isLooping = true
            bgmPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }


    private fun initButtons() {
        // 开始游戏
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startActivity(Intent(this, HigherSortGameActivity::class.java))
        }

        // 历史记录
        findViewById<Button>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, GameHistoryActivity::class.java))
        }


        findViewById<Button>(R.id.btn_settings).setOnClickListener {
//            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 退出游戏
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
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

    override fun onPause() {
        super.onPause()
        bgmPlayer.pause()  // 暂停音乐（切到后台）
    }

    override fun onResume() {
        super.onResume()
        bgmPlayer.start()  // 恢复音乐（回到前台）
    }

    override fun onDestroy() {
        super.onDestroy()
        bgmPlayer.release()  // 释放播放器（防止内存泄漏）
    }
}