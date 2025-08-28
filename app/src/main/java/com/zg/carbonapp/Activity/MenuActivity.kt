package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.model.GarbageType
import com.zg.carbonapp.model.Player


class MenuActivity : AppCompatActivity() {

    // 定义四个垃圾桶角色
    private val players = listOf(
        Player("厨余垃圾桶", GarbageType.KITCHEN, R.drawable.kitchen_bin),
        Player("可回收物桶", GarbageType.RECYCLABLE, R.drawable.recyclable_bin),
        Player("有害垃圾桶", GarbageType.HAZARDOUS, R.drawable.hazardous_bin),
        Player("其他垃圾桶", GarbageType.OTHER, R.drawable.other_bin)
    )

    private var currentPlayerIndex = 0

    private lateinit var playerImageView: ImageView
    private lateinit var playerNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        playerImageView = findViewById(R.id.player_image)
        playerNameTextView = findViewById(R.id.player_name)

        // 初始化显示第一个角色
        updatePlayerDisplay()

        // 左右切换按钮点击事件
        findViewById<View>(R.id.btn_left).setOnClickListener {
            currentPlayerIndex = (currentPlayerIndex - 1 + players.size) % players.size
            updatePlayerDisplay()
        }

        findViewById<View>(R.id.btn_right).setOnClickListener {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
            updatePlayerDisplay()
        }

        // 开始游戏按钮点击事件
        findViewById<View>(R.id.btn_start_game).setOnClickListener {
            val intent = Intent(this, NewGarbageGameActivity::class.java)
            intent.putExtra("selected_player", players[currentPlayerIndex])
            startActivity(intent)
        }

        // 查看记录按钮点击事件
        findViewById<View>(R.id.btn_view_records).setOnClickListener {
            // 这里可以实现查看游戏记录的逻辑
            // 简单处理，实际项目中应该跳转到记录列表界面
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }
    }

    // 更新当前选中的角色显示
    private fun updatePlayerDisplay() {
        val currentPlayer = players[currentPlayerIndex]
        playerImageView.setImageResource(currentPlayer.imageRes)
        playerNameTextView.text = currentPlayer.name
    }
}
