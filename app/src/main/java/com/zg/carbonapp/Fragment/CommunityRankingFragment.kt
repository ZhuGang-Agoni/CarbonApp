package com.zg.carbonapp.Fragment

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.RankingAdapter
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.MMKV.RankingItemMMKV
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentRankingBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // 新增：下拉刷新依赖

class CommunityRankingFragment: Fragment() {
    private lateinit var  binding:FragmentRankingBinding
    private val recyclerView by lazy { binding.recyclerView}
    // 新增：下拉刷新控件引用（需确保布局中已添加SwipeRefreshLayout，id为swipeRefreshLayout）
    private val swipeRefresh by lazy { binding.swipeRefreshLayout }
    private lateinit var currentAdapter: RankingAdapter // 新增：保存当前适配器引用

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentRankingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 原有代码：令牌检查
        val token=TokenManager.getToken()
        if (token==null){
            MyToast.sendToast("令牌为空，请用户先行登录",requireActivity())
        }

        // 原有代码：初始化列表

        val rankList=getList()
        currentAdapter=RankingAdapter(rankList,requireContext())
        val layoutManager=LinearLayoutManager(context)
        recyclerView.adapter=currentAdapter
        recyclerView.layoutManager=layoutManager

        // 新增：初始化下拉刷新
        initSwipeRefresh()
    }

    // 新增：初始化下拉刷新逻辑
    private fun initSwipeRefresh() {
        // 设置刷新颜色
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        // 设置下拉刷新监听器
        swipeRefresh.setOnRefreshListener {
            // 模拟网络请求延迟（实际项目中替换为真实接口请求）
            Handler(Looper.getMainLooper()).postDelayed({
                refreshRankingData() // 刷新数据
                swipeRefresh.isRefreshing = false // 停止刷新动画
            }, 1500)
        }
    }

    // 新增：刷新排行榜数据
    private fun refreshRankingData() {
        // 优先从MMKV获取数据（实现原有注释中的逻辑）
        val mmkvList = RankingItemMMKV.getRankingItem()
        if (mmkvList.isNotEmpty()) {
            currentAdapter.updateData(mmkvList) // 刷新适配器数据（需在RankingAdapter中新增updateData方法）
//           currentAdapter.notifyDataSetChanged()
            MyToast.sendToast("已更新最新排行", requireActivity())
        } else {
            // MMKV无数据时，模拟网络请求新数据
            val newData = getRefreshData()
            currentAdapter.updateData(newData)
//            currentAdapter.notifyDataSetChanged()
            // 同时保存到MMKV
            RankingItemMMKV.saveRankingItem(newData)
            MyToast.sendToast("从网络获取最新排行", requireActivity())
        }
    }

    // 新增：模拟刷新时的新数据
    private fun getRefreshData(): List<RankingItem> {
        // 模拟新增/更新的数据（实际项目中替换为接口返回数据）
        return listOf(
            RankingItem("1","Agoni",getImage(),120.0,1), // 分数更新
            RankingItem("2","Mona",getImage(),110.0,2), // 新增用户
            RankingItem("3","Carbon",getImage(),105.0,3), // 新增用户
            RankingItem("4","Eco",getImage(),90.0,4),
            RankingItem("5","Green",getImage(),80.0,5) // 新增用户
        )
    }

    // 原有代码：模拟初始数据
    private fun getList():List<RankingItem>{
        return listOf(RankingItem("1","Agoni",getImage(),100.0,1),
            RankingItem("2","Agoni",getImage(),100.0,2)
            ,RankingItem("3","Agoni",getImage(),100.0,3),
            RankingItem("4","Agoni",getImage(),100.0,4))
    }

    // 原有代码：获取图片Uri
    private fun getImage():String{
//        var resId: Int = com.zg.carbonapp.R.drawable.img
//        var packageName: String = requireActivity().getPackageName()
//        var uri: Uri = Uri.parse("android.resource://$packageName/$resId")
//        var uriString: String = uri.toString()
//        return uriString
        val resId="R.drawable.img"
        return resId
    }
}