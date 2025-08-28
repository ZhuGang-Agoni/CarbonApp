package com.zg.carbonapp.Activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.zg.carbonapp.Dao.LowCarbonArea
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityCarbonMapBinding

class CarbonMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonMapBinding
    private lateinit var mBaiduMap: BaiduMap
    private var currentLat = 0.0
    private var currentLng = 0.0

    // 模拟低碳区域数据
    private val lowCarbonAreas = listOf(
        LowCarbonArea(
            "1", "城市绿道",
            39.915, 116.404,
            "适合步行/骑行的绿色廊道，全长5公里"
        ),
        LowCarbonArea(
            "2", "中央公园无车区",
            39.925, 116.414,
            "禁止机动车进入，纯步行区域"
        ),
        LowCarbonArea(
            "3", "滨河自行车专用道",
            39.905, 116.424,
            "专用自行车道，风景优美"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化地图控件
        mBaiduMap = binding.mapView.map
        initMapSettings()

        // 获取传递的位置信息
        currentLat = intent.getDoubleExtra("lat", 39.915)
        currentLng = intent.getDoubleExtra("lng", 116.404)

        // 移动地图到当前位置
        val currentLatLng = LatLng(currentLat, currentLng)
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(currentLatLng, 15f))

        // 修改后的添加Marker方法
        addCurrentLocationMarker(currentLatLng)
        addLowCarbonAreaMarkers()
    }

    // 初始化地图设置
    private fun initMapSettings() {
        mBaiduMap.uiSettings.apply {
            isCompassEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
        }
    }

    // 修改后的当前位置Marker添加方法
    private fun addCurrentLocationMarker(latLng: LatLng) {
        try {
            // 使用 AppCompatResources 加载矢量图
            val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_location)
            if (drawable == null) {
                Log.e("MarkerError", "ic_location 资源未找到")
                showToast("无法加载位置图标")
                return
            }

            // 将 Drawable 转换为 Bitmap
            val bitmap = drawable.toBitmap()
            val icon = BitmapDescriptorFactory.fromBitmap(bitmap)

            // 构建 MarkerOption
            val option = MarkerOptions()
                .position(latLng)
                .icon(icon)
                .title("我的位置")
                .draggable(false)
                .flat(false)
                .alpha(1.0f)

            mBaiduMap.addOverlay(option)
        } catch (e: Exception) {
            Log.e("MarkerError", "添加当前位置Marker失败", e)
            showToast("添加位置标记失败: ${e.message}")
        }
    }

    // 修改后的低碳区域Marker添加方法
    private fun addLowCarbonAreaMarkers() {
        lowCarbonAreas.forEach { area ->
            try {
                val point = LatLng(area.lat, area.lng)

                // 使用 AppCompatResources 加载矢量图
                val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_low_carbon)
                if (drawable == null) {
                    Log.e("MarkerError", "ic_low_carbon 资源未找到")
                    showToast("无法加载区域图标")
                    return@forEach
                }

                // 将 Drawable 转换为 Bitmap
                val bitmap = drawable.toBitmap()
                val icon = BitmapDescriptorFactory.fromBitmap(bitmap)

                // 构建 MarkerOption
                val option = MarkerOptions()
                    .position(point)
                    .icon(icon)
                    .title(area.name)
                    .draggable(false)
                    .flat(false)
                    .alpha(1.0f)

                mBaiduMap.addOverlay(option)
                setMarkerInfoWindow(point, area.name, area.description)

            } catch (e: Exception) {
                Log.e("MarkerError", "添加${area.name}Marker失败", e)
                showToast("添加${area.name}标记失败: ${e.message}")
            }
        }
    }

    // 扩展函数：将 Drawable 转换为 Bitmap
    private fun Drawable.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth,
            intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    // 设置Marker信息窗口（保持原有逻辑）
    private fun setMarkerInfoWindow(latLng: LatLng, title: String, description: String) {
        @SuppressLint("MissingInflatedId")
        fun createInfoWindowView(text: String): View {
            val view = LayoutInflater.from(this).inflate(R.layout.info_window, null)
            val tv = view.findViewById<TextView>(R.id.info_text)
            tv.text = text
            return view
        }

        val infoWindow = InfoWindow(
            createInfoWindowView(description.ifEmpty { "暂无描述" }),
            latLng,
            -100
        )

        mBaiduMap.setOnMarkerClickListener { marker ->
            if (marker.title == title) {
                mBaiduMap.showInfoWindow(infoWindow)
            }
            true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}