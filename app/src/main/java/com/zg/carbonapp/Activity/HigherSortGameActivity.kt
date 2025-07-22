package com.zg.carbonapp.Activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.zg.carbonapp.Dao.GameHistoryRecord
import com.zg.carbonapp.MMKV.GameHistoryRecordMMKV
import com.zg.carbonapp.R
import kotlin.random.Random
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



class HigherSortGameActivity : AppCompatActivity() {

    // UI元素
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvCombo: TextView
    private lateinit var gameArea: ConstraintLayout
    private lateinit var recyclableBin: ImageView
    private lateinit var kitchenBin: ImageView
    private lateinit var hazardousBin: ImageView
    private lateinit var otherBin: ImageView
    private lateinit var gameOverLayout: LinearLayout
    private lateinit var finalScore: TextView
    private lateinit var btnRestart: TextView
    private lateinit var btnMenu: TextView

    // 游戏变量
    private var score = 0
    private var comboCount = 0
    private var gameActive = true
    private lateinit var countDownTimer: CountDownTimer
    private val handler = Handler(Looper.getMainLooper())
    private val garbageItems = mutableListOf<ImageView>()

    // 垃圾类型和对应的垃圾桶
    private val garbageTypes = listOf(
        R.drawable.banana to "kitchen",
        R.drawable.old_book to "recyclable",
        R.drawable.light_bulb to "hazardous",
        R.drawable._4___water_bottle__flat___oliviu_stoian_s_conflict to "recyclable",
        R.drawable.battery to "hazardous",
        R.drawable.newspaper to "recyclable",
        R.drawable.fish_bone to "kitchen",
        R.drawable._37_medicine to "hazardous",
        R.drawable.clothes to "other",
        R.drawable.cigarettes to "other",
        R.drawable.__1 to "kitchen"
    )

    // 碳排放减少说明
    private val carbonReductionTips = mapOf(
        "recyclable" to "回收1吨废纸可减少碳排放0.8吨",
        "kitchen" to "厨余垃圾堆肥可减少甲烷排放",
        "hazardous" to "正确处理有害垃圾避免土壤污染",
        "other" to "减少垃圾总量降低运输排放"
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_higher_sort_game)

        // 初始化视图
        initViews()

        // 设置垃圾桶拖放监听
        setupBinDragListeners()

        // 设置按钮监听
        setupButtonListeners()

        // 开始游戏
        startGame()
    }

    private fun initViews() {
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvCombo = findViewById(R.id.tvCombo)
        gameArea = findViewById(R.id.gameArea)
        recyclableBin = findViewById(R.id.recyclableBin)
        kitchenBin = findViewById(R.id.kitchenBin)
        hazardousBin = findViewById(R.id.hazardousBin)
        otherBin = findViewById(R.id.otherBin)
        gameOverLayout = findViewById(R.id.gameOverLayout)
        finalScore = findViewById(R.id.finalScore)
        btnRestart = findViewById(R.id.btnRestart)
        btnMenu = findViewById(R.id.btnMenu)

        // 隐藏组合计数
        tvCombo.visibility = View.INVISIBLE
    }

    private fun setupBinDragListeners() {
        recyclableBin.setOnDragListener(binDragListener)
        kitchenBin.setOnDragListener(binDragListener)
        hazardousBin.setOnDragListener(binDragListener)
        otherBin.setOnDragListener(binDragListener)
    }

    private fun setupButtonListeners() {
        btnRestart.setOnClickListener {
            resetGame()
        }

        btnMenu.setOnClickListener {
            // 返回主菜单
            finish()
        }
    }

    private fun startGame() {
        score = 0
        comboCount = 0
        gameActive = true
        updateScore()
        gameOverLayout.visibility = View.GONE

        // 启动倒计时
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "${millisUntilFinished / 1000}秒"
            }

            override fun onFinish() {
                tvTimer.text = "0秒"
                endGame()
            }
        }.start()

        // 开始生成垃圾
        generateGarbage()
    }

    private fun generateGarbage() {
        if (!gameActive) return

        // 【修改1：延长垃圾生成间隔（1-2秒），减少同时出现的垃圾】
        val delay = Random.nextLong(1000, 2000) // 原：500-1500
        handler.postDelayed({
            if (gameActive) {
                createGarbageItem()
                generateGarbage()
            }
        }, delay)
    }

    private fun createGarbageItem() {
        // 选择随机垃圾类型
        val (garbageRes, garbageType) = garbageTypes.random()

        // 创建垃圾图像视图
        val garbage = ImageView(this)
        garbage.setImageResource(garbageRes)
        garbage.tag = garbageType

        // 设置大小
        val size = resources.getDimensionPixelSize(R.dimen.garbage_size)
        val params = ConstraintLayout.LayoutParams(size, size)

        // 随机水平位置
        val screenWidth = resources.displayMetrics.widthPixels
        val xPos = Random.nextInt(50, screenWidth - size - 50)
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginStart = xPos
        params.topMargin = -size // 从屏幕上方开始

        garbage.layoutParams = params

        // 添加触摸监听器
        garbage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 开始拖动
                v.performClick()
                val shadowBuilder = View.DragShadowBuilder(v)
                v.startDragAndDrop(null, shadowBuilder, v, 0)
                true
            } else {
                false
            }
        }

        // 添加到游戏区域
        gameArea.addView(garbage)
        garbageItems.add(garbage)

        // 【修改2：延长下落动画时间（6-10秒），下落更慢】
        val animator = ObjectAnimator.ofFloat(garbage, "translationY", 0f, resources.displayMetrics.heightPixels.toFloat())
        animator.duration = Random.nextLong(6000, 10000) // 原：3000-6000
        animator.interpolator = AccelerateInterpolator()
        animator.start()

        // 动画结束监听
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (garbage.parent != null && gameActive) {
                    // 垃圾未处理，扣除时间
                    gameArea.removeView(garbage)
                    garbageItems.remove(garbage)
                    deductTime()
                }
            }
        })
    }

    private val binDragListener = View.OnDragListener { bin, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // 高亮垃圾桶
                bin.alpha = 0.7f
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                bin.alpha = 1f
                bin.scaleX = 1.2f
                bin.scaleY = 1.2f
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                bin.alpha = 0.7f
                bin.scaleX = 1f
                bin.scaleY = 1f
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                bin.alpha = 1f
                bin.scaleX = 1f
                bin.scaleY = 1f
                true
            }
            DragEvent.ACTION_DROP -> {
                // 获取拖动的垃圾
                val draggedView = event.localState as ImageView
                val garbageType = draggedView.tag as String

                // 检查是否正确分类
                val binType = when (bin.id) {
                    R.id.recyclableBin -> "recyclable"
                    R.id.kitchenBin -> "kitchen"
                    R.id.hazardousBin -> "hazardous"
                    R.id.otherBin -> "other"
                    else -> ""
                }

                if (garbageType == binType) {
                    // 正确分类
                    handleCorrectDrop(draggedView, bin, garbageType)
                } else {
                    // 错误分类
                    handleWrongDrop(draggedView)
                }
                true
            }
            else -> false
        }
    }

    private fun handleCorrectDrop(garbage: ImageView, bin: View, garbageType: String) {
        // 移除垃圾
        gameArea.removeView(garbage)
        garbageItems.remove(garbage)

        // 增加分数
        comboCount++
        val points = if (comboCount >= 3) 20 * comboCount else 10
        score += points

        // 更新UI
        updateScore()

        // 显示组合特效
        showComboEffect()

        // 播放垃圾桶动画
        playBinAnimation(bin)

        // 显示碳排放提示
        showCarbonTip(garbageType)
    }

    private fun handleWrongDrop(garbage: ImageView) {
        // 移除垃圾
        gameArea.removeView(garbage)
        garbageItems.remove(garbage)

        // 重置组合
        comboCount = 0
        tvCombo.visibility = View.INVISIBLE

        // 扣除时间
        deductTime()
    }

    private fun updateScore() {
        tvScore.text = "分数: $score"
    }

    private fun showComboEffect() {
        if (comboCount >= 2) {
            tvCombo.visibility = View.VISIBLE
            tvCombo.text = "$comboCount 连击!"

            // 动画效果
            tvCombo.scaleX = 1.5f
            tvCombo.scaleY = 1.5f
            tvCombo.alpha = 1f

            // 缩放动画
            val scaleX = ObjectAnimator.ofFloat(tvCombo, "scaleX", 1.5f, 1f)
            val scaleY = ObjectAnimator.ofFloat(tvCombo, "scaleY", 1.5f, 1f)
            scaleX.duration = 500
            scaleY.duration = 500
            scaleX.start()
            scaleY.start()

            // 根据连击数设置颜色
            val color = when (comboCount) {
                2 -> Color.YELLOW
                3 -> Color.GREEN
                4 -> Color.BLUE
                else -> Color.MAGENTA
            }
            tvCombo.setTextColor(color)
        }
    }

    private fun playBinAnimation(bin: View) {
        // 闪烁动画
        val animator = ValueAnimator.ofFloat(1f, 1.5f, 1f)
        animator.duration = 300
        animator.interpolator = BounceInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            bin.scaleX = value
            bin.scaleY = value

            // 连击特效
            if (comboCount >= 3) {
                bin.background = ContextCompat.getDrawable(this@HigherSortGameActivity, R.drawable.bin_glow)
            }
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                bin.background = null
            }
        })

        animator.start()
    }

    private fun showCarbonTip(garbageType: String) {
        val tip = carbonReductionTips[garbageType] ?: ""
        if (tip.isNotEmpty() && comboCount >= 3) {
            val tipView = TextView(this)
            tipView.text = tip
            tipView.setTextColor(Color.GREEN)
            tipView.textSize = 16f
            tipView.setBackgroundColor(Color.parseColor("#80000000"))
            tipView.setPadding(16, 8, 16, 8)

            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

            gameArea.addView(tipView, params)

            // 3秒后移除提示
            handler.postDelayed({
                if (tipView.parent != null) {
                    gameArea.removeView(tipView)
                }
            }, 3000)
        }
    }

    private fun deductTime() {
        // 【修改3：减少未接住垃圾的惩罚（扣1秒）】
        val currentTime = tvTimer.text.toString().replace("秒", "").toIntOrNull() ?: 0
        val newTime = (currentTime - 1).coerceAtLeast(0) // 原：-2

        if (newTime == 0) {
            endGame()
        } else {
            tvTimer.text = "${newTime}秒"
        }

        // 闪烁红色警告
        tvTimer.setTextColor(Color.RED)
        handler.postDelayed({
            tvTimer.setTextColor(Color.WHITE)
        }, 500)
    }
    // 这里是游戏结算画面
    private fun endGame() {
        gameActive = false
        countDownTimer.cancel()
        handler.removeCallbacksAndMessages(null)

        // 移除所有垃圾
        garbageItems.forEach { gameArea.removeView(it) }
        garbageItems.clear()

        // 计算碳减排量 (每10分相当于减少1kg碳排放)
        val carbonReduction = score / 10

        // 显示游戏结束界面
        gameOverLayout.visibility = View.VISIBLE
        finalScore.text = "最终分数: $score\n减少碳排放: ${carbonReduction}kg"

// 这里的id 从 userMMKV 获取去 这里只是模拟
        val currentTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formattedTime = currentTime.format(formatter)
//？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？
        val gameRecord = GameHistoryRecord(
            id = 123, // 实际从userMMKV获取
            score = score,
            carbonReduction = carbonReduction,
            date = formattedTime // 这里传入格式化后的时间
        )
        GameHistoryRecordMMKV.saveGameRecord(gameRecord)

    }

    private fun resetGame() {
        // 重置游戏状态
        gameActive = false
        countDownTimer.cancel()
        handler.removeCallbacksAndMessages(null)

        // 移除所有垃圾
        garbageItems.forEach { gameArea.removeView(it) }
        garbageItems.clear()

        // 重新开始游戏
        startGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        handler.removeCallbacksAndMessages(null)
    }
}