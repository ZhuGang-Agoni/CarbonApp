package com.zg.carbonapp.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import com.zg.carbonapp.Adapter.TravelRecordAdapter
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.AnimationFlashing
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityGreenTravelBinding
import java.text.SimpleDateFormat
import java.util.*

class GreenTravelActivity : AppCompatActivity(),
    AMapLocationListener,
    PoiSearch.OnPoiSearchListener,
    RouteSearch.OnRouteSearchListener {

    private lateinit var binding: ActivityGreenTravelBinding
    private lateinit var travelRecord: TravelRecord
    private var travelList = listOf<ItemTravelRecord>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    // 高德API相关
    private var locationClient: AMapLocationClient? = null
    private var poiSearch: PoiSearch? = null
    private var routeSearch: RouteSearch? = null
    private var startPoint: LatLonPoint? = null
    private var endPoint: LatLonPoint? = null
    private var realDistance = 0.0
    private var startAddress = "当前位置"
    private var endAddress = "未知终点"

    // 地图相关
    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var currentLocation: LatLng? = null

    // 出行方式管理
    private enum class RouteType { BUS, RIDE, WALK }
    private var currentMode: RouteType = RouteType.BUS
    private var currentCity = "岳阳市"

    // 路线覆盖物
    private var routePolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private lateinit var geocodeSearch: GeocodeSearch


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGreenTravelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化地图
        initMap(savedInstanceState)
        initListener()
        initTravelRecords()
    }

    // ====================== 1. 地图初始化 ======================
    private fun initMap(savedInstanceState: Bundle?) {

        // 初始化逆地理编码
        geocodeSearch = GeocodeSearch(this).apply {
            setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(result: GeocodeResult?, errorCode: Int) {
                    // 正向编码（根据地址查经纬度），此处无需处理
                }

                override fun onRegeocodeSearched(result: RegeocodeResult?, errorCode: Int) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                        val detailedAddress = result.regeocodeAddress.formatAddress
                        // 更新起点地址为精确地址
                        startAddress = detailedAddress
                        binding.etStart.setText(detailedAddress)
                        // 更新标记的snippet
                        startMarker?.snippet = detailedAddress
                    }
                }
            })
        }
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        aMap = mapView.map

        // 配置地图UI
        aMap?.apply {
            uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
                isZoomControlsEnabled = true
            }

            // 配置定位图层
            val locationStyle = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
                showMyLocation(true)
                interval(2000)
                strokeColor(Color.TRANSPARENT)
                radiusFillColor(Color.argb(100, 0, 145, 255))
            }

            this.myLocationStyle = locationStyle
            isMyLocationEnabled = true
            moveCamera(CameraUpdateFactory.zoomTo(16f))

            // 地图加载完成后尝试定位
            setOnMapLoadedListener {
                if (ContextCompat.checkSelfPermission(
                        this@GreenTravelActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startLocation()
                }
            }
        }
    }

    // ====================== 2. 初始化数据 ======================
    private fun initTravelRecords() {
        val cacheRecord = TravelRecordManager.getRecords()
        travelRecord = cacheRecord ?: getData()

        // 确保数据完整
        travelRecord = travelRecord.copy(
            userId = travelRecord.userId ?: "DEFAULT_USER",
            totalCarbon = travelRecord.totalCarbon ?: "0.0",
            todayCarbon = travelRecord.todayCarbon ?: "0.0",
            carbonPoint = travelRecord.carbonPoint ?: "0",
            list = travelRecord.list ?: emptyList()
        )

        // 排序并更新UI
        travelList = travelRecord.list.sortedByDescending { it.time }
        updateRecyclerView()
        updateUI()
    }

    private fun updateRecyclerView() {
        val adapter = TravelRecordAdapter(travelList, this)
        binding.recyclerViewTravelRecord.adapter = adapter
        binding.recyclerViewTravelRecord.layoutManager = LinearLayoutManager(this)
    }

    // ====================== 3. 事件监听 ======================
    private fun initListener() {
        // 返回按钮
        binding.btnBack.setOnClickListener { finish() }

        // 碳积分
        binding.cardCarbonAccount.setOnClickListener {
            MyToast.sendToast("此功能开发中", this)
        }

        // 刷新按钮
        binding.ivRefresh.setOnClickListener {
            travelRecord = TravelRecordManager.getRecords() ?: getData()
            travelList = travelRecord.list.sortedByDescending { it.time }
            updateRecyclerView()
            updateUI()
            Toast.makeText(this, "数据已刷新", Toast.LENGTH_SHORT).show()
        }

        // 定位按钮
        binding.btnLocate.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
            } else {
                startLocation()
            }
        }

        // 终点搜索
        binding.etEnd.setOnEditorActionListener { v, _, _ ->
            val keyword = v.text.toString().trim()
            if (keyword.isNotEmpty()) {
                searchPOI(keyword)
            }
            true
        }

        // 计算路线按钮
        binding.btnCalculate.setOnClickListener {
            val endKeyword = binding.etEnd.text.toString().trim()
            if (endKeyword.isEmpty()) {
                Toast.makeText(this, "请输入目的地", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (startPoint == null) {
                Toast.makeText(this, "请先定位起点", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnCalculate.text = "计算中..."
            binding.btnCalculate.isEnabled = false

            if (endPoint == null) {
                searchPOI(endKeyword)
                binding.root.postDelayed({
                    if (endPoint != null) {
                        calculateRoute()
                    } else {
                        binding.btnCalculate.text = "计算路线"
                        binding.btnCalculate.isEnabled = true
                        Toast.makeText(this, "终点搜索失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }, 1500)
            } else {
                calculateRoute()
            }
        }

        // 出行方式按钮
        binding.cardBus.setOnClickListener {
            currentMode = RouteType.BUS
            AnimationFlashing.flashView(binding.cardBus, Color.BLUE)
            binding.root.postDelayed({ recordTravel("公交") }, 500)
        }

        binding.cardBike.setOnClickListener {
            currentMode = RouteType.RIDE
            AnimationFlashing.flashView(binding.cardBike, Color.BLUE)
            binding.root.postDelayed({ recordTravel("骑行") }, 500)
        }

        binding.cardWalk.setOnClickListener {
            currentMode = RouteType.WALK
            AnimationFlashing.flashView(binding.cardWalk, Color.BLUE)
            binding.root.postDelayed({ recordTravel("步行") }, 500)
        }

        binding.cardMetro.setOnClickListener {
            MyToast.sendToast("地铁功能开发中", this)
        }

        // 分享按钮
        binding.btnShare.setOnClickListener { shareCarbonAchievement() }
    }

    // ====================== 4. 定位功能 ======================
    @SuppressLint("MissingPermission")
    private fun startLocation() {
        if (locationClient == null) {
            locationClient = AMapLocationClient(this).apply {
                val option = AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isOnceLocationLatest = true
                    isNeedAddress = true
                    httpTimeOut = 10000
                    isGpsFirst = true
                }
                setLocationOption(option)
                setLocationListener(this@GreenTravelActivity)
            }
        }

        binding.btnLocate.text = "定位中..."
        locationClient?.startLocation()
    }
    override fun onLocationChanged(location: AMapLocation?) {
        binding.btnLocate.text = "重新定位"
        location?.let {
            if (it.errorCode == 0) {
                // 1. 记录经纬度
                val latLng = LatLng(it.latitude, it.longitude)
                startPoint = LatLonPoint(it.latitude, it.longitude)
                currentLocation = latLng

                // 2. 发起逆地理编码请求（获取精确地址）
                val regeocodeQuery = RegeocodeQuery(startPoint, 200f, GeocodeSearch.AMAP)
                geocodeSearch.getFromLocationAsyn(regeocodeQuery)

                // 3. 字段拼接作为兜底（逆地理编码失败时用）
                val addressBuilder = StringBuilder()
                if (!it.province.isNullOrEmpty()) addressBuilder.append(it.province)
                if (!it.city.isNullOrEmpty()) addressBuilder.append(it.city)
                if (!it.district.isNullOrEmpty()) addressBuilder.append(it.district)
                if (!it.street.isNullOrEmpty()) addressBuilder.append(it.street)
                if (!it.streetNum.isNullOrEmpty()) addressBuilder.append(it.streetNum)
                val fallbackAddress = if (addressBuilder.isNotEmpty()) {
                    addressBuilder.toString()
                } else {
                    "当前位置（定位信息不足）" // 避免空地址
                }

                // 4. 先显示兜底地址，逆地理编码结果回来后自动更新
                startAddress = fallbackAddress
                binding.etStart.setText(fallbackAddress)

                // 5. 添加/更新起点标记
                startMarker?.remove()
                startMarker = aMap?.addMarker(MarkerOptions()
                    .position(latLng)
                    .title("当前位置")
                    .snippet(fallbackAddress)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

                Toast.makeText(this, "定位成功（逆地理编码中...）", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "定位失败：码${it.errorCode}，原因${it.errorInfo}", Toast.LENGTH_LONG).show()
            }
        }
    }
//    override fun onLocationChanged(location: AMapLocation?) {
//        binding.btnLocate.text = "重新定位"
//        location?.let {
//            if (it.errorCode == 0) {
//                // 构建详细地址
//                val addressBuilder = StringBuilder()
//                if (!it.province.isNullOrEmpty()) addressBuilder.append(it.province)
//                if (!it.city.isNullOrEmpty()) addressBuilder.append(it.city)
//                if (!it.district.isNullOrEmpty()) addressBuilder.append(it.district)
//                if (!it.street.isNullOrEmpty()) addressBuilder.append(it.street)
//                if (!it.streetNum.isNullOrEmpty()) addressBuilder.append(it.streetNum)
//
//                val address = if (addressBuilder.isNotEmpty()) addressBuilder.toString() else "当前地址"
//
//                startPoint = LatLonPoint(it.latitude, it.longitude)
//                startAddress = address
//                currentLocation = LatLng(it.latitude, it.longitude)
//
//                // 更新UI显示具体位置
//                binding.etStart.setText(address)
//                binding.etStart.isEnabled = false
//
//                // 地图定位中心点移动
//                aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                    LatLng(it.latitude, it.longitude),
//                    16f
//                ))
//
//                // 添加起点标记
//                startMarker?.remove()
//                startMarker = aMap?.addMarker(MarkerOptions()
//                    .position(currentLocation!!)
//                    .title("起点")
//                    .snippet(address)
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
//
//                Toast.makeText(this, "定位成功：$address", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "定位失败：码${it.errorCode}，原因${it.errorInfo}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            Toast.makeText(this, "需要定位权限才能获取精确起点", Toast.LENGTH_SHORT).show()
        }
    }

    // ====================== 5. POI搜索 ======================
    private fun searchPOI(keyword: String) {
        val query = PoiSearch.Query(keyword, "", currentCity)
        query.pageSize = 10
        query.pageNum = 1

        poiSearch = PoiSearch(this, query).apply {
            setOnPoiSearchListener(this@GreenTravelActivity)
            searchPOIAsyn()
        }
    }

    override fun onPoiSearched(result: PoiResult?, errorCode: Int) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null && result.pois.isNotEmpty()) {
            val firstPoi = result.pois[0]
            endPoint = firstPoi.latLonPoint
            endAddress = firstPoi.title
            binding.etEnd.setText(firstPoi.title)

            // 添加终点标记
            endMarker?.remove()
            endMarker = aMap?.addMarker(MarkerOptions()
                .position(LatLng(firstPoi.latLonPoint.latitude, firstPoi.latLonPoint.longitude))
                .title("目的地")
                .snippet(endAddress)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

            // 地图移动到终点
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(firstPoi.latLonPoint.latitude, firstPoi.latLonPoint.longitude),
                16f
            ))

            Toast.makeText(this, "搜索成功：${firstPoi.title}", Toast.LENGTH_SHORT).show()
        } else {
            val errorMsg = when(errorCode) {
                10001 -> "Key无效（检查高德配置）"
                12 -> "网络错误（请检查网络）"
                27 -> "无搜索结果（尝试更精确的关键词）"
                else -> "搜索失败：错误码$errorCode"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {}

    // ====================== 6. 路线规划 ======================
    private fun calculateRoute() {
        if (startPoint == null || endPoint == null) return

        // 清除旧路线
        clearRoute()

        val fromAndTo = RouteSearch.FromAndTo(startPoint, endPoint)

        try {
            when (currentMode) {
                RouteType.BUS -> {
                    val query = RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BUS_DEFAULT, currentCity, 0)
                    routeSearch = RouteSearch(this).apply {
                        setRouteSearchListener(this@GreenTravelActivity)
                        calculateBusRouteAsyn(query)
                    }
                }
                RouteType.RIDE -> {
                    val query = RouteSearch.RideRouteQuery(fromAndTo)
                    routeSearch = RouteSearch(this).apply {
                        setRouteSearchListener(this@GreenTravelActivity)
                        calculateRideRouteAsyn(query)
                    }
                }
                RouteType.WALK -> {
                    val query = RouteSearch.WalkRouteQuery(fromAndTo)
                    routeSearch = RouteSearch(this).apply {
                        setRouteSearchListener(this@GreenTravelActivity)
                        calculateWalkRouteAsyn(query)
                    }
                }
            }
        } catch (e: Exception) {
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            Toast.makeText(this, "路线计算失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 清除旧路线
    private fun clearRoute() {
        routePolyline?.remove()
        routePolyline = null
    }

    // ====================== 7. 路线回调处理 ======================
    override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {
        handleRouteResult(result, errorCode, "公交")
    }

    override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) {
        handleRouteResult(result, errorCode, "骑行")
    }

    override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {
        handleRouteResult(result, errorCode, "步行")
    }

    override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) {}

    private fun <T : RouteResult> handleRouteResult(result: T?, errorCode: Int, mode: String) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
            when (result) {
                is BusRouteResult -> {
                    if (result.paths.isNotEmpty()) {
                        realDistance = result.paths[0].distance / 1000.0
                        showRouteResult(mode, realDistance)

                        // 绘制两点连线
                        drawStraightLine(
                            start = LatLng(startPoint!!.latitude, startPoint!!.longitude),
                            end = LatLng(endPoint!!.latitude, endPoint!!.longitude),
                            color = Color.BLUE
                        )
                    } else {
                        Toast.makeText(this, "未找到${mode}路线", Toast.LENGTH_SHORT).show()
                    }
                }
                is RideRouteResult -> {
                    if (result.paths.isNotEmpty()) {
                        realDistance = result.paths[0].distance / 1000.0
                        showRouteResult(mode, realDistance)

                        // 绘制两点连线
                        drawStraightLine(
                            start = LatLng(startPoint!!.latitude, startPoint!!.longitude),
                            end = LatLng(endPoint!!.latitude, endPoint!!.longitude),
                            color = Color.GREEN
                        )
                    } else {
                        Toast.makeText(this, "未找到${mode}路线", Toast.LENGTH_SHORT).show()
                    }
                }
                is WalkRouteResult -> {
                    if (result.paths.isNotEmpty()) {
                        realDistance = result.paths[0].distance / 1000.0
                        showRouteResult(mode, realDistance)

                        // 绘制两点连线
                        drawStraightLine(
                            start = LatLng(startPoint!!.latitude, startPoint!!.longitude),
                            end = LatLng(endPoint!!.latitude, endPoint!!.longitude),
                            color = Color.MAGENTA
                        )
                    } else {
                        Toast.makeText(this, "未找到${mode}路线", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            val errorMsg = when(errorCode) {
                10001 -> "Key无效（检查高德配置）"
                12 -> "网络错误（请检查网络）"
                32 -> "无此路线（尝试更短距离或更换目的地）"
                else -> "${mode}路线规划失败：错误码$errorCode"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // 绘制两点间的直线
    private fun drawStraightLine(start: LatLng, end: LatLng, color: Int) {
        routePolyline?.remove()

        val polylineOptions = PolylineOptions()
            .add(start)
            .add(end)
            .color(color)
            .width(10f)
            .geodesic(true)
        routePolyline = aMap?.addPolyline(polylineOptions)
    }

    private fun showRouteResult(mode: String, distance: Double) {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvDistanceResult.text = "距离：${String.format("%.2f", distance)} km"
        val carbon = calculateCarbonEmission(mode, distance)
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.2f", carbon)} kg"
    }

    // ====================== 8. 记录出行 ======================
    @SuppressLint("NewApi")
    private fun recordTravel(mode: String) {
        if (realDistance == 0.0) {
            Toast.makeText(this, "请先计算路线", Toast.LENGTH_SHORT).show()
            return
        }

        val carbonEmission = calculateCarbonEmission(mode, realDistance)
        val newRecord = ItemTravelRecord(
            travelModel = mode,
            travelRoute = "$startAddress→$endAddress",
            carbonCount = "${String.format("%.2f", carbonEmission)} kg",
            distance = "${String.format("%.2f", realDistance)} km",
            time = System.currentTimeMillis(), // 使用当前时间戳
            modelRavel = getIconResId(mode)
        )

        saveToCache(newRecord, carbonEmission)
        updateUI()

        // 清空搜索状态
        binding.etEnd.setText("")
        endPoint = null
        endAddress = "未知终点"
        binding.cardResult.visibility = View.GONE
        realDistance = 0.0

        // 清除地图标记
        clearRoute()
        endMarker?.remove()
        endMarker = null

        Toast.makeText(
            this,
            "记录成功：${mode} ${String.format("%.2f", realDistance)}km 减碳${String.format("%.2f", carbonEmission)}kg",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ====================== 9. 辅助方法 ======================
    private fun calculateCarbonEmission(mode: String, distance: Double): Double {
        return when (mode) {
            "公交" -> distance * 0.12
            "骑行", "步行" -> 0.0
            else -> distance * 0.1
        }
    }

    private fun getIconResId(mode: String): Int {
        return when (mode) {
            "公交" -> R.drawable.ic_bus
            "骑行" -> R.drawable.ic_bike
            "步行" -> R.drawable.ic_walk
            else -> R.drawable.green_go_navigation
        }
    }

    private fun updateUI() {
        // 更新列表
        travelList = travelRecord.list.sortedByDescending { it.time }
        (binding.recyclerViewTravelRecord.adapter as? TravelRecordAdapter)?.notifyDataSetChanged()

        val totalCarbon = travelRecord.totalCarbon.toDoubleOrNull() ?: 0.0
        binding.tvTotalCarbon.text = String.format("%.1f kg", totalCarbon)
        binding.tvCarbonPoints.text = "${(totalCarbon * 10).toInt()} 积分"

        val todayCarbon = travelRecord.todayCarbon.toDoubleOrNull() ?: 0.0
        binding.tvTodayCarbon.text = String.format("今日减碳 %.1f kg", todayCarbon)
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun saveToCache(newRecord: ItemTravelRecord, carbonEmission: Double) {
        val oldTotal = travelRecord.totalCarbon.toDoubleOrNull() ?: 0.0
        val newTotal = oldTotal + carbonEmission
        val newPoints = "${(newTotal * 10).toInt()}"

        val oldToday = travelRecord.todayCarbon.toDoubleOrNull() ?: 0.0
        val newToday = if (isToday(newRecord.time)) oldToday + carbonEmission else oldToday

        val newList = mutableListOf(newRecord).apply { addAll(travelRecord.list) }

        travelRecord = TravelRecord(
            userId = travelRecord.userId,
            totalCarbon = String.format("%.1f", newTotal),
            todayCarbon = String.format("%.1f", newToday),
            carbonPoint = newPoints,
            list = newList
        )

        TravelRecordManager.saveRecord(travelRecord)
    }

    private fun getData(): TravelRecord {
        // 使用示例时间（2025-07-11 10:11）
        val sampleTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            .parse("2025-07-11 10:11")?.time ?: System.currentTimeMillis()

        return TravelRecord(
            userId = "USER_001",
            totalCarbon = "3.3",
            todayCarbon = "2.1",
            carbonPoint = "33",
            list = listOf(
                ItemTravelRecord(
                    travelModel = "公交",
                    travelRoute = "湖南省岳阳市XX路→长沙理工大学",
                    carbonCount = "2.1 kg",
                    distance = "17.5 km",
                    time = sampleTime,
                    modelRavel = R.drawable.ic_bus
                )
            )
        )
    }

    private fun shareCarbonAchievement() {
        val totalCarbon = binding.tvTotalCarbon.text.toString()
        val shareText = "我已减少${totalCarbon}碳排放，一起环保！"
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("share_content", shareText)
            putExtra("navigate_to_community", true)
        }
        startActivity(intent)
        finish()
    }

    // ====================== 10. 生命周期管理 ======================
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationClient?.stopLocation()
        locationClient?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}