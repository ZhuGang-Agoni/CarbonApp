package com.zg.carbonapp.Activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.ActivityAdapter
import com.zg.carbonapp.Dao.Activity
import com.zg.carbonapp.Dao.ActivityStatus
import com.zg.carbonapp.MMKV.ActivityMMKV
import com.zg.carbonapp.MMKV.CarbonPointsManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.TimeUtils
//import com.zg.carbonapp.Utils.TimeUtils
import com.zg.carbonapp.databinding.ActivityAllActivitiesBinding

class AllActivitiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllActivitiesBinding
    private lateinit var activityAdapter: ActivityAdapter
    private val allActivities = mutableListOf<Activity>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 标题栏
        binding.toolbar.title = "全部低碳活动"
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 加载数据
        loadAllActivities()

        // 初始化适配器
        activityAdapter = ActivityAdapter(allActivities,
            onJoinClick = { handleActivityJoin(it) },
            onCancelClick = { handleActivityCancel(it) }
        )
        binding.rvActivities.layoutManager = LinearLayoutManager(this)
        binding.rvActivities.adapter = activityAdapter

        // 筛选按钮
        binding.btnFilter.setOnClickListener { showFilterDialog() }

        // 定时刷新状态（每5分钟一次，避免频繁计算）
        startStatusRefreshTimer()
    }

    // 加载活动数据（无需设置status，完全动态计算）
    private fun loadAllActivities() {
        val user = UserMMKV.getUser()
        val userId = user?.userId ?: ""

        allActivities.add(
            Activity(
                id = 1,
                name = "城市骑行日挑战",
                description = "参与城市骑行活动，完成5公里骑行即可获得积分奖励",
                imageRes = R.drawable.ic_activity_cycling,
                startTime = "2025-08-25 09:00", // 近期活动
                endTime = "2025-08-25 16:00",
                location = "城市中央公园",
                points = 500,
                participantCount = 238,
                joined = ActivityMMKV.isActivityJoined(userId, 1)
            )
        )
        allActivities.add(
            Activity(
                id = 2,
                name = "垃圾分类公益讲座",
                description = "专业讲师讲解垃圾分类知识，参与互动问答可额外获得积分",
                imageRes = R.drawable.ic_activity_garbage,
                startTime = "2025-08-30 14:00", // 未开始
                endTime = "2025-08-30 16:00",
                location = "市民中心报告厅",
                points = 300,
                participantCount = 156,
                joined = ActivityMMKV.isActivityJoined(userId, 2)
            )
        )
        allActivities.add(
            Activity(
                id = 3,
                name = "植树节植树活动",
                description = "共同参与植树造林，每种植一棵树苗可获得高额积分奖励",
                imageRes = R.drawable.ic_activity_tree,
                startTime = "2025-03-12 08:00", // 已结束
                endTime = "2025-03-12 12:00",
                location = "城郊林场",
                points = 1000,
                participantCount = 521,
                joined = ActivityMMKV.isActivityJoined(userId, 3)
            )
        )
        allActivities.add(
            Activity(
                id = 4,
                name = "低碳生活创意大赛",
                description = "分享你的低碳生活小妙招，优秀作品可获得丰厚积分奖励",
                imageRes = R.drawable.ic_activity_creative,
                startTime = "2025-09-01 00:00",
                endTime = "2025-09-30 23:59",
                location = "线上参与",
                points = 800,
                participantCount = 76,
                joined = false
            )
        )
    }

    // 处理报名
    private fun handleActivityJoin(activity: Activity) {
        val user = UserMMKV.getUser() ?: run {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val index = allActivities.indexOfFirst { it.id == activity.id }
        if (index != -1) {
            val updatedActivity = activity.copy(
                joined = true,
                participantCount = activity.participantCount + 1
            )
            allActivities[index] = updatedActivity
            activityAdapter.notifyItemChanged(index)
        }

        ActivityMMKV.saveJoinedActivity(user.userId, activity.id)
        Toast.makeText(this, "报名成功！活动结束后将发放${activity.points}积分", Toast.LENGTH_SHORT).show()
    }

    // 处理取消报名（补全之前未完成的代码）
    private fun handleActivityCancel(activity: Activity) {
        val user = UserMMKV.getUser() ?: run {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val index = allActivities.indexOfFirst { it.id == activity.id }
        if (index != -1) {
            val newCount = maxOf(activity.participantCount - 1, 0)
            val updatedActivity = activity.copy(
                joined = false,
                participantCount = newCount
            )
            allActivities[index] = updatedActivity
            activityAdapter.notifyItemChanged(index)
        }

        ActivityMMKV.cancelActivityJoin(user.userId, activity.id)
        Toast.makeText(this, "已取消报名", Toast.LENGTH_SHORT).show()
    }

    // 筛选对话框（基于动态状态筛选）
    private fun showFilterDialog() {
        val statusOptions = arrayOf("全部活动", "进行中", "未开始", "已结束", "已报名")
        AlertDialog.Builder(this)
            .setTitle("筛选活动")
            .setItems(statusOptions) { _, which ->
                val userId = UserMMKV.getUser()?.userId ?: ""
                activityAdapter.filter { activity ->
                    when (which) {
                        0 -> true // 全部
                        1 -> TimeUtils.getActivityStatus(activity.startTime, activity.endTime) == ActivityStatus.ON_GOING
                        2 -> TimeUtils.getActivityStatus(activity.startTime, activity.endTime) == ActivityStatus.NOT_STARTED
                        3 -> TimeUtils.getActivityStatus(activity.startTime, activity.endTime) == ActivityStatus.ENDED
                        4 -> ActivityMMKV.isActivityJoined(userId, activity.id) // 已报名
                        else -> true
                    }
                }
            }
            .show()
    }

    // 定时刷新状态（每5分钟刷新一次列表，确保状态实时）
    private fun startStatusRefreshTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                activityAdapter.notifyDataSetChanged() // 刷新列表，重新计算状态
                checkAndIssuePoints() // 检查是否有活动结束，发放积分
                handler.postDelayed(this, 5 * 60 * 1000) // 5分钟一次
            }
        }, 0)
    }

    // 检查活动结束并发放积分
    private fun checkAndIssuePoints() {
        allActivities.forEach { activity ->
            if (TimeUtils.getActivityStatus(activity.startTime, activity.endTime) == ActivityStatus.ENDED) {
                val participants = ActivityMMKV.getJoinedUserIds(activity.id) ?: return@forEach
                participants.forEach { userId ->
                    CarbonPointsManager.addPoints(userId, activity.points)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
