package com.zg.carbonapp.ui.place

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Activity.MainActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.logic.model.Place
import com.zg.carbonapp.ui.weather.WeatherActivity
import java.util.Calendar

class PlaceFragment : Fragment() {

    internal val viewModel by lazy { ViewModelProvider(this).get(PlaceViewModel::class.java) }

    private lateinit var adapter: PlaceAdapter
    private var back_ground: ImageView? = null

    // 新增：用于获取根布局，以便切换背景
    private var rootLayout: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootLayout = inflater.inflate(R.layout.fragment_place, container, false)
        return rootLayout
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // 判断一下是否已经存储过了
        if (viewModel.isPlaceSaved() && activity is MainActivity) {
            val place = viewModel.getSavedPlace()
            val intent = Intent(context, WeatherActivity::class.java).apply {
                putExtra("location_lng", place.location.lng)
                putExtra("location_lat", place.location.lat)
                putExtra("place_name", place.name)
            }
            startActivity(intent)
            activity?.finish()
            return
        }

        val layoutManager = LinearLayoutManager(activity)
        val recyclerView = activity?.findViewById<RecyclerView>(R.id.recyclerView)
        adapter = PlaceAdapter(this, viewModel.placeList)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = layoutManager

        val searchPlaceEdit = activity?.findViewById<EditText>(R.id.searchPlaceEdit)
        searchPlaceEdit?.addTextChangedListener { editInformation ->
            val content = editInformation.toString()
            if (content.isNotEmpty()) {
                viewModel.searchPlaces(content)
            } else {
                recyclerView?.visibility = View.GONE

                back_ground!!.visibility = View.VISIBLE
                viewModel.placeList.clear()
                adapter.notifyDataSetChanged()
            }
        }

        viewModel.placeLiveData.observe(requireActivity(), Observer { res: Result<List<Place>> ->
            val places = res.getOrNull()
            if (places != null) {
                recyclerView?.visibility = View.VISIBLE
                back_ground?.visibility = View.GONE
                viewModel.placeList.clear()
                viewModel.placeList.addAll(places)
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(activity, "未能查询到任何地点", Toast.LENGTH_SHORT).show()
                res.exceptionOrNull()?.printStackTrace()
            }
        })

        // 新增：调用切换背景的方法
        switchBackgroundByTime()
    }

    // 新增：根据时间切换背景的方法
    private fun switchBackgroundByTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // 假设 6 点到 18 点为白天，其余为夜晚，可根据实际需求调整时间区间
        val isDayTime = hour in 6..17

        val gradientRes = if (isDayTime) {
            R.drawable.weather_gradient_background  // 白天渐变背景
        } else {
            R.drawable.weather_gradient_night  // 夜晚渐变背景
        }

        rootLayout?.setBackgroundResource(gradientRes)
    }
}