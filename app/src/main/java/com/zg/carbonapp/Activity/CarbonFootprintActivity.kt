package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.FriendRankingAdapter
import com.zg.carbonapp.Dao.FriendRanking
import com.zg.carbonapp.MMKV.CarbonFootprintDataMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Service.SensorManager
import com.zg.carbonapp.databinding.ActivityCarbonFootprintBinding
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化传感器管理器
        sensorManager = SensorManager(this)
        sensorManager.initializeSensors()

        // 获取本周日期列表
        weekDates = getThisWeekDates()
        
        // 异步获取本周步数
        getWeekSteps(weekDates) { total ->
            weekSteps = total
            updateUI(todaySteps, weekSteps)
        }

        // 获取今日步数（本地缓存 > 后端 > 传感器）
        val today = getTodayDate()
        loadTodaySteps(today)

        // 实时监听本地步数变化，自动刷新今日步数和一周统计
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
     * 加载今日步数数据
     *
     * 数据获取策略：
     * 1. 优先从本地缓存获取
     * 2. 本地没有则从服务器获取
     * 3. 服务器没有则从传感器获取
     * 4. 获取到数据后保存到本地缓存
     */
    private fun loadTodaySteps(today: String) {
        // 检查本地缓存(MMKV)是否有今日步数
        if (CarbonFootprintDataMMKV.hasStep(today)) {
            todaySteps = CarbonFootprintDataMMKV.getStep(today)
            updateUI(todaySteps, weekSteps)
        } else {
            // 本地（MMKV）没有，尝试从服务器(后端)获取
            val stepFromServer = fetchStepFromServer(today)
            if (stepFromServer > 0) {
                // 服务器(后端)有数据，保存到本地（MMKV）缓存
                CarbonFootprintDataMMKV.saveStep(today, stepFromServer)
                todaySteps = stepFromServer
                updateUI(todaySteps, weekSteps)
            } else {
                // 服务器（后端）没有，从传感器获取
                sensorManager.getTodaySteps { localStep ->
                    CarbonFootprintDataMMKV.saveStep(today, localStep)
                    todaySteps = localStep
                    updateUI(todaySteps, weekSteps)
                }
            }
        }
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
                val intent = Intent(this, PlantTreeActivity::class.java)
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

    // 获取今天的日期字符串
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
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
            FriendRanking(id = 1, nickname = "小明", avatarResId = R.drawable.ic_profile, step = 12000),
            FriendRanking(id = 2, nickname = "小红", avatarResId = R.drawable.ic_profile, step = 8000),
            FriendRanking(id = 3, nickname = "小刚", avatarResId = R.drawable.ic_profile, step = 15000),
            FriendRanking(id = 4, nickname = "小美", avatarResId = R.drawable.ic_profile, step = 6000)
        )
    }
    
    /**
     * 界面销毁时释放传感器资源
     */
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.releaseSensors()
    }
} 