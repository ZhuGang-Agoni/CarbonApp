//package com.zg.carbonapp.Activity
//
//import android.content.Context
//import android.hardware.SensorManager
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.GridLayoutManager
//import com.zg.carbonapp.Adapter.TreeIconAdapter
//import com.zg.carbonapp.databinding.ActivityPlantTreeBinding
//import com.zg.carbonapp.R
//
//import java.text.SimpleDateFormat
//import java.util.*
//
//
//class PlantTreeActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityPlantTreeBinding
//    // 传感器管理器，用于获取步数数据
//    private lateinit var sensorManager: SensorManager
//    // 本周日期列表
//    private lateinit var weekDates: List<String>
//    // 本周总步数
//    private var weekSteps: Int = 0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityPlantTreeBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // 初始化传感器管理器
//        // 替换原 new SensorManager() 的代码
//        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        sensorManager.initializeSensors()
//
//        // 获取本周日期列表
//        weekDates = getThisWeekDates()
//
//        // 异步获取本周步数并更新UI
//        getWeekSteps(weekDates) { total ->
//            weekSteps = total
//            updateUI(weekSteps)
//        }
//
//        // 实时监听本地步数变化，自动刷新一周统计
//        sensorManager.setOnStepChangedListener { _ ->
//            // 当步数发生变化时，重新计算本周总步数
//            getWeekSteps(weekDates) { total ->
//                weekSteps = total
//                updateUI(weekSteps)
//            }
//        }
//    }
//
//    /**
//     * 异步获取本周7天真实步数总和
//     *
//     * 实现说明：
//     * 1. 遍历本周7天的日期
//     * 2. 对每个日期异步获取步数
//     * 3. 累加所有天数的步数
//     * 4. 当所有天数都获取完成后，回调总步数
//     */
//    private fun getWeekSteps(weekDates: List<String>, callback: (Int) -> Unit) {
//        var total = 0
//        var completed = 0
//
//        // 遍历本周7天，获取每天的步数
//        weekDates.forEach { date ->
//            sensorManager.getStepsForDate(date) { steps ->
//                total += steps
//                completed++
//
//                // 当所有7天都获取完成后，回调总步数
//                if (completed == 7) {
//                    callback(total)
//                }
//            }
//        }
//    }
//
//    /**
//     * 更新界面显示
//     *
//     * 显示内容：
//     * - 本周总步数
//     * - 可种植的树木数量
//     * - 树木图标可视化显示
//     *
//     * 计算逻辑：
//     * 树木数量 = (本周总步数 * 0.00004) / 0.5
//     * - 0.00004：每步减少的碳排放量（克）
//     * - 0.5：每棵树吸收的碳排放量（克）
//     */
//    private fun updateUI(weekSteps: Int) {
//        // 根据步数计算可种植的树木数量
//        val carbonReduction = weekSteps * 0.00004 // 克(g)
//        val treeCount = (carbonReduction / 0.5).toInt()
//
//        runOnUiThread {
//            // 更新界面显示
//            binding.tvTotalSteps.text = "本周总步数：$weekSteps"
//            binding.tvTreeCount.text = "可种树：$treeCount 棵"
//
//            // 设置树木图标可视化显示
//            setupTreeVisualization(treeCount)
//        }
//    }
//
//    /**
//     * 设置树木可视化显示
//     *
//     * 功能说明：
//     * 1. 创建树木图标数据列表
//     * 2. 设置RecyclerView的布局管理器（网格布局）
//     * 3. 设置适配器显示树木图标
//     * 4. 根据树木数量显示对应数量的图标
//     */
//    private fun setupTreeVisualization(treeCount: Int) {
//        // 创建树木图标数据列表
//        val treeIcons = mutableListOf<Int>()
//
//        // 根据树木数量添加对应数量的图标
//        repeat(treeCount) {
//            treeIcons.add(R.drawable.ic_tree) // 使用树木图标资源
//        }
//
//        // 设置RecyclerView的布局管理器（网格布局，每行显示5个图标）
//        binding.recyclerViewTrees.layoutManager = GridLayoutManager(this, 5)
//
//        // 设置适配器显示树木图标
//        val treeAdapter = TreeIconAdapter(treeIcons)
//        binding.recyclerViewTrees.adapter = treeAdapter
//    }
//
//    // 获取今天的日期字符串
//    private fun today(): String {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        return sdf.format(Date())
//    }
//
//    /**
//     * 获取本周7天的日期列表
//     *
//     * 实现说明：
//     * 1. 获取当前日期是周几
//     * 2. 计算本周一的日期
//     * 3. 生成本周7天的日期列表
//     */
//    private fun getThisWeekDates(): List<String> {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val calendar = Calendar.getInstance()
//
//        // 获取当前是周几
//        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
//        // 计算本周一的日期
//        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dayOfWeek)
//
//        // 生成本周7天的日期列表
//        return (0..6).map {
//            val date = sdf.format(calendar.time)
//            calendar.add(Calendar.DAY_OF_YEAR, 1)
//            date
//        }
//    }
//
//    // 从服务器获取指定日期的步数（待实现）
//    private fun fetchStepFromServer(date: String): Int {
//        // TODO: 实现后端接口
//        return 12345
//    }
//
//    /**
//     * 界面销毁时释放传感器资源
//     */
//    override fun onDestroy() {
//        super.onDestroy()
//        sensorManager.releaseSensors()
//    }
//}