package com.zg.carbonapp.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Activity.CarbonFootprintActivity
import com.zg.carbonapp.Activity.EnergyAssistantActivity
import com.zg.carbonapp.Activity.GarbageSortActivity
import com.zg.carbonapp.Activity.GreenTravelActivity
import com.zg.carbonapp.Activity.ShoppingActivity
import com.zg.carbonapp.Activity.TravelStatisticActivity
import com.zg.carbonapp.Adapter.LowCarbonKnowledgeAdapter
import com.zg.carbonapp.Dao.LowCarbonKnowledge
import com.zg.carbonapp.MMKV.CarbonFootprintDataMMKV
import com.zg.carbonapp.MMKV.RankingItemMMKV
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Repository.LowCarbonKnowledgeRepository
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.databinding.FragmentMainHomeBinding
import kotlinx.serialization.descriptors.PrimitiveKind


class MainHomeFragment : Fragment() {

    private var _binding: FragmentMainHomeBinding? = null
    private val binding get() = _binding!!
    private val knowledgeList:List<LowCarbonKnowledge> by lazy {
        LowCarbonKnowledgeRepository.getRandomLowCarbonKnowledge()
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
    }

    private fun initView(){
        binding.lowCarbonKnowledgeRv.adapter=LowCarbonKnowledgeAdapter(requireContext(),
            knowledgeList)

        binding.lowCarbonKnowledgeRv.layoutManager=LinearLayoutManager(
            requireContext(),LinearLayoutManager.HORIZONTAL,false
        )

//         这个其实是属于那个啥 我也很无语数据概览卡片
//        减排量的数据要从 TravelRecordManager里面获取
        val travelRecord=TravelRecordManager.getRecords()
        if (travelRecord!=null){
            val totalReduceCarbon=travelRecord.todayCarbon
            binding.reduceCarbon.text=totalReduceCarbon
        }

        // binding.tree 暂时弄不了 他没把Tree保存起来 弄不了
//        binding.plantTree

        val rankList = RankingItemMMKV.getRankingItem()
        val user = UserMMKV.getUser()

        if (user != null && rankList != null) {
            for ((index, item) in rankList.withIndex()) {
                // 这里我们暂时先不用id
                if (item.isCurrentUser) {
                    val rank = index + 1
                    val percentage = (rank.toDouble() / rankList.size) * 100
                    binding.rank.text = "排名: ${rank}/${rankList.size}"
                    break
                }
            }
        } else {
            binding.rank.text = "暂无排名数据"
        }

        val travelRecord1=TravelRecordManager.getRecords()
        val totalReduceCarbon=travelRecord1.totalCarbon
        binding.userCarbonInfo.text="总共已减排${totalReduceCarbon} kg"

    }

    private fun setupClickListeners() {
        // 用户信息区
        binding.userInfoSection.setOnClickListener { navigateToUserProfile() }
        binding.userAvatar.setOnClickListener { navigateToUserProfile() }
        binding.userName.setOnClickListener { navigateToUserProfile() }
        binding.notificationIcon.setOnClickListener { navigateToNotifications() }


        // 功能导航区
        binding.functionTitle.setOnClickListener { /* 标题点击可能不需要跳转 */ }
        setupFunctionGridClickListeners()

        // 活动推荐区
        binding.activityTitle.setOnClickListener { navigateToActivities() }
        binding.activityCard.setOnClickListener { navigateToActivityDetail() }
        binding.activityCard.findViewById<Button>(R.id.registerButton).setOnClickListener {
            registerForActivity()
        }

        binding.updateIcon.setOnClickListener{
            val newKnowledgeList = LowCarbonKnowledgeRepository.getRandomLowCarbonKnowledge()

            // 更新适配器
            binding.lowCarbonKnowledgeRv.adapter?.let { adapter ->
                if (adapter is LowCarbonKnowledgeAdapter) {
                    adapter.updateData(newKnowledgeList)
                    adapter.notifyDataSetChanged()
                }
            }

            // 添加平滑滚动效果
            binding.lowCarbonKnowledgeRv.smoothScrollToPosition(0)
        }
    }

    private fun setupFunctionGridClickListeners() {
        // 第一行功能项
        if (binding.functionGrid.childCount > 0 && binding.functionGrid.getChildAt(0) is ViewGroup) {
            val firstRow = binding.functionGrid.getChildAt(0) as ViewGroup
            firstRow.getChildAt(0).setOnClickListener { navigateToGreenTravel() }
            firstRow.getChildAt(1).setOnClickListener { navigateToEnergyStatistics() }
            firstRow.getChildAt(2).setOnClickListener { navigateToGarbageClassification() }
            firstRow.getChildAt(3).setOnClickListener { navigateToGreenMall() }
        }

        // 第二行功能项
        if (binding.functionGrid.childCount > 1 && binding.functionGrid.getChildAt(1) is ViewGroup) {
            val secondRow = binding.functionGrid.getChildAt(1) as ViewGroup
            secondRow.getChildAt(0).setOnClickListener { navigateToCarbonFootprint() }
            secondRow.getChildAt(1).setOnClickListener { navigateToMoreFunctions() }
            secondRow.getChildAt(2).setOnClickListener{navigateToVRScene()}
            // 空白占位项不设置点击事件
        }
    }


    private fun navigateToVRScene(){
        // 这里实际一点还要那个啥 等待接进来

        Toast.makeText(requireContext(),"点击了低碳VR场景体验",Toast.LENGTH_SHORT).show()
    }
    // 用户相关导航
    private fun navigateToUserProfile() {
        // 示例：跳转到用户个人资料页
        showToast("跳转到用户个人资料页")
        // startActivity(Intent(requireContext(), UserProfileActivity::class.java))
    }

    private fun navigateToNotifications() {
        // 示例：跳转到通知页
        showToast("跳转到通知页")
    }
    // 功能导航相关
    private fun navigateToGreenTravel() {
        // 示例：跳转到绿色出行页
        IntentHelper.goIntent(requireContext(),GreenTravelActivity::class.java)
        showToast("跳转到绿色出行页")
    }

    private fun navigateToEnergyStatistics() {
        // 示例：跳转到节能统计页
       IntentHelper.goIntent(requireContext(),TravelStatisticActivity::class.java)
        showToast("跳转到节能统计页")
    }

    private fun navigateToGarbageClassification() {
        // 示例：跳转到垃圾分类页
        IntentHelper.goIntent(requireContext(),GarbageSortActivity::class.java)
        showToast("跳转到垃圾分类页")
    }

    private fun navigateToGreenMall() {
        // 示例：跳转到绿色商城页
        IntentHelper.goIntent(requireContext(),ShoppingActivity::class.java)
        showToast("跳转到绿色商城页")
    }

    private fun navigateToCarbonFootprint() {
        // 示例：跳转到碳足迹计算页
        IntentHelper.goIntent(requireContext(),CarbonFootprintActivity::class.java)
        showToast("跳转到碳足迹计算页")
    }

    private fun navigateToMoreFunctions() {
        // 示例：跳转到更多功能页
        showToast("跳转到更多功能页")
    }

    // 推荐内容相关
    private fun navigateToRecommendations() {
        // 示例：跳转到推荐内容列表
        showToast("跳转到推荐内容列表")
    }

    // 活动相关
    private fun navigateToActivities() {
        // 示例：跳转到活动列表
        showToast("跳转到活动列表")
    }

    private fun navigateToActivityDetail() {
        // 示例：跳转到活动详情
        showToast("跳转到活动详情")
    }

    private fun registerForActivity() {
        // 示例：处理活动报名
        showToast("处理活动报名")
    }

    private fun loadInitialData() {
        // 加载初始数据（示例方法，实际项目中实现具体逻辑）
        loadUserData()
        loadLowCarbonData()
        loadRecommendedContent()
        loadLowCarbonActivities()
    }

    private fun loadUserData() {
        // 加载用户数据逻辑
    }

    private fun loadLowCarbonData() {
        // 加载低碳数据逻辑
    }

    private fun loadRecommendedContent() {
        // 加载推荐内容逻辑
    }

    private fun loadLowCarbonActivities() {
        // 加载低碳活动逻辑
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}