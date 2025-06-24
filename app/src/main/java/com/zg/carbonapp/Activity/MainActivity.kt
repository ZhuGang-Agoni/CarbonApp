package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.FrameLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.zg.carbonapp.Fragment.AskFragment
import com.zg.carbonapp.Fragment.CompareSceneFragment
import com.zg.carbonapp.Fragment.ImFragment
import com.zg.carbonapp.Fragment.SearchFragment
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityMainBinding
import java.lang.IllegalStateException


class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private lateinit var container:FrameLayout
    private val fragments = mutableMapOf<Int, Fragment>()


    private var currentPosition=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        container=binding.container
        setContentView(binding.root)
//        fragments[0]?.let { swapFragment(it) }
        //初始化默认为第一个
        if (savedInstanceState == null) {
            val firstFragment = AskFragment()
            fragments[0] = firstFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.container, firstFragment)
                .show(firstFragment)
                .commit()
        }
        initTabLayout()
        initListener()

    }
    //初始化化底部导航栏
    private fun initTabLayout(){
        val searchTab=binding.tabLayout.newTab().setIcon(R.drawable.ic_launcher_background)
            .setText("查询")

        val carbonAskTab=binding.tabLayout.newTab().setIcon(R.drawable.ic_launcher_background)
            .setText("询问")
        val compareAskTab=binding.tabLayout.newTab().setIcon(R.drawable.ic_launcher_background)
            .setText("比较")
        val imTab=binding.tabLayout.newTab().setIcon(R.drawable.ic_launcher_background)
            .setText("主页")

        binding.tabLayout.addTab(searchTab)
        binding.tabLayout.addTab(carbonAskTab)
        binding.tabLayout.addTab(compareAskTab)
        binding.tabLayout.addTab(imTab)

    }


    //初始化监听器
    private fun initListener(){
        binding.tabLayout.addOnTabSelectedListener(object:TabLayout.OnTabSelectedListener{
            override fun onTabUnselected(tab: Tab?) {

            }

            override fun onTabReselected(tab: Tab?) {

            }

            override fun onTabSelected(tab: Tab?) {
                if (currentPosition==tab?.position) return

                val fragment=fragments.getOrPut(tab!!.position){//后面是一个lambada表达式 不用解释
                    when(tab?.position){
                        0-> AskFragment()
                        1-> SearchFragment()
                        2->CompareSceneFragment()
                        3-> ImFragment()
                        else -> throw IllegalStateException("无效的位置")
                    }
                }


                swapFragment(fragment)
                currentPosition=tab.position
            }
        })

    }
//交换一个Fragment的逻辑
    private fun swapFragment( fragment: Fragment){
        val transaction=supportFragmentManager.beginTransaction()

        fragments[currentPosition]?.let{
             transaction.hide(it)//因为我现在的currentPosition还没有转换 所以我先那个啥给他隐藏了
        }

        if (fragment.isAdded){
            transaction.show(fragment)
        }
        else {
            transaction.add(container.id,fragment)//这我也是醉l
        }
        transaction.commit()
    }
}