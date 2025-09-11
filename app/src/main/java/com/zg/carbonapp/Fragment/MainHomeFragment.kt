package com.zg.carbonapp.Fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zg.carbonapp.Activity.*
import com.zg.carbonapp.Adapter.ActivityAdapter
import com.zg.carbonapp.Adapter.BadgeAdapter
import com.zg.carbonapp.Adapter.LowCarbonKnowledgeAdapter
import com.zg.carbonapp.Dao.*
import com.zg.carbonapp.MMKV.*
import com.zg.carbonapp.R
import com.zg.carbonapp.Repository.LowCarbonKnowledgeRepository
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.databinding.DialogBadgeListBinding
import com.zg.carbonapp.databinding.FragmentMainHomeBinding

class MainHomeFragment : Fragment() {

    private var _binding: FragmentMainHomeBinding? = null
    private val binding get() = _binding!!
    private val knowledgeList: List<LowCarbonKnowledge> by lazy {
        LowCarbonKnowledgeRepository.getLottieList()
    }

    // 活动相关变量
    private lateinit var activityAdapter: ActivityAdapter
    private val activityList = mutableListOf<Activity>()

    private companion object {
        val TYPE_FRAME = ProductType.AVATAR_FRAME // 头像框
        val TYPE_ACCESSORY = ProductType.AVATAR_ITEM // 挂件
    }

    private val userObserver = Observer<User?> { user ->
        user?.let {
            refreshUserAvatar() // 数据变化时立即刷新头像
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadInitialData()
        initView()
        initMedalDisplay() // 初始化勋章显示
        setupMedalClickListener() // 勋章点击事件
        UserMMKV.userLiveData.observe(viewLifecycleOwner, userObserver)
        refreshAvatarDecorations()

        checkAndPromptSignIn()

        // 初始化活动区域
        initActivitySection()

        binding.qiandao.setOnClickListener {
            IntentHelper.goIntent(requireContext(), SignInActivity::class.java)
        }
    }

    // 初始化活动区域
    private fun initActivitySection() {
        // 模拟活动数据
        loadActivityData()

        // 初始化适配器（包含取消报名回调）
        activityAdapter = ActivityAdapter(activityList as MutableList<Activity>,
            onJoinClick = { activity -> handleActivityJoin(activity) },
            onCancelClick = { activity -> handleActivityCancel(activity) }
        )

        // 设置RecyclerView
        binding.activityRecyclerView.adapter = activityAdapter
        binding.activityRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )

        // 查看全部活动点击事件
        binding.seeAllActivity.setOnClickListener {
            IntentHelper.goIntent(requireContext(), AllActivitiesActivity::class.java)
            showToast("跳转到全部活动列表")
        }
    }

    // 取消报名处理方法
    private fun handleActivityCancel(activity: Activity) {
        val user = UserMMKV.getUser() ?: return

        // 1. 更新本地数据状态（参与人数-1）
        val index = activityList.indexOfFirst { it.id == activity.id }
        if (index != -1) {
            val newCount = maxOf(activity.participantCount - 1, 0)
            activityList[index] = activity.copy(
                joined = false,
                participantCount = newCount
            )
            activityAdapter.notifyItemChanged(index)
        }

        // 2. 移除报名记录
        ActivityMMKV.cancelActivityJoin(user.userId, activity.id)

        // 3. 提示用户
        Toast.makeText(requireContext(), "已取消报名", Toast.LENGTH_SHORT).show()
    }

    // 报名处理方法（同步人数更新）
    private fun handleActivityJoin(activity: Activity) {
        val user = UserMMKV.getUser() ?: return

        // 1. 更新本地数据状态（参与人数+1）
        val index = activityList.indexOfFirst { it.id == activity.id }
        if (index != -1) {
            activityList[index] = activity.copy(
                joined = true,
                participantCount = activity.participantCount + 1
            )
            activityAdapter.notifyItemChanged(index)
        }

        // 2. 保存报名记录
        ActivityMMKV.saveJoinedActivity(user.userId, activity.id)

        // 3. 提示用户
        Toast.makeText(
            requireContext(),
            "报名成功！活动结束后将发放${activity.points}积分",
            Toast.LENGTH_SHORT
        ).show()
    }

    // 模拟加载活动数据（移除status字段，状态由TimeUtils动态计算）
    private fun loadActivityData() {
        activityList.add(
            Activity(
                id = 1,
                name = "城市骑行日挑战",
                description = "参与城市骑行活动，完成5公里骑行即可获得积分奖励，减少碳排放从我做起",
                imageRes = R.drawable.activity_city_cycling,
                startTime = "2025-06-15 09:00",
                endTime = "2025-06-15 16:00",
                location = "城市中央公园",
                points = 500,
                participantCount = 238,
                joined = false
            )
        )
        activityList.add(
            Activity(
                id = 2,
                name = "垃圾分类公益讲座",
                description = "专业讲师讲解垃圾分类知识，参与互动问答可额外获得积分",
                imageRes = R.drawable.activity_garbage_lectrue,
                startTime = "2025-06-20 14:00",
                endTime = "2025-06-20 16:00",
                location = "市民中心报告厅",
                points = 300,
                participantCount = 156,
                joined = true
            )
        )
    }


    private fun checkAndPromptSignIn() {
        val user = UserMMKV.getUser()
        if (user == null) {
            return
        }

        if (!SignInManager.isTodaySigned()) {
            showSignInPromptDialog()
        }
    }

    private fun showSignInPromptDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("今日签到提醒")
        builder.setMessage("今日还未签到，前往签到页面完成签到可获取碳积分哦～")
        builder.setPositiveButton("前往签到") { dialog, _ ->
            IntentHelper.goIntent(requireContext(), SignInActivity::class.java)
            dialog.dismiss()
        }
        builder.setNegativeButton("暂不") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    // 合并任务和商城的勋章数据
    private fun getAllUnlockedBadges(userId: String): List<VirtualProduct> {
        // 获取任务解锁的勋章
        val taskBadges = AchievementProductManager.getAchievementUnlockedProducts(userId)
            .filter { it.type == ProductType.BADGE }
        // 获取商城购买的勋章
        val mallBadges = UserAssetsManager.getUnlockedProducts(userId)
            .filter { it.type == ProductType.BADGE }
        // 合并去重（根据id）
        return (taskBadges + mallBadges).distinctBy { it.id }
    }

    // 合并任务和商城的头像框数据
    private fun getAllUnlockedFrames(userId: String): List<VirtualProduct> {
        val taskFrames = AchievementProductManager.getAchievementUnlockedProducts(userId)
            .filter { it.type == ProductType.AVATAR_FRAME }
        val mallFrames = UserAssetsManager.getUnlockedProducts(userId)
            .filter { it.type == ProductType.AVATAR_FRAME }
        return (taskFrames + mallFrames).distinctBy { it.id }
    }

    // 合并任务和商城的挂件数据
    private fun getAllUnlockedAccessories(userId: String): List<VirtualProduct> {
        val taskAccessories = AchievementProductManager.getAchievementUnlockedProducts(userId)
            .filter { it.type == ProductType.AVATAR_ITEM }
        val mallAccessories = UserAssetsManager.getUnlockedProducts(userId)
            .filter { it.type == ProductType.AVATAR_ITEM }
        return (taskAccessories + mallAccessories).distinctBy { it.id }
    }

    private fun initMedalDisplay() {
        val user = UserMMKV.getUser()
        val userId = user?.userId

        binding.medalIcon.visibility = View.GONE
        binding.medalText.text = "无"

        if (userId == null) {
            binding.medalText.text = "请登录查看勋章"
            return
        }

        // 使用合并后的勋章列表
        val allUnlockedBadges = getAllUnlockedBadges(userId)

        if (allUnlockedBadges.isEmpty()) {
            binding.medalText.text = "未获得勋章"
            binding.medalIcon.visibility = View.GONE
            return
        }

        val currentBadgeId = UserAssetsManager.getCurrentBadgeId(userId)
        val currentBadge = allUnlockedBadges.firstOrNull { it.id == currentBadgeId } ?: allUnlockedBadges[0]

        binding.medalIcon.visibility = View.VISIBLE
        binding.medalIcon.setImageResource(currentBadge.iconRes)
        binding.medalText.text = "${currentBadge.name}"
    }

    private fun setupMedalClickListener() {
        binding.userMedal.setOnClickListener {
            val user = UserMMKV.getUser()
            val userId = user?.userId

            if (userId == null) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 使用合并后的勋章列表
            val allUnlockedBadges = getAllUnlockedBadges(userId)

            if (allUnlockedBadges.isEmpty()) {
                Toast.makeText(requireContext(), "暂无解锁勋章", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showBadgeDialog(userId, allUnlockedBadges)
        }
    }

    private fun showBadgeDialog(userId: String, badges: List<VirtualProduct>) {
        val currentBadgeId = UserAssetsManager.getCurrentBadgeId(userId)
        val dialogBinding = DialogBadgeListBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.rvBadges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = BadgeAdapter(badges, currentBadgeId) { selectedBadge ->
                showConfirmDialog(userId, selectedBadge)
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .show()
    }

    // 修改 showConfirmDialog 方法中的确定按钮逻辑
    private fun showConfirmDialog(userId: String, selectedBadge: VirtualProduct) {
        AlertDialog.Builder(requireContext())
            .setTitle("切换勋章")
            .setMessage("确定将当前勋章切换为【${selectedBadge.name}】吗？")
            .setPositiveButton("确定") { _, _ ->
                // 1. 更新存储的当前勋章ID
                UserAssetsManager.setCurrentBadgeId(userId, selectedBadge.id)
                // 2. 立即更新勋章名称
                binding.medalText.text = selectedBadge.name
                // 3. 立即更新勋章图标
                binding.medalIcon.setImageResource(selectedBadge.iconRes)
                // 4. 显示提示
                Toast.makeText(requireContext(), "勋章已切换", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun initView() {
        binding.lowCarbonKnowledgeRv.adapter = LowCarbonKnowledgeAdapter(requireContext(), knowledgeList)
        binding.lowCarbonKnowledgeRv.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        val travelRecord = TravelRecordManager.getRecords()
        if (travelRecord != null) {
            val totalReduceCarbon = travelRecord.todayCarbon
            binding.reduceCarbon.text = totalReduceCarbon
        }

        binding.plantTree.setOnClickListener {
            // 种树逻辑
            showToast("前往种树页面")
        }

        val rankList = RankingItemMMKV.getRankingItem()
        val user = UserMMKV.getUser()

        if (user != null && rankList != null) {
            for ((index, item) in rankList.withIndex()) {
                if (item.isCurrentUser) {
                    val rank = index + 1
                    binding.rank.text = "排名: ${rank}/${rankList.size}"
                    break
                }
            }
        } else {
            binding.rank.text = "暂无排名数据"
        }

        val travelRecord1 = TravelRecordManager.getRecords()
        val totalReduceCarbon = travelRecord1.totalCarbon
        binding.userCarbonInfo.text = "总共已减排${totalReduceCarbon} kg"
    }

    override fun onResume() {
        super.onResume()
        refreshUserAvatar()
    }

    private fun refreshUserAvatar() {
        val user = UserMMKV.getUser()
        user?.let {
            if (it.userAvatar.isNotEmpty()) {
                val avatarUri = Uri.parse(it.userAvatar)
                Glide.with(this)
                    .load(avatarUri)
                    .error(R.drawable.default_avatar)
                    .into(binding.userAvatar)
            } else {
                binding.userAvatar.setImageResource(R.drawable.default_avatar)
            }
        } ?: run {
            binding.userAvatar.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun setupClickListeners() {
        // 头像点击事件
        binding.userAvatar.setOnClickListener { showAvatarOptionsDialog() }
        binding.userInfoSection.setOnClickListener { navigateToUserProfile() }
        binding.notificationIcon.setOnClickListener { navigateToNotifications() }

        binding.functionTitle.setOnClickListener { /* 无逻辑 */ }
        setupFunctionGridClickListeners()

        binding.activityTitle.setOnClickListener { navigateToActivities() }


        binding.updateIcon.setOnClickListener {
            val newKnowledgeList = LowCarbonKnowledgeRepository.getLottieList()
            binding.lowCarbonKnowledgeRv.adapter?.let { adapter ->
                if (adapter is LowCarbonKnowledgeAdapter) {
                    adapter.updateData(newKnowledgeList)
                    adapter.notifyDataSetChanged()
                }
            }
            binding.lowCarbonKnowledgeRv.smoothScrollToPosition(0)
        }
    }

    private fun showAvatarOptionsDialog() {
        val user = UserMMKV.getUser() ?: run {
            navigateToUserProfile() // 未登录时直接跳转个人主页
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("头像设置")
            .setItems(arrayOf("更换头像框", "更换挂件", "前往个人主页")) { dialog, which ->
                when (which) {
                    0 -> showFrameDialog(user.userId, getAllUnlockedFrames(user.userId))
                    1 -> showAccessoryDialog(user.userId, getAllUnlockedAccessories(user.userId))
                    2 -> navigateToUserProfile()
                }
            }
            .show()
    }

    private fun setupFunctionGridClickListeners() {
        if (binding.functionGrid.childCount > 0 && binding.functionGrid.getChildAt(0) is ViewGroup) {
            val firstRow = binding.functionGrid.getChildAt(0) as ViewGroup
            firstRow.getChildAt(0).setOnClickListener { navigateToGreenTravel() }
            firstRow.getChildAt(1).setOnClickListener { navigateToEnergyStatistics() }
            firstRow.getChildAt(2).setOnClickListener { navigateToGarbageClassification() }
            firstRow.getChildAt(3).setOnClickListener { navigateToGreenMall() }
        }

        if (binding.functionGrid.childCount > 1 && binding.functionGrid.getChildAt(1) is ViewGroup) {
            val secondRow = binding.functionGrid.getChildAt(1) as ViewGroup
            secondRow.getChildAt(0).setOnClickListener { navigateToCarbonFootprint() }
            secondRow.getChildAt(1).setOnClickListener { navigateToMoreFunctions() }
            secondRow.getChildAt(2).setOnClickListener { navigateToVRScene() }
        }
    }

    private fun navigateToVRScene() {
        IntentHelper.goIntent(requireContext(),MyActionActivity::class.java)
    }

    private fun navigateToUserProfile() {
        showToast("跳转到用户个人资料页")
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.switchToSpecificTab(4)
        } else {
            showToast("跳转失败，请重试")
        }
    }

    private fun navigateToNotifications() {
        showToast("跳转到通知页")
    }

    private fun navigateToGreenTravel() {
        IntentHelper.goIntent(requireContext(), GreenTravelActivity::class.java)
        showToast("跳转到绿色出行页")
    }

    private fun navigateToEnergyStatistics() {
        IntentHelper.goIntent(requireContext(), TravelStatisticActivity::class.java)
        showToast("跳转到节能统计页")
    }

    private fun navigateToGarbageClassification() {
        IntentHelper.goIntent(requireContext(),GarbageSortActivity::class.java)
        showToast("跳转到垃圾分类页")
    }

    private fun navigateToGreenMall() {
        IntentHelper.goIntent(requireContext(), ShoppingActivity::class.java)
        showToast("跳转到绿色商城页")
    }

    private fun navigateToCarbonFootprint() {
        IntentHelper.goIntent(requireContext(), StepCarbonActivity::class.java)
        showToast("跳转到碳足迹计算页")
    }

    private fun navigateToMoreFunctions() {
        IntentHelper.goIntent(requireContext(), TravelHelperActivity::class.java)
        showToast("跳转到更多功能页")
    }

    private fun navigateToActivities() {
        showToast("跳转到活动列表")
    }

    private fun navigateToActivityDetail() {
        showToast("跳转到活动详情")
    }

    private fun registerForActivity() {
        showToast("处理活动报名")
    }

    private fun loadInitialData() {
        loadUserData()
        loadLowCarbonData()
        loadRecommendedContent()
        loadLowCarbonActivities()
    }

    private fun loadUserData() {
         binding.userName.text="用户名字："+UserMMKV.getUser()?.userName
    }
    private fun loadLowCarbonData() {}
    private fun loadRecommendedContent() {}
    private fun loadLowCarbonActivities() {}

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.avatarFrameOuter.clearAnimation()
        binding.avatarAccessory.clearAnimation()
        UserMMKV.userLiveData.removeObserver(userObserver)
        _binding = null
    }

    // 刷新头像装饰（合并任务和商城商品）
    private fun refreshAvatarDecorations() {
        val user = UserMMKV.getUser()
        val userId = user?.userId ?: return

        // 头像框逻辑（合并任务和商城数据）
        val allUnlockedFrames = getAllUnlockedFrames(userId)
        val currentFrameId = UserAssetsManager.getCurrentFrameId(userId)
        val currentFrame = allUnlockedFrames.firstOrNull { it.id == currentFrameId }

        if (currentFrame != null) {
            binding.avatarFrameInner.visibility = View.VISIBLE
            binding.avatarFrameOuter.visibility = View.VISIBLE
            binding.avatarFrameInner.setImageResource(currentFrame.iconRes)
            binding.avatarFrameOuter.setImageResource(currentFrame.iconRes)
        } else {
            binding.avatarFrameInner.visibility = View.GONE
            binding.avatarFrameOuter.visibility = View.GONE
        }

        // 挂件逻辑（合并任务和商城数据）
        val allUnlockedAccessories = getAllUnlockedAccessories(userId)
        val currentAccessoryId = UserAssetsManager.getCurrentAccessoryId(userId)
        val currentAccessory = allUnlockedAccessories.firstOrNull { it.id == currentAccessoryId }

        if (currentAccessory != null) {
            binding.avatarAccessory.visibility = View.VISIBLE
            binding.avatarAccessory.setImageResource(currentAccessory.iconRes)
        } else {
            binding.avatarAccessory.visibility = View.GONE
        }
    }

    private fun setCurrentFrameId(userId: String, frameId: Int) {
        UserAssetsManager.setCurrentFrameId(userId, frameId)
        refreshAvatarDecorations()
    }

    private fun setCurrentAccessoryId(userId: String, accessoryId: Int) {
        UserAssetsManager.setCurrentAccessoryId(userId, accessoryId)
        refreshAvatarDecorations()
    }

    private fun getCurrentFrameId(userId: String): Int {
        return UserAssetsManager.getCurrentFrameId(userId)
    }

    private fun getCurrentAccessoryId(userId: String): Int {
        return UserAssetsManager.getCurrentAccessoryId(userId)
    }

    private fun showFrameDialog(userId: String, frames: List<VirtualProduct>) {
        if (frames.isEmpty()) {
            Toast.makeText(requireContext(), "暂无解锁头像框", Toast.LENGTH_SHORT).show()
            return
        }

        val currentFrameId = getCurrentFrameId(userId)
        val dialogBinding = DialogBadgeListBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.rvBadges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = BadgeAdapter(frames, currentFrameId) { selectedFrame ->
                showFrameConfirmDialog(userId, selectedFrame)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("我的头像框")
            .setView(dialogBinding.root)
            .show()
    }

    private fun showFrameConfirmDialog(userId: String, selectedFrame: VirtualProduct) {
        AlertDialog.Builder(requireContext())
            .setTitle("切换头像框")
            .setMessage("确定将当前头像框更换为【${selectedFrame.name}】吗？")
            .setPositiveButton("确定") { _, _ ->
                setCurrentFrameId(userId, selectedFrame.id)
                Toast.makeText(requireContext(), "头像框已切换", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAccessoryDialog(userId: String, accessories: List<VirtualProduct>) {
        if (accessories.isEmpty()) {
            Toast.makeText(requireContext(), "暂无解锁挂件", Toast.LENGTH_SHORT).show()
            return
        }

        val currentAccessoryId = getCurrentAccessoryId(userId)
        val dialogBinding = DialogBadgeListBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.rvBadges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = BadgeAdapter(accessories, currentAccessoryId) { selectedAccessory ->
                showAccessoryConfirmDialog(userId, selectedAccessory)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("我的挂件")
            .setView(dialogBinding.root)
            .show()
    }

    private fun showAccessoryConfirmDialog(userId: String, selectedAccessory: VirtualProduct) {
        AlertDialog.Builder(requireContext())
            .setTitle("切换挂件")
            .setMessage("确定将当前挂件更换为【${selectedAccessory.name}】吗？")
            .setPositiveButton("确定") { _, _ ->
                setCurrentAccessoryId(userId, selectedAccessory.id)
                Toast.makeText(requireContext(), "挂件已切换", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}