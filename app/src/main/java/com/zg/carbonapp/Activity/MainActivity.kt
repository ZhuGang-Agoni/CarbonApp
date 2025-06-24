package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import com.zg.carbonapp.Fragment.AskFragment
import com.zg.carbonapp.Fragment.ChallengeFragment
import com.zg.carbonapp.Fragment.CommunityFragment
import com.zg.carbonapp.Fragment.ImFragment

import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tabLayout by lazy { binding.tabLayout }
    private val container by lazy { binding.container }
    private var currentTabPosition=0
    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initTableLayout()
        initListener()
//实现一个初始化
        if (savedInstanceState == null) {
            val firstFragment = ChallengeFragment()
            fragments[0] = firstFragment
            supportFragmentManager.beginTransaction()
                .add(container.id, firstFragment)
                .show(firstFragment)
                .commit()
        }
    }
//这里在添加的时候一定要按顺序来
    private fun initTableLayout(){

            // 动态添加 tabs icon是图像的意思
            val askTab = tabLayout.newTab().setIcon(R.drawable.ic_ai_assistant).setText("低碳ai助手")
            val challengeTab = tabLayout.newTab().setIcon(R.drawable.ic_challenge).setText("减排挑战")
            val communityTab = tabLayout.newTab().setIcon(R.drawable.ic_community).setText("碳社区")
            val imTab = tabLayout.newTab().setIcon(R.drawable.ic_profile).setText("我的主页")
            tabLayout.addTab(challengeTab)
            tabLayout.addTab(askTab)
            tabLayout.addTab(communityTab)
            tabLayout.addTab(imTab)


    }
//这里有一个 Fragment被我删掉了 应该是那个啥 场景的你自己到时候添加进来 下面的代码 我都要写了注释了
  private fun  initListener() {
      tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
          override fun onTabSelected(tab: TabLayout.Tab) {
              if (currentTabPosition == tab.position) return
//这是一个高阶函数哈 fun:(Int)->Int
              val fragment = fragments.getOrPut(tab.position) //通过源码我们可以得知 后面一个参数是一个函数
              {//如果这个Fragment已经存在了 就直接返回 如果不存在就先添加
                  when (tab.position) {//position是Int类型的
                      0 -> ChallengeFragment()//这里的newInstance就是获取对象的一个意思
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
//交换Fragment
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