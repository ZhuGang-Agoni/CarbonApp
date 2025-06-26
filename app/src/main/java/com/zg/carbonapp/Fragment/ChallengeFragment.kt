package com.zg.carbonapp.Fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Adapter.ChallengeAdapter
import com.zg.carbonapp.Adapter.FriendRankingAdapter
import com.zg.carbonapp.Dao.Challenge
import com.zg.carbonapp.Dao.FriendRanking
import com.zg.carbonapp.R
import com.zg.carbonapp.Activity.FootprintActivity
import com.zg.carbonapp.Activity.PlantTreeActivity
import com.zg.carbonapp.databinding.FragmentChallengeBinding

class ChallengeFragment : Fragment() {

    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!
    // 示例挑战列表，实际可从服务器或本地获取
    private val challengeList = mutableListOf(
        Challenge(1,"步行挑战","每天步行1万步","连续7天",total = 7),
        Challenge(2,"节电挑战","本月用电量减少5%","30天",total = 30),
        Challenge(3,"低碳饮食","每周至少3天素食","4周",total = 4)
    )
    // private lateinit var adapter: ChallengeAdapter

    // MMKV 实例，用于本地持久化挑战数据
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val KEY_CHALLENGE_LIST = "challenge_list"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*
        // 1. 读取本地已参与挑战及进度
        loadJoinedChallengesFromMMKV()
        // 2. 初始化 Adapter，传入各类操作回调
        adapter = ChallengeAdapter(
            challengeList,
            onJoinCLick = { position ->
                challengeList[position].isJoined = true
                saveJoinedChallengesToMMKV()
                adapter.notifyItemChanged(position)
            },
            onCheckInClick = { position ->
                val challenge = challengeList[position]
                val today = getTodayString()
                if (challenge.lastCheckInDate != today && !challenge.isCompleted) {
                    challenge.progress += 1
                    challenge.lastCheckInDate = today
                    if (challenge.progress >= challenge.total) {
                        challenge.isCompleted = true
                    }
                    saveJoinedChallengesToMMKV()
                    adapter.notifyItemChanged(position)
                }
            },
            onRestartClick = { position ->
                val challenge = challengeList[position]
                challenge.progress = 0
                challenge.isCompleted = false
                challenge.lastCheckInDate = null
                saveJoinedChallengesToMMKV()
                adapter.notifyItemChanged(position)
            },
            onQuitClick = { position ->
                val challenge = challengeList[position]
                challenge.isJoined = false
                challenge.progress = 0
                challenge.isCompleted = false
                challenge.lastCheckInDate = null
                saveJoinedChallengesToMMKV()
                adapter.notifyItemChanged(position)
            }
        )
        binding.recyclerViewChallenge.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChallenge.adapter = adapter
        */

        // 好友排行榜数据
        val friendRankingList = listOf(
            FriendRanking(1, "小明", R.drawable.ic_profile, 12),
            FriendRanking(2, "小红", R.drawable.ic_profile, 10),
            FriendRanking(3, "小刚", R.drawable.ic_profile, 8),
            FriendRanking(4, "小美", R.drawable.ic_profile, 7)
        )
        val rankingAdapter = FriendRankingAdapter(friendRankingList.sortedByDescending { it.treeCount })
        binding.recyclerViewRanking.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRanking.adapter = rankingAdapter

        // 跳转到碳足迹界面
        binding.tvStepCount.setOnClickListener {
            val intent = Intent(requireContext(), FootprintActivity::class.java)
            startActivity(intent)
        }
        // 跳转到种树界面
        binding.tvTreeCount.setOnClickListener {
            val intent = Intent(requireContext(), PlantTreeActivity::class.java)
            startActivity(intent)
        }
    }

    /*
    private fun loadJoinedChallengesFromMMKV() {
        val json = mmkv.decodeString(KEY_CHALLENGE_LIST,"")
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Challenge>>() {}.type
            val list: MutableList<Challenge> = Gson().fromJson(json,type)
            challengeList.clear()
            challengeList.addAll(list)
        }
    }
    private fun saveJoinedChallengesToMMKV() {
        val json = Gson().toJson(challengeList)
        mmkv.encode(KEY_CHALLENGE_LIST, json)
    }
    private fun getTodayString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}