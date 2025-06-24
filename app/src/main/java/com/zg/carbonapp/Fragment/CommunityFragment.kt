package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zg.carbonapp.databinding.FragmentCommunityBinding

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
//        setupListeners()
    }

    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.title = "碳社区"

        // 设置ViewPager2
        binding.viewPager.adapter = CommunityPagerAdapter(this)

        // 设置TabLayout
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "动态"
                1 -> "排行榜"
                else -> ""
            }
        }.attach()
    }

//    private fun setupListeners() {
//        // 发布按钮点击事件
//        binding.fabPost.setOnClickListener {
//            //
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class CommunityPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CommunityFeedFragment()
                1 -> CommunityRankingFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }
}
