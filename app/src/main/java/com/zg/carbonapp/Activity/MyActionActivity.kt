package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zg.carbonapp.Adapter.UserActivityPagerAdapter
import com.zg.carbonapp.Fragment.LikedFeedsFragment
//import com.zg.carbonapp.Fragment.SavesFeedsFragment
import com.zg.carbonapp.Fragment.CommentedFeedsFragment
import com.zg.carbonapp.Fragment.CommunityFeedFragment
import com.zg.carbonapp.Fragment.CommunityRankingFragment
import com.zg.carbonapp.Fragment.SavesFeedsFragment
import com.zg.carbonapp.databinding.ActivityMyActionBinding


class MyActionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyActionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }


    private fun setupUI() {

        // 设置ViewPager2
        binding.viewPager.adapter = MyActionPagerAdapter(this)

        // 设置TabLayout
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "我的点赞"
                1 -> "我的收藏"
                2 -> "我的评论"
                else->""
            }
        }.attach()
    }

    private inner class MyActionPagerAdapter(fragmentActivity: AppCompatActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LikedFeedsFragment()
                1 -> SavesFeedsFragment()
                2 -> CommentedFeedsFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }
}