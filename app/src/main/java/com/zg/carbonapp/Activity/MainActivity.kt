package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.zg.carbonapp.Fragment.AskFragment
import com.zg.carbonapp.Fragment.CommunityFragment
import com.zg.carbonapp.Fragment.DataAnalyseFragment
import com.zg.carbonapp.Fragment.ImFragment
import com.zg.carbonapp.Fragment.MainHomeFragment
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tabLayout by lazy { binding.tabLayout }
    private val container by lazy { binding.container }
    private var currentTabPosition = 0// 默认选中第3个Tab (AskFragment)
    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTableLayout()
        initListener()

        // 初始化AskFragment并显示
        if (savedInstanceState == null) {
            val firstFragment = MainHomeFragment()
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
    fun switchToSpecificTab(position: Int) {
        // 选中指定位置的Tab，会自动触发onTabSelected回调，同步切换Fragment
        tabLayout.getTabAt(position)?.select()
    }
    private fun initTableLayout() {
        // 动态添加 tabs
        val askTab = tabLayout.newTab().setIcon(R.drawable.ic_ai_assistant).setText("低碳ai助手")
        val mainHomeTab = tabLayout.newTab().setIcon(R.drawable.main_home2).setText("主页")
        val communityTab = tabLayout.newTab().setIcon(R.drawable.ic_community).setText("碳社区")
        val imTab = tabLayout.newTab().setIcon(R.drawable.ic_profile).setText("我的")
//        val dataTab = tabLayout.newTab().setIcon(R.drawable.data).setText("数据分析")

        // 按指定顺序添加Tabs
        tabLayout.addTab(mainHomeTab)  // position 0
//        tabLayout.addTab(dataTab)       // position 1
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
                        0 -> MainHomeFragment()
//                        1 -> DataAnalyseFragment()
                        1 -> AskFragment()
                        2 -> CommunityFragment()
                        3 -> ImFragment()
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

    fun switchFragment(newFragment: Fragment) {
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