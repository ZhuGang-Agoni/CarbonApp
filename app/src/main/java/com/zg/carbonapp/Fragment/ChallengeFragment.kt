package com.zg.carbonapp.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Activity.BarcodeScannerActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.Activity.CarbonFootprintActivity
import com.zg.carbonapp.Activity.ElectricitySavingActivity
import com.zg.carbonapp.Activity.GreenTravelActivity
import com.zg.carbonapp.Activity.GarbageSortActivity

import com.zg.carbonapp.Adapter.ChallengeCardAdapter

import com.zg.carbonapp.databinding.FragmentChallengeBinding

class ChallengeFragment : Fragment() {
    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!

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
        // 挑战卡片数据
        val challengeList = listOf(
            ChallengeCard(0, "碳足迹", "记录你的碳排放，助力绿色生活", R.drawable.ic_footprint),
            ChallengeCard(1, "绿色出行", getString(R.string.desc_green_travel), R.drawable.ic_green_travel),
            ChallengeCard(2, "垃圾分类", getString(R.string.desc_garbage_sort), R.drawable.ic_garbage_sort),
            ChallengeCard(3, "节电挑战", getString(R.string.desc_power_saving), R.drawable.ic_power_saving),
            ChallengeCard(4,"食物識別","掃描二維碼，獲取相關信息",R.drawable.food)
        )
        val adapter = ChallengeCardAdapter(challengeList) { card ->
            val intent = when (card.id) {
                0 -> Intent(requireContext(), CarbonFootprintActivity::class.java)
                1 -> Intent(requireContext(), GreenTravelActivity::class.java)
                2 -> Intent(requireContext(), GarbageSortActivity::class.java)
                3 -> Intent(requireContext(), ElectricitySavingActivity::class.java)
                4->Intent(requireContext(),BarcodeScannerActivity::class.java)
                else -> null
            }
            intent?.let { startActivity(it) }
        }
        binding.recyclerViewChallenges.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChallenges.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ChallengeCard(val id: Int, val title: String, val description: String, val imageResId: Int)
}