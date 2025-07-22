package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R

class GameMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)

        // 初始化按钮并添加点击事件
        initButtons()

        // 添加标题和按钮的动画效果
        animateUIElements()
    }

    private fun initButtons() {
        // 开始游戏按钮
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            // 跳转到游戏主界面
            startActivity(Intent(this, HigherSortGameActivity::class.java))

        }

        // 历史记录按钮
        findViewById<Button>(R.id.btn_history).setOnClickListener {
            // 跳转到历史记录界面
            startActivity(Intent(this, GameHistoryActivity::class.java))

        }

        // 设置按钮
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            // 跳转到设置界面
//            startActivity(Intent(this, SettingsActivity::class.java))

        }

        // 退出按钮
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finish()
            // 关闭整个应用
            System.exit(0)
        }
    }

    private fun animateUIElements() {
        // 标题动画：淡入
        val title = findViewById<TextView>(R.id.tv_title)
        val subtitle = findViewById<TextView>(R.id.tv_subtitle)

        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slow))
        subtitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slow))

        // 按钮动画：从底部滑入
        val buttons = arrayOf(
            findViewById<Button>(R.id.btn_start),
            findViewById<Button>(R.id.btn_history),
            findViewById<Button>(R.id.btn_settings),
            findViewById<Button>(R.id.btn_exit)
        )

//        for (i in buttons.indices) {
//            buttons[i].startAnimation(
//                AnimationUtils.loadAnimation(
//                    this,
//                    R.anim.slide_up_delay(i * 100) // 每个按钮延迟100ms
//                )
//            )
        }

}