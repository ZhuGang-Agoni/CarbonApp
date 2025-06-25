package com.zg.carbonapp.Fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.RankingAdapter
import com.zg.carbonapp.Dao.RankingItem
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentRankingBinding

class CommunityRankingFragment: Fragment() {
    private lateinit var  binding:FragmentRankingBinding
    private val recyclerView by lazy { binding.recyclerView}

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
        //这边就是单纯的一个模拟
        val token=TokenManager.getToken()
        if (token==null){
            MyToast.sendToast("令牌为空，请用户先行登录",requireActivity())
        }

        val rankList=getList()
        val adapter=RankingAdapter(rankList,requireContext())
        val layoutManager=LinearLayoutManager(context)

        recyclerView.adapter=adapter
        recyclerView.layoutManager=layoutManager

    }
    //这里也是单纯的一个模拟
    private fun getList():List<RankingItem>{
         return listOf(RankingItem("Agoni",getImage(),100.0,1),
             RankingItem("Agoni",getImage(),100.0,2)
             ,RankingItem("Agoni",getImage(),100.0,3),
             RankingItem("Agoni",getImage(),100.0,4))
    }

    private fun getImage():String{
        var resId: Int = com.zg.carbonapp.R.drawable.img

        var packageName: String = requireActivity().getPackageName()
        var uri: Uri = Uri.parse("android.resource://$packageName/$resId")
        var uriString: String = uri.toString() // 转为String
        return uriString
    }
}