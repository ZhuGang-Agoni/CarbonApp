package com.zg.carbonapp.Activity

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.FriendRankingAdapter
import com.zg.carbonapp.Dao.FriendRanking
import com.zg.carbonapp.R
import com.zg.carbonapp.Service.SensorManager
import com.zg.carbonapp.databinding.ActivityCarbonFootprintBinding
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.zg.carbonapp.Entity.CarbonFootprint
import java.util.Calendar

class CarbonFootprintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarbonFootprintBinding
    // 传感器管理器，用于获取步数数据
    private lateinit var sensorManager: SensorManager
    // 本周日期列表
    private lateinit var weekDates: List<String>
    // 今日步数
    private var todaySteps: Int = 0
    // 本周总步数
    private var weekSteps: Int = 0

    private val resetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.zg.carbonapp.RESET_STEP_BASELINE") {
                sensorManager.resetDailyBaseline()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 注册0点重置的广播接收器
        val filter = IntentFilter("com.zg.carbonapp.RESET_STEP_BASELINE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Android 8.0 及以上
            registerReceiver(resetReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }
        setupMidnightResetAlarm()

        // 用全限定名获取系统 SensorManager
        val sysSensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val stepCounterSensor = sysSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            Toast.makeText(this, "本设备支持步数计数器", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "本设备不支持步数计数器", Toast.LENGTH_SHORT).show()
        }

        sensorManager = SensorManager(this)
        sensorManager.initializeSensors()

        // 获取本周日期列表
        weekDates = getThisWeekDates()
        
        // 异步获取本周步数
        getWeekSteps(weekDates) { total ->
            weekSteps = total
            updateUI(todaySteps, weekSteps)
        }

        // 实时获取今日步数并监听变化，始终以传感器为准
        sensorManager.getTodaySteps { steps ->
            todaySteps = steps
            updateUI(todaySteps, weekSteps)
        }
        sensorManager.setOnStepChangedListener { newTodaySteps ->
            todaySteps = newTodaySteps
            // 重新异步统计一周步数
            getWeekSteps(weekDates) { total ->
                weekSteps = total
                updateUI(todaySteps, weekSteps)
            }
        }
    }

    /**
     * 设置每天0点重置步数基准的闹钟
     */
    private fun setupMidnightResetAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.zg.carbonapp.RESET_STEP_BASELINE")
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 设置每天0点执行
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 如果当前时间已过今天0点，则设置为明天0点
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 设置重复闹钟，每天执行一次
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * 异步获取本周7天真实步数总和
     *
     * 实现说明：
     * 1. 遍历本周7天的日期
     * 2. 对每个日期异步获取步数
     * 3. 累加所有天数的步数
     * 4. 当所有天数都获取完成后，回调总步数
     */
    private fun getWeekSteps(weekDates: List<String>, callback: (Int) -> Unit) {
        var total = 0
        var completed = 0
        
        // 遍历本周7天，获取每天的步数
        weekDates.forEach { date ->
            sensorManager.getStepsForDate(date) { steps ->
                total += steps
                completed++
                
                // 当所有7天都获取完成后，回调总步数
                if (completed == 7) {
                    callback(total)
                }
            }
        }
    }

    /**
     * 更新界面显示
     *
     * 显示内容：
     * - 今日步数
     * - 可种植的树木数量
     * - 好友排行榜
     * 
     * 交互功能：
     * - 点击步数跳转到详细页面
     * - 点击树木数量跳转到种树页面
     */
    private fun updateUI(todaySteps: Int, weekSteps: Int) {
        runOnUiThread {
            // 根据本周总步数计算可种植的树木数量
            // 计算逻辑：每步减少0.00004g碳，每棵树吸收0.5g碳
            val carbonReduction = weekSteps * 0.00004 // 克(g)
            val treeCount = (carbonReduction / 0.5).toInt()
            
            // 更新步数显示
            binding.tvStepCount.text = "$todaySteps 步"
            binding.tvTreeCount.text = "$treeCount 棵"
            
            // 设置步数点击事件，跳转到详细页面
            binding.tvStepCount.setOnClickListener {
                val intent = Intent(this, FootprintActivity::class.java)
                startActivity(intent)
            }
            
            // 设置树木数量点击事件，跳转到种树页面
            binding.tvTreeCount.setOnClickListener {
                val intent = Intent(this,CarbonFootprintActivity::class.java)
                startActivity(intent)
            }
            
            // 获取并显示好友排行榜
            val rawRankingList = fetchRankingFromServer()
            // 为每个用户计算树木数量并排序
            val displayRankingList = rawRankingList.map { user ->
                val userCarbonReduction = user.step * 0.00004 // 克(g)
                val userTreeCount = (userCarbonReduction / 0.5).toInt()
                user.copy(treeCount = userTreeCount)
            }.sortedByDescending { it.treeCount }
            
            // 设置排行榜适配器
            val rankingAdapter = FriendRankingAdapter(displayRankingList)
            binding.recyclerViewRanking.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewRanking.adapter = rankingAdapter
        }
    }
    
    /**
     * 获取本周7天的日期列表
     *
     * 实现说明：
     * 1. 获取当前日期是周几
     * 2. 计算本周一的日期
     * 3. 生成本周7天的日期列表
     */
    private fun getThisWeekDates(): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // 获取当前是周几
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // 计算本周一的日期
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dayOfWeek)
        
        // 生成本周7天的日期列表
        return (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            date
        }
    }
    
    // 从服务器获取指定日期的步数（待实现）
    private fun fetchStepFromServer(date: String): Int {
        // TODO: 实现后端接口
        return 12345
    }
    
    /**
     * 从服务器获取好友排行榜数据（待实现）
     *
     * TODO: 实现后端接口调用
     * 当前返回模拟数据用于测试
     */
    private fun fetchRankingFromServer(): List<FriendRanking> {
        return listOf(
            FriendRanking(id = 1, nickname = "小明", avatarResId = R.drawable.default_avatar, step = 12000),
            FriendRanking(id = 2, nickname = "小红", avatarResId = R.drawable.default_avatar, step = 8000),
            FriendRanking(id = 3, nickname = "小刚", avatarResId = R.drawable.default_avatar, step = 15000),
            FriendRanking(id = 4, nickname = "小美", avatarResId = R.drawable.default_avatar, step = 6000)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.releaseSensors()
        unregisterReceiver(resetReceiver)
    }
} 