package com.zg.carbonapp.Activity

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.zg.carbonapp.Dao.*
import com.zg.carbonapp.MMKV.AchievementProductManager
import com.zg.carbonapp.MMKV.StepCarbonMMKV
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.Repository.AchievementProductRepository
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityStepCarbonBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class StepCarbonActivity : AppCompatActivity(), SensorEventListener {
    // 视图绑定
    private lateinit var binding: ActivityStepCarbonBinding

    // 传感器相关
    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null

    // 步数数据
    private var todaySteps = 0
    private var totalSteps = 0L
    private lateinit var todayData: DailyStepData
    private val dailyStepGoal = 10000 // 每日目标步数

    // 进度条控制
    private var maxProgressWidth = 0
    private var currentProgressWidth = 0

    // 日期格式化
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 定位相关
    private lateinit var locationClient: LocationClient
    private var currentLat = 0.0
    private var currentLng = 0.0

    // 成就时间轴
    private lateinit var timelineNodes: List<TimelineNode>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepCarbonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化数据和视图
        initData()
        initViews()
        initListeners()
        setupTimeline()
        setupSensor()
        setupLocation()

        // 启动数据刷新
        startDataRefresh()
    }

    // 初始化基础数据
    private fun initData() {
        val todayDate = getTodayDate()
        todayData = StepCarbonMMKV.getTodayData() ?: DailyStepData(todayDate)
        todaySteps = todayData.steps

        // 检查是否跨天
        val lastRecordDate = StepCarbonMMKV.getLastStepDate()
        if (lastRecordDate != todayDate) {
            resetDailyData() // 跨天重置数据
        }

        calculateTotalSteps()
    }

    // 初始化视图
    private fun initViews() {
        // 进度条初始化（关键修复）
        binding.progressBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // 获取进度条容器宽度（ConstraintLayout）
                maxProgressWidth = (binding.progressBar.parent as View).width
                updateProgressUI() // 初始更新进度
            }
        })

        // 初始UI更新
        updateStepDisplay()
        updateStatsDisplay()
    }

    // 初始化事件监听
    private fun initListeners() {
        // 返回按钮
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 历史数据按钮
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, CarbonHistoryActivity::class.java))
        }

        // 减排报告按钮
        binding.btnReport.setOnClickListener {
            val intent = Intent(this, CarbonReportActivity::class.java)
            intent.putExtra("todayData", todayData as Parcelable)
            startActivity(intent)
        }
    }

    // 初始化时间轴
    private fun setupTimeline() {
        timelineNodes = listOf(
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 1 }!!,
                requiredSteps = 10000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 2 }!!,
                requiredSteps = 50000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 3 }!!,
                requiredSteps = 200000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 4 }!!,
                requiredSteps = 30000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 5 }!!,
                requiredSteps = 100000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 6 }!!,
                requiredSteps = 20000
            ),
            TimelineNode(
                product = AchievementProductRepository.allAchievements.find { it.id == 7 }!!,
                requiredSteps = 150000
            )
        ).sortedBy { it.requiredSteps }

        // 填充时间轴视图
        val inflater = LayoutInflater.from(this)
        timelineNodes.forEachIndexed { index, node ->
            // 添加节点
            val nodeView = inflater.inflate(R.layout.item_timeline_node, binding.timelineContainer, false)
            nodeView.findViewById<ImageView>(R.id.nodeIcon).setImageResource(node.product.iconRes)
            nodeView.findViewById<TextView>(R.id.nodeName).text = node.product.name
            nodeView.findViewById<TextView>(R.id.nodeSteps).text = "${node.requiredSteps / 1000}k步"

            // 节点点击事件
            nodeView.setOnClickListener { showProductDetail(node) }
            binding.timelineContainer.addView(nodeView)

            // 添加连接线（最后一个节点不需要）
            if (index < timelineNodes.size - 1) {
                val lineView = inflater.inflate(R.layout.item_timeline_line, binding.timelineContainer, false)
                binding.timelineContainer.addView(lineView)
            }
        }

        checkAchievements()
    }

    // 初始化传感器
    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounter == null) {
            Toast.makeText(this, "设备不支持步数统计", Toast.LENGTH_LONG).show()
        }
    }

    // 初始化定位
    private fun setupLocation() {
        LocationClient.setAgreePrivacy(true)
        locationClient = LocationClient(applicationContext)
        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setScanSpan(5000)
            setIsNeedAddress(true)
        }
        locationClient.locOption = option
        locationClient.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation) {
                currentLat = location.latitude
                currentLng = location.longitude
            }
        })
        locationClient.start()
    }

    // 启动数据刷新
    private fun startDataRefresh() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                stepCounter?.let {
                    sensorManager.registerListener(this@StepCarbonActivity, it, SensorManager.SENSOR_DELAY_UI)
                }
                Handler(Looper.getMainLooper()).postDelayed(this, 3000)
            }
        }, 0)
    }

    // 传感器数据更新
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepCount = event.values[0].toLong()
            val todayDate = getTodayDate()

            // 设置每日基准值（首次启动时）
            if (StepCarbonMMKV.getDailyStepBase(todayDate) == 0L) {
                StepCarbonMMKV.saveDailyStepBase(todayDate, totalStepCount)
            }

            // 计算今日步数
            val todayBase = StepCarbonMMKV.getDailyStepBase(todayDate)
            val newSteps = (totalStepCount - todayBase).toInt()
            val validSteps = if (newSteps < 0) 0 else newSteps

            // 步数变化时更新
            if (validSteps != todaySteps) {
                todaySteps = validSteps
                updateDailyData()
                updateStepDisplay()
                updateStatsDisplay()
                updateProgressUI()
                checkAchievements()
            }
        }
    }

    // 更新每日数据
    private fun updateDailyData() {
        todayData = todayData.copy(
            steps = todaySteps
        ).apply {
            calculateCarbon() // 重新计算碳减排和积分
        }
        StepCarbonMMKV.saveTodayData(todayData)
        calculateTotalSteps()
    }

    // 跨天重置数据
    private fun resetDailyData() {
        val todayDate = getTodayDate()
        todayData = DailyStepData(todayDate)
        todaySteps = 0
        currentProgressWidth = 0
        StepCarbonMMKV.saveLastStepDate(todayDate)
        StepCarbonMMKV.saveTodayData(todayData)
        // 强制更新进度条为0
        runOnUiThread {
            binding.progressBar.layoutParams.width = 0
            binding.progressBar.requestLayout()
        }
    }

    // 计算累计总步数
    private fun calculateTotalSteps() {
        totalSteps = 0L
        StepCarbonMMKV.getDateList().forEach { date ->
            totalSteps += StepCarbonMMKV.getTodayData(date)?.steps ?: 0
        }
        totalSteps += (todaySteps - (StepCarbonMMKV.getTodayData()?.steps ?: 0))
    }

    // 更新步数显示
    private fun updateStepDisplay() {
        // 步数动画
        ValueAnimator.ofInt(
            binding.tvSteps.text.toString().toIntOrNull() ?: 0,
            todaySteps
        ).apply {
            duration = 500
            addUpdateListener { anim ->
                binding.tvSteps.text = anim.animatedValue.toString()
                binding.tvTotalSteps.text = totalSteps.toString()
            }
            start()
        }
    }

    // 更新统计数据显示
    private fun updateStatsDisplay() {
        binding.apply {
            // 碳减排
            tvCarbon.text = String.format("%.2f kg", todayData.carbonReduction)

            // 积分动画
            ValueAnimator.ofInt(
                tvPoints.text.toString().toIntOrNull() ?: 0,
                todayData.carbonPoints
            ).apply {
                duration = 500
                addUpdateListener { anim ->
                    val points = anim.animatedValue.toString().toInt()
                    tvPoints.text = points.toString()
                    // 更新用户总积分
                    UserMMKV.getUser()?.let { user ->
                        UserMMKV.saveUser(user.copy(
                            carbonCount = user.carbonCount + (points - (todayData.carbonPoints - points))
                        ))
                    }
                }
                start()
            }

            // 距离
            tvDistance.text = String.format("%.1f 公里", todaySteps / 1000.0)
        }
    }

    // 更新进度条UI（核心修复：0步时完全隐藏进度条填充）
    private fun updateProgressUI() {
        if (maxProgressWidth == 0) return // 确保宽度已获取

        // 步数为0时特殊处理
        if (todaySteps == 0) {
            if (currentProgressWidth != 0) {
                currentProgressWidth = 0
                binding.progressBar.layoutParams.width = 0
                binding.progressBar.requestLayout()
            }
            binding.tvGoalProgress.text = "0/$dailyStepGoal 步 (0%)"
            return
        }

        // 计算进度百分比
        val progressPercent = (todaySteps.toFloat() / dailyStepGoal * 100).coerceAtMost(100f).roundToInt()

        // 更新进度文本
        binding.tvGoalProgress.text = "$todaySteps/$dailyStepGoal 步 ($progressPercent%)"

        // 计算目标宽度
        val targetWidth = (maxProgressWidth * progressPercent / 100).coerceAtMost(maxProgressWidth.toFloat().toInt()).toInt()

        // 进度条动画
        if (targetWidth != currentProgressWidth) {
            ValueAnimator.ofInt(currentProgressWidth, targetWidth).apply {
                duration = 500
                addUpdateListener { anim ->
                    currentProgressWidth = anim.animatedValue as Int
                    binding.progressBar.layoutParams.width = currentProgressWidth
                    binding.progressBar.requestLayout()
                }
                start()
            }
        }
    }

    // 检查成就解锁
    private fun checkAchievements() {
        val user = UserMMKV.getUser() ?: return
        timelineNodes.forEach { node ->
            if (totalSteps >= node.requiredSteps
                && !AchievementProductManager.isAchievementItemUnlocked(user.userId, node.product.id)
                && AchievementProductManager.needShowReminder(user.userId, node.product.id)) {

                node.isUnlocked = true
                AchievementProductManager.unlockAndMarkReminder(user.userId, node.product.id)
                showAchievementUnlockedDialog(node)
            }
        }
        updateTimelineStatus()
    }

    // 更新时间轴状态
    private fun updateTimelineStatus() {
        val user = UserMMKV.getUser() ?: return
        binding.timelineContainer.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child.id == R.id.nodeContainer) {
                    val nodeIndex = i / 2
                    if (nodeIndex < timelineNodes.size) {
                        val node = timelineNodes[nodeIndex]
                        val isUnlocked = AchievementProductManager.isAchievementItemUnlocked(user.userId, node.product.id)
                        val icon = child.findViewById<ImageView>(R.id.nodeIcon)
                        icon.setImageResource(if (isUnlocked) node.product.unlockRes else node.product.iconRes)
                        child.findViewById<TextView>(R.id.nodeName).setTextColor(
                            ContextCompat.getColor(this, if (isUnlocked) R.color.green_primary else R.color.gray_400)
                        )
                    }
                }
            }
        }
    }

    // 显示成就解锁对话框
    private fun showAchievementUnlockedDialog(node: TimelineNode) {
        AlertDialog.Builder(this)
            .setTitle("成就解锁!")
            .setMessage("恭喜您解锁了${node.product.name}!\n${node.product.description}")
            .setPositiveButton("查看详情") { _, _ ->
                showProductDetail(node)
            }
            .setNegativeButton("关闭", null)
            .setIcon(AppCompatResources.getDrawable(this, node.product.unlockRes))
            .show()
    }

    // 显示物品详情
    private fun showProductDetail(node: TimelineNode) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_detail, null)
        dialogView.findViewById<ImageView>(R.id.productIcon).setImageResource(
            if (node.isUnlocked) node.product.unlockRes else node.product.iconRes
        )
        dialogView.findViewById<TextView>(R.id.productName).text = node.product.name
        dialogView.findViewById<TextView>(R.id.productDesc).text = node.product.description
        dialogView.findViewById<TextView>(R.id.productRequirements).text =
            "需要累计${node.requiredSteps / 1000}k步"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(if (node.isUnlocked) "已解锁" else "继续努力", null)
            .show()
    }

    // 获取今日日期
    private fun getTodayDate() = dateFormat.format(Date())

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationClient.stop()
    }
}
