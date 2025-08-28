package com.zg.carbonapp.Activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.ViewModel.GarbageSortingGameView
import com.zg.carbonapp.model.Player


class NewGarbageGameActivity : AppCompatActivity() {

    private lateinit var gameView: GarbageSortingGameView
    private lateinit var restartButton: Button

    private var hasResurrected = false  // 是否已经使用过复活机会

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_garbage_game)

        // 获取从菜单界面传递过来的选中的角色
        val selectedPlayer = intent.getParcelableExtra<Player>("selected_player")

        gameView = findViewById(R.id.gameView)
        restartButton = findViewById(R.id.restartButton)

        // 初始化游戏视图，设置玩家
        selectedPlayer?.let { gameView.initGame(it) }

        // 开始游戏
        gameView.startGame()

        // 重新开始按钮点击事件
        restartButton.setOnClickListener {
            gameView.resetGame()
            hasResurrected = false
        }

        // 设置游戏回调
        gameView.setOnGameEventListener(object : GarbageSortingGameView.GameEventListener {
            override fun onGarbageMissed() {
                // 垃圾未接住的处理
            }

            override fun onWrongGarbageCollected(garbageType: String, playerType: String) {
                // 接到错误垃圾的处理
                showWrongGarbageDialog(garbageType, playerType)
            }

            override fun onGameOver(score: Int) {
                // 游戏结束的处理
                showGameOverDialog(score)
            }
        })
    }

    // 显示接错垃圾的对话框
    private fun showWrongGarbageDialog(garbageType: String, playerType: String) {
        runOnUiThread {
            val message = "错误：这是$garbageType，而您选择的是$playerType 垃圾桶！"

            if (!hasResurrected) {
                AlertDialog.Builder(this)
                    .setTitle("分类错误")
                    .setMessage("$message\n是否使用一次免费复活机会？")
                    .setPositiveButton("复活") { _, _ ->
                        hasResurrected = true
                        gameView.resurrect()
                    }
                    .setNegativeButton("结束游戏") { _, _ ->
                        gameView.gameOver()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("分类错误")
                    .setMessage("$message\n您已经使用过复活机会，游戏结束！")
                    .setPositiveButton("确定") { _, _ ->
                        gameView.gameOver()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    // 显示游戏结束对话框
    private fun showGameOverDialog(score: Int) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("游戏结束")
                .setMessage("您的最终得分：$score")
                .setPositiveButton("返回菜单") { _, _ ->
                    finish()
                }
                .setNegativeButton("再来一局") { _, _ ->
                    gameView.resetGame()
                    hasResurrected = false
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.stopGame()
    }

    override fun onResume() {
        super.onResume()
        gameView.startGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.stopGame()
    }
}
