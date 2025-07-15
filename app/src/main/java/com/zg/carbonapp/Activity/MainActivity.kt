package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.zg.carbonapp.Fragment.AskFragment
import com.zg.carbonapp.Fragment.ChallengeFragment
import com.zg.carbonapp.Fragment.CommunityFragment
import com.zg.carbonapp.Fragment.DataAnalyseFragment
import com.zg.carbonapp.Fragment.ImFragment
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tabLayout by lazy { binding.tabLayout }
    private val container by lazy { binding.container }
    private var currentTabPosition = 2 // 默认选中第3个Tab (AskFragment)
    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTableLayout()
        initListener()

        // 初始化AskFragment并显示
        if (savedInstanceState == null) {
            val firstFragment = AskFragment()
            fragments[currentTabPosition] = firstFragment
            supportFragmentManager.beginTransaction()
                .add(container.id, firstFragment)
                .show(firstFragment)
                .commit()

            // 确保TabLayout选中与Fragment一致的位置
            tabLayout.selectTab(tabLayout.getTabAt(currentTabPosition))
        }

        handleShareIntent()
    }

    private fun handleShareIntent() {
        // 处理分享逻辑
    }

    private fun initTableLayout() {
        // 动态添加 tabs
        val askTab = tabLayout.newTab().setIcon(R.drawable.ic_ai_assistant).setText("低碳ai助手")
        val challengeTab = tabLayout.newTab().setIcon(R.drawable.ic_challenge).setText("减排挑战")
        val communityTab = tabLayout.newTab().setIcon(R.drawable.ic_community).setText("碳社区")
        val imTab = tabLayout.newTab().setIcon(R.drawable.ic_profile).setText("我的主页")
        val dataTab = tabLayout.newTab().setIcon(R.drawable.data).setText("数据分析")

        // 按指定顺序添加Tabs
        tabLayout.addTab(challengeTab)  // position 0
        tabLayout.addTab(dataTab)       // position 1
        tabLayout.addTab(askTab)        // position 2 (默认选中)
        tabLayout.addTab(communityTab)  // position 3
        tabLayout.addTab(imTab)         // position 4
    }

    private fun initListener() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (currentTabPosition == tab.position) return

                val fragment = fragments.getOrPut(tab.position) {
                    when (tab.position) {
                        0 -> ChallengeFragment()
                        1 -> DataAnalyseFragment()
                        2 -> AskFragment()
                        3 -> CommunityFragment()
                        4 -> ImFragment()
                        else -> throw IllegalStateException("Invalid position")
                    }
                }

                switchFragment(fragment)
                currentTabPosition = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // 可选：处理未选中事件
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // 可选：处理重新选中事件
            }
        })
    }

    private fun switchFragment(newFragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        fragments[currentTabPosition]?.let {
            transaction.hide(it)
        }

        if (newFragment.isAdded) {
            transaction.show(newFragment)
        } else {
            transaction.add(container.id, newFragment)
            transaction.show(newFragment)
        }

        transaction.commit()
    }
}