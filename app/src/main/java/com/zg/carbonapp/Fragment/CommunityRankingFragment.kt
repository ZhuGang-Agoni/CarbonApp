package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.RankingAdapter
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.MMKV.RankingItemMMKV
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R // 导入资源类
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentRankingBinding


//路径加载成功: /storage/emulated/0/Android/data/com.zg.carbonapp/files/Pictures/JPEG_20250821_174811_579703639522855974.jpg
class CommunityRankingFragment : Fragment() {
    private lateinit var binding: FragmentRankingBinding
    private lateinit var currentAdapter: RankingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRankingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 令牌检查
        val token = TokenManager.getToken()
        if (token == null) {
            MyToast.sendToast("令牌为空，请用户先行登录", requireActivity())
        }

        // 初始化列表 - 从MMKV获取数据
        val rankList = loadRankingData()
        currentAdapter = RankingAdapter(rankList, requireContext())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = currentAdapter
        }

        // 初始化下拉刷新
        initSwipeRefresh()
    }

    // 加载排行榜数据
    private fun loadRankingData(): List<RankingItem> {
        val mmkvList = RankingItemMMKV.getRankingItem()
        return if (mmkvList.isNotEmpty()) {
            Log.d("RankingFragment", "从MMKV加载排行榜数据，数量: ${mmkvList.size}")
            // 检查并修复已有数据中的错误头像路径
            mmkvList.forEach { item ->
                if (item.userEvator == "/R.drawable.img" || item.userEvator.isNullOrEmpty()) {
                    item.userEvator = getDefaultAvatarPath()
                }
            }
            mmkvList
        } else {
            Log.d("RankingFragment", "MMKV中无数据，使用模拟数据")
            val initialData = getInitialData()
            RankingItemMMKV.saveRankingItem(initialData)
            initialData
        }
    }

    // 初始化下拉刷新
    private fun initSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                refreshRankingData()
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1500)
        }
    }

    // 刷新排行榜数据
    private fun refreshRankingData() {
        val newData = getRefreshData()
        RankingItemMMKV.saveRankingItem(newData)
        currentAdapter.updateData(newData)
        MyToast.sendToast("已更新最新排行", requireActivity())
    }

    // 获取当前用户的真实头像
    private fun getCurrentUserAvatar(): String {
        val currentUser = UserMMKV.getUser()
        return if (currentUser != null && currentUser.userAvatar.isNotBlank()
            && currentUser.userAvatar != "/R.drawable.img") { // 排除错误路径
            Log.d("RankingFragment", "获取当前用户真实头像: ${currentUser.userAvatar}")
            currentUser.userAvatar
        } else {
            Log.d("RankingFragment", "当前用户无有效头像，使用默认头像")
            getDefaultAvatarPath()
        }
    }

    // 获取默认头像的正确引用（使用资源ID的字符串形式）
    private fun getDefaultAvatarPath(): String {
        // 返回默认头像的资源ID字符串，Glide可以识别这种格式
        return "android.resource://${requireContext().packageName}/${R.drawable.default_avatar}"
    }

    // 模拟刷新数据
    private fun getRefreshData(): List<RankingItem> {
        val userAvatar = getCurrentUserAvatar()
        return listOf(
            RankingItem("1", "Agoni", userAvatar, 120.0, 1),
            RankingItem("2", "Mona", userAvatar, 110.0, 2),
            RankingItem("3", "Carbon", userAvatar, 105.0, 3),
            RankingItem("4", "Eco", userAvatar, 90.0, 4), // 使用默认头像
            RankingItem("5", "Green", userAvatar, 80.0, 5) // 使用默认头像
        )
    }

    // 初始模拟数据
    private fun getInitialData(): List<RankingItem> {
        val userAvatar = getCurrentUserAvatar()
        return listOf(
            RankingItem("1", "Agoni", userAvatar, 100.0, 1),
            RankingItem("2", "Mona", userAvatar, 100.0, 2),
            RankingItem("3", "Carbon", userAvatar, 100.0, 3),
            RankingItem("4", "Eco", getDefaultAvatarPath(), 100.0, 4) // 使用默认头像
        )
    }
}
