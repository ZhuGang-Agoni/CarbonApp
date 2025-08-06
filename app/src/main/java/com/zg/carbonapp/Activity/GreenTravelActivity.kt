package com.zg.carbonapp.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.baidu.location.*
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.model.LatLngBounds
import com.baidu.mapapi.search.core.PoiInfo
import com.baidu.mapapi.search.core.RouteNode
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.baidu.mapapi.search.poi.*
import com.baidu.mapapi.search.route.*
import com.zg.carbonapp.Adapter.TravelRecordAdapter
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.com.baidu.mapapi.overlayutil.*
import com.zg.carbonapp.databinding.ActivityGreenTravelBinding
import com.zg.carbonapp.databinding.DialogRouteDetailBinding
import java.lang.Math.toRadians
import java.util.*
import kotlin.math.*

class GreenTravelActivity : AppCompatActivity(),
    BDLocationListener,
    OnGetRoutePlanResultListener,
    OnGetGeoCoderResultListener,
    OnGetPoiSearchResultListener {

    private lateinit var binding: ActivityGreenTravelBinding
    private lateinit var travelAdapter: TravelRecordAdapter
    private var mMapView: MapView? = null
    private lateinit var mBaiduMap: BaiduMap
    private var mLocationClient: LocationClient? = null
    private var routeSearch: RoutePlanSearch? = null
    private var geoCoder: GeoCoder? = null
    private var poiSearch: PoiSearch? = null

    // 路线和标记相关
    private var currentLatLng: LatLng? = null // 当前定位坐标
    private var endLatLng: LatLng? = null // 终点坐标
    private var currentCity: String? = null // 当前城市

    // 覆盖物管理
    private var walkingRouteOverlay: WalkingRouteOverlay? = null
    private var bikingRouteOverlay: BikingRouteOverlay? = null
    private var drivingRouteOverlay: DrivingRouteOverlay? = null // 驾车路线覆盖物
    private var massTransitRouteOverlay: MassTransitRouteOverlay? = null // 跨城公交路线覆盖物

    // 记录当前路线信息
    private var currentDistance: Int = 0
    private var currentDuration: Int = 0
    private var currentRouteDescription: String = ""

    private val handler = Handler(Looper.getMainLooper())

    private val BAIDU_KEY = "Vm6x2v1JaXggwfsiaVHW17hgRrANq8BF"
    private val TAG = "GreenTravelActivity"
    private val REQUEST_LOCATION_PERMISSION = 100

    private enum class RouteType { WALK, RIDE, BUS, DRIVE } // 路线类型：步行、骑行、跨城公交、自驾
    private var currentRouteType: RouteType = RouteType.WALK

    // 输入框文本监听器
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.isNullOrEmpty()) {
                endLatLng = null
                clearRouteAndMarkers()
            }
        }
        override fun afterTextChanged(p0: Editable?) {}
    }

    // 定位回调
    override fun onReceiveLocation(location: BDLocation?) {
        location?.let {
            // 更新定位状态按钮
            binding.btnLocate.text = "重新定位"
            binding.btnLocate.isEnabled = true

            // 构造定位数据
            val locData = MyLocationData.Builder()
                .accuracy(it.radius)
                .direction(it.direction)
                .latitude(it.latitude)
                .longitude(it.longitude)
                .build()

            // 设置定位数据
            mBaiduMap.setMyLocationData(locData)

            // 保存当前坐标和城市
            currentLatLng = LatLng(it.latitude, it.longitude)
            currentCity = it.city?.replace("市", "") // 去除城市名称中的"市"字

            // 设置起点地址
            binding.etStart.setText(it.addrStr)

            // 首次定位时移动地图到当前位置
            if (binding.etStart.text.isNotEmpty() && mBaiduMap.mapStatus.zoom < 10) {
                val u = MapStatusUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude),
                    16f
                )
                mBaiduMap.animateMapStatus(u)
            }

        } ?: run {
            binding.btnLocate.text = "获取定位"
            binding.btnLocate.isEnabled = true
            Toast.makeText(this, "定位失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGreenTravelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 百度定位隐私政策设置
        initBaiduLocationPrivacy()

        // 初始化百度地图
        mMapView = findViewById(R.id.bmapView)
        mBaiduMap = mMapView?.map!!
        mBaiduMap.isMyLocationEnabled = true // 开启定位图层
        mBaiduMap.uiSettings.isCompassEnabled = true // 显示指南针
        mBaiduMap.uiSettings.isZoomGesturesEnabled = true // 允许缩放手势

        // 初始化搜索组件
        initSearchComponents()

        // 初始化定位
        initLocation()

        // 初始化视图
        initViews()

        // 初始化监听器
        initListeners()

        // 初始化出行记录
        initTravelRecord()
    }

    // 百度定位隐私政策设置
    private fun initBaiduLocationPrivacy() {
        LocationClient.setAgreePrivacy(true)
    }

    // 初始化搜索组件
    private fun initSearchComponents() {
        // 路线规划
        routeSearch = RoutePlanSearch.newInstance()
        routeSearch?.setOnGetRoutePlanResultListener(this)

        // 地理编码
        geoCoder = GeoCoder.newInstance()
        geoCoder?.setOnGetGeoCodeResultListener(this)

        // POI搜索
        poiSearch = PoiSearch.newInstance()
        poiSearch?.setOnGetPoiSearchResultListener(this)
    }

    private fun initLocation() {
        // 初始化定位客户端
        mLocationClient = LocationClient(applicationContext)

        // 配置定位参数
        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setIsNeedAddress(true) // 需要地址信息
            setScanSpan(10000) // 10秒定位一次
            setIsNeedLocationDescribe(true)
            isOpenGps = true // 打开GPS
            setCoorType("bd09ll") // 使用百度坐标
        }

        mLocationClient?.locOption = option
        mLocationClient?.registerLocationListener(this)
    }

    private fun initViews() {
        // 初始化RecyclerView
        travelAdapter = TravelRecordAdapter(emptyList(), this)
        binding.recyclerViewTravelRecord.apply {
            layoutManager = LinearLayoutManager(this@GreenTravelActivity)
            adapter = travelAdapter
            setHasFixedSize(true)
        }

        // 显示结果卡片
        binding.cardResult.visibility = android.view.View.VISIBLE
    }

    private fun initListeners() {
        // 定位按钮
        binding.btnLocate.setOnClickListener {
            checkLocationPermission()
        }

        // 终点输入框监听
        binding.etEnd.addTextChangedListener(textWatcher)
        binding.etEnd.setOnEditorActionListener { _, _, _ ->
            val endAddress = binding.etEnd.text.toString().trim()
            if (endAddress.isNotEmpty()) {
                searchEndLocation(endAddress)
                true
            } else {
                false
            }
        }

        // 计算路线按钮
        binding.btnCalculate.setOnClickListener {
            calculateRouteWithCheck()
        }

        // 出行方式选择
        binding.travelModeGroup.setOnCheckedChangeListener { _, checkedId ->
            currentRouteType = when (checkedId) {
                binding.walkRadio.id -> RouteType.WALK
                binding.bikeRadio.id -> RouteType.RIDE
                binding.busRadio.id -> RouteType.BUS
                binding.driveRadio.id -> RouteType.DRIVE // 对应驾车
                else -> RouteType.WALK
            }
        }

        // 刷新按钮
        binding.ivRefresh.setOnClickListener {
            refreshTravelRecordUI()
            Toast.makeText(this, "已刷新最新记录", Toast.LENGTH_SHORT).show()
        }
    }

    // 搜索终点位置（提高精度）
    private fun searchEndLocation(keyword: String) {
        if (currentCity.isNullOrEmpty()) {
            Toast.makeText(this, "请先完成定位", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载状态
        binding.btnCalculate.text = "搜索中..."
        binding.btnCalculate.isEnabled = false

        // 1. 提高搜索精度：增加城市限定和结果数量
        val searchOption = PoiCitySearchOption()
            .city(currentCity)
            .keyword(keyword)
            .pageNum(0)
            .pageCapacity(20) // 增加返回结果数量
            .scope(2) // 返回POI详细信息
            .cityLimit(true) // 严格限制在当前城市内

        // 2. 对特定关键词（如车站、景点）增强搜索
        if (keyword.contains("站") || keyword.contains("火车站") ||
            keyword.contains("机场") || keyword.contains("景点")) {
            // 优先搜索POI类型为交通设施
            poiSearch?.searchInCity(searchOption)

            // 3. 同时发起周边搜索，提高精度
            currentLatLng?.let { latLng ->
                val nearbyOption = PoiNearbySearchOption()
                    .location(latLng)
                    .keyword(keyword)
                    .radius(20000) // 20公里范围内
                    .pageNum(0)
                    .pageCapacity(10)
                poiSearch?.searchNearby(nearbyOption)
            }
        } else {
            poiSearch?.searchInCity(searchOption)
        }
    }

    // 检查定位权限
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )!= PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            startLocation()
        }
    }

    // 开始定位
    @SuppressLint("MissingPermission")
    private fun startLocation() {
        binding.btnLocate.text = "定位中..."
        binding.btnLocate.isEnabled = false

        if (mLocationClient?.isStarted == false) {
            mLocationClient?.start()
        } else {
            mLocationClient?.requestLocation()
        }
    }

    // 权限请求结果处理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocation()
            } else {
                Toast.makeText(this, "需要定位权限才能使用此功能", Toast.LENGTH_SHORT).show()
                binding.btnLocate.text = "获取定位"
                binding.btnLocate.isEnabled = true
            }
        }
    }

    // 清除路线和标记
    private fun clearRouteAndMarkers() {
        // 清除所有路线覆盖物
        walkingRouteOverlay?.removeFromMap()
        walkingRouteOverlay = null

        bikingRouteOverlay?.removeFromMap()
        bikingRouteOverlay = null

        drivingRouteOverlay?.removeFromMap()
        drivingRouteOverlay = null

        massTransitRouteOverlay?.removeFromMap()
        massTransitRouteOverlay = null
    }

    // 路线计算前校验
    private fun calculateRouteWithCheck() {
        clearRouteAndMarkers()

        if (currentLatLng == null) {
            Toast.makeText(this, "请先定位获取起点位置", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }

        val endAddress = binding.etEnd.text.toString().trim()
        if (endAddress.isEmpty()) {
            Toast.makeText(this, "请输入目的地", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }

        if (endLatLng == null) {
            // 提高地理编码精度：拼接城市名
            val fullAddress = if (currentCity.isNullOrEmpty()) endAddress else "${currentCity}市${endAddress}"
            geoCoder?.geocode(GeoCodeOption()
                .city(currentCity)
                .address(fullAddress))
            return
        }

        calculateRoute()
    }

    // 计算路线
    private fun calculateRoute() {
        if (currentLatLng == null || endLatLng == null || currentCity.isNullOrEmpty()) return

        binding.btnCalculate.text = "计算中..."
        binding.btnCalculate.isEnabled = false

        val startNode = PlanNode.withLocation(currentLatLng)
        val endNode = PlanNode.withLocation(endLatLng)

        when (currentRouteType) {
            RouteType.WALK -> {
                val walkOption = WalkingRoutePlanOption()
                    .from(startNode)
                    .to(endNode)
                routeSearch?.walkingSearch(walkOption)
            }
            RouteType.RIDE -> {
                val rideOption = BikingRoutePlanOption()
                    .from(startNode)
                    .to(endNode)
                    .ridingType(0)
                routeSearch?.bikingSearch(rideOption)
            }
            RouteType.BUS -> {
                // 跨城公交路线规划
                val massTransitOption = MassTransitRoutePlanOption()
                    .from(startNode)
                    .to(endNode)
                routeSearch?.masstransitSearch(massTransitOption)
            }
            RouteType.DRIVE -> {
                // 驾车路线规划
                val driveOption = DrivingRoutePlanOption()
                    .from(startNode)
                    .to(endNode)
                routeSearch?.drivingSearch(driveOption)
            }
        }
    }

    // 显示路线详情对话框
    private fun showRouteDetailDialog(title: String, content: String) {
        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogRouteDetailBinding.inflate(inflater)

        dialogBinding.routeTitle.text = title
        dialogBinding.routeDescription.text = content

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                saveTravelRecord()
            }
            .show()
    }

    // 保存出行记录
    private fun saveTravelRecord() {
        try {
            // 获取当前记录
            val existingRecord = TravelRecordManager.getRecords()

            // 计算碳排放量（根据不同出行方式计算，单位：克）
            val carbonCount = calculateCarbonEmission()

            // 获取用户信息并更新碳积分（累加逻辑）
            val user = UserMMKV.getUser()?: return
            val currentPoints = user.carbonCount
            val newPoints = currentPoints + calculateCarbonPoints(carbonCount)
            user.carbonCount = newPoints
            UserMMKV.saveUser(user)

            // 创建新的出行记录项
            val newItem = ItemTravelRecord(
                travelModel = getRouteTypeName(currentRouteType),
                travelRoute = "${binding.etStart.text} -> ${binding.etEnd.text}",
                carbonCount = carbonCount.toString(),
                distance = formatDistance(currentDistance),
                time = System.currentTimeMillis(),
                modelRavel = getRouteIconResId(currentRouteType)
            )

            // 更新总碳排放量
            val totalCarbon = existingRecord.totalCarbon.toDoubleOrNull()?: 0.0
            val newTotalCarbon = totalCarbon + carbonCount / 1000.0 // 转换为kg

            // 创建新的记录列表（添加新记录到前面）
            val newRecordList = mutableListOf<ItemTravelRecord>()
            newRecordList.add(newItem)
            newRecordList.addAll(existingRecord.list)

            // 构建新的TravelRecord对象
            val userId = user.userId?: "default_user"
            val todayCarbon = calculateTodayCarbonFromList(newRecordList)

            val updatedRecord = TravelRecord(
                userId = userId,
                totalCarbon = String.format("%.3f", newTotalCarbon),
                todayCarbon = todayCarbon,
                carbonPoint = newPoints.toString(),
                list = newRecordList
            )

            // 保存到MMKV
            TravelRecordManager.saveRecord(updatedRecord)

            // 更新UI
            refreshTravelRecordUI()

            Toast.makeText(this, "出行记录已保存，减少碳排放 $carbonCount 克", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "保存出行记录失败: ${e.message}", e)
            Toast.makeText(this, "保存记录失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    // 获取路线图标资源ID
    private fun getRouteIconResId(type: RouteType): Int {
        return try {
            when (type) {
                RouteType.WALK -> R.drawable.walk
                RouteType.RIDE -> R.drawable.bike
                RouteType.BUS -> R.drawable.bus
                RouteType.DRIVE -> R.drawable.drive_eta // 驾车图标
            }
        } catch (e: Exception) {
            // 所有图标都找不到时使用默认图标
            R.drawable.destination
        }
    }

    // 根据出行方式计算碳排放量（单位：克）
    private fun calculateCarbonEmission(): Int {
        // 不同交通方式每公里的碳排放量（克）
        val emissionPerKm = when (currentRouteType) {
            RouteType.WALK -> 0 // 步行不产生碳排放
            RouteType.RIDE -> 0 // 骑行不产生碳排放
            RouteType.BUS -> 82 // 公交每公里约82克
            RouteType.DRIVE -> 171 // 驾车每公里约171克
        }

        // 转换距离为公里（currentDistance单位是米）
        val distanceKm = currentDistance / 1000.0

        // 计算并返回碳排放量（克）
        return (distanceKm * emissionPerKm).toInt()
    }

    // 计算碳积分（100克碳排放 = 1积分）
    private fun calculateCarbonPoints(carbonCount: Int): Int {
        return (carbonCount / 100).coerceAtLeast(1) // 至少1积分
    }

    // 获取路线类型名称
    private fun getRouteTypeName(type: RouteType): String {
        return when (type) {
            RouteType.WALK -> "步行"
            RouteType.RIDE -> "骑行"
            RouteType.BUS -> "跨城公交"
            RouteType.DRIVE -> "自驾"
        }
    }

    // 刷新出行记录UI
    private fun refreshTravelRecordUI() {
        try {
            val record = TravelRecordManager.getRecords()
            val user = UserMMKV.getUser()
            if (user!= null) {
                binding.tvCarbonPoints.text = "${user.carbonCount} 积分"
            }

            val todayCarbon = calculateTodayCarbonFromList(record.list)
            binding.tvTodayCarbon.text = "今日减碳 ${todayCarbon} kg"
            binding.tvTotalCarbon.text = "${record.totalCarbon} kg"
            travelAdapter.updateList(record.list.sortedByDescending { it.time })
        } catch (e: Exception) {
            Log.e(TAG, "刷新出行记录失败: ${e.message}")
            Toast.makeText(this, "获取记录失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 计算今日减碳量
    private fun calculateTodayCarbonFromList(recordList: List<ItemTravelRecord>): String {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val todayTotal = recordList
            .filter { it.time >= todayStart }
            .sumOf { it.carbonCount.toIntOrNull()?: 0 }

        return "%.3f".format(todayTotal / 1000.0)
    }

    private fun initTravelRecord() {
        refreshTravelRecordUI()
    }

    // 步行路线结果回调
    override fun onGetWalkingRouteResult(result: WalkingRouteResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (result == null || result.error!= SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "步行路线计算失败", Toast.LENGTH_SHORT).show()
            return
        }

        if (result.routeLines.isEmpty()) {
            Toast.makeText(this, "没有找到步行路线", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalRoute = result.routeLines[0]
        clearRouteAndMarkers()

        // 保存当前路线信息
        currentDistance = optimalRoute.distance
        currentDuration = optimalRoute.duration

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的步行路线覆盖物
        walkingRouteOverlay = WalkingRouteOverlay(mBaiduMap)
        walkingRouteOverlay?.setData(optimalRoute)
        walkingRouteOverlay?.addToMap()
        walkingRouteOverlay?.zoomToSpan()

        // 生成路线描述并显示对话框
        val routeDescription = generateWalkRouteDescription(optimalRoute)
        currentRouteDescription = routeDescription

        showRouteDetailDialog(
            "步行路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})",
            routeDescription
        )
    }

    // 骑行路线结果回调
    override fun onGetBikingRouteResult(result: BikingRouteResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (result == null || result.error!= SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "骑行路线计算失败", Toast.LENGTH_SHORT).show()
            return
        }

        if (result.routeLines.isEmpty()) {
            Toast.makeText(this, "没有找到骑行路线", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalRoute = result.routeLines[0]
        clearRouteAndMarkers()

        // 保存当前路线信息
        currentDistance = optimalRoute.distance
        currentDuration = optimalRoute.duration

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的骑行路线覆盖物
        bikingRouteOverlay = BikingRouteOverlay(mBaiduMap)
        bikingRouteOverlay?.setData(optimalRoute)
        bikingRouteOverlay?.addToMap()
        bikingRouteOverlay?.zoomToSpan()

        // 生成路线描述并显示对话框
        val routeDescription = generateRideRouteDescription(optimalRoute)
        currentRouteDescription = routeDescription

        showRouteDetailDialog(
            "骑行路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})",
            routeDescription
        )
    }

    // 驾车路线结果回调
    override fun onGetDrivingRouteResult(result: DrivingRouteResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (result == null || result.error!= SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "驾车路线计算失败", Toast.LENGTH_SHORT).show()
            return
        }

        if (result.routeLines.isEmpty()) {
            Toast.makeText(this, "没有找到驾车路线", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalRoute = result.routeLines[0]
        clearRouteAndMarkers()

        // 保存当前路线信息
        currentDistance = optimalRoute.distance
        currentDuration = optimalRoute.duration

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的驾车路线覆盖物
        drivingRouteOverlay = DrivingRouteOverlay(mBaiduMap)
        drivingRouteOverlay?.setData(optimalRoute)
        drivingRouteOverlay?.addToMap()
        drivingRouteOverlay?.zoomToSpan()

        // 生成路线描述并显示对话框
        val routeDescription = generateDrivingRouteDescription(optimalRoute)
        currentRouteDescription = routeDescription

        showRouteDetailDialog(
            "驾车路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})",
            routeDescription
        )
    }

    // 跨城公交路线结果回调（修复空指针异常）
    override fun onGetMassTransitRouteResult(result: MassTransitRouteResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (result == null || result.error!= SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "跨城公交路线计算失败", Toast.LENGTH_SHORT).show()
            return
        }

        if (result.routeLines.isNullOrEmpty()) {
            Toast.makeText(this, "没有找到跨城公交路线", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalRoute = result.routeLines[0]
        clearRouteAndMarkers()

        // 保存当前路线信息
        currentDistance = optimalRoute.distance
        currentDuration = optimalRoute.duration

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的跨城公交路线覆盖物
        massTransitRouteOverlay = MassTransitRouteOverlay(mBaiduMap)
        massTransitRouteOverlay?.setData(optimalRoute)
        massTransitRouteOverlay?.addToMap()
        massTransitRouteOverlay?.zoomToSpan()

        // 生成路线描述并显示对话框（处理可能的null值）
        val routeDescription = generateMassTransitRouteDescription(optimalRoute)
        currentRouteDescription = routeDescription

        showRouteDetailDialog(
            "跨城公交路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})",
            routeDescription
        )
    }

    // 手动添加标记
    private fun addMarker(position: LatLng, iconResId: Int, title: String): Marker? {
        return try {
            // 使用 AppCompatResources 加载矢量图
            val drawable: Drawable? = ResourcesCompat.getDrawable(resources, iconResId, theme)

            if (drawable == null) {
                Log.e(TAG, "图标资源 $iconResId 加载失败，使用默认图标")
                return createDefaultMarker(position, title)
            }

            // 将Drawable转换为BitmapDescriptor
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val icon = BitmapDescriptorFactory.fromBitmap(bitmap)

            // 创建Marker
            val marker = mBaiduMap.addOverlay(
                MarkerOptions()
                    .position(position)
                    .icon(icon)
                    .title(title)
            ) as Marker
            marker
        } catch (e: Exception) {
            Log.e(TAG, "创建标记失败", e)
            Toast.makeText(this, "标记显示失败", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // 创建默认Marker
    private fun createDefaultMarker(position: LatLng, title: String): Marker? {
        return mBaiduMap.addOverlay(
            MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker))
        ) as Marker
    }

    // 调整地图视野以显示完整路线
    private fun zoomToRoute(start: RouteNode, end: RouteNode, points: List<LatLng>) {
        val startLatLng = start.location
        val endLatLng = end.location

        val boundsBuilder = LatLngBounds.Builder()
        startLatLng?.let { boundsBuilder.include(it) }
        endLatLng?.let { boundsBuilder.include(it) }
        points.forEach { boundsBuilder.include(it) }

        val mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(boundsBuilder.build())
        mBaiduMap.animateMapStatus(mapStatusUpdate)
    }

    // 未使用的接口方法
    override fun onGetTransitRouteResult(result: TransitRouteResult?) {}
    override fun onGetIndoorRouteResult(result: IndoorRouteResult?) {}
    override fun onGetIntegralRouteResult(result: IntegralRouteResult?) {}

    // 生成步行路线描述
    private fun generateWalkRouteDescription(route: WalkingRouteLine): String {
        val sb = StringBuilder()
        route.allStep.forEachIndexed { index, step ->
            sb.append("${index + 1}. ${step.instructions}\n")
        }
        sb.append("${route.allStep.size + 1}. 到达目的地")
        return sb.toString()
    }

    // 生成骑行路线描述
    private fun generateRideRouteDescription(route: BikingRouteLine): String {
        val sb = StringBuilder()
        route.allStep.forEachIndexed { index, step ->
            sb.append("${index + 1}. ${step.instructions}\n")
        }
        sb.append("${route.allStep.size + 1}. 到达目的地")
        return sb.toString()
    }

    // 生成驾车路线描述
    private fun generateDrivingRouteDescription(route: DrivingRouteLine): String {
        val sb = StringBuilder()
        route.allStep.forEachIndexed { index, step ->
            sb.append("${index + 1}. ${step.instructions}\n")
        }
        sb.append("${route.allStep.size + 1}. 到达目的地")
        return sb.toString()
    }

    // 生成跨城公交路线描述（修复空指针异常）
    private fun generateMassTransitRouteDescription(route: MassTransitRouteLine): String {
        val sb = StringBuilder()
        var stepCount = 1

        // 处理跨城公交的多层步骤结构，增加null判断
        // 处理跨城公交的多层步骤结构
        route.newSteps?.forEach { subSteps ->
            subSteps.forEach { step ->
                // 获取交通工具类型名称
                val vehicleType = when (step.vehileType) {
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN -> "火车"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_PLANE -> "飞机"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH -> "大巴"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS -> "公交"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_DRIVING -> "驾车"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK -> "步行"
                    else -> "未知"
                }

                // 构建步骤描述
                val instruction = if (step.instructions.isNullOrEmpty()) {
                    "乘坐${vehicleType}前往"
                } else {
                    "${step.instructions}（$vehicleType）"
                }

                sb.append("${stepCount}. ${instruction}\n")
                stepCount++
            }

        }
        sb.append("${stepCount}. 到达目的地")
        return sb.toString()
    }


    // 格式化距离
    private fun formatDistance(distance: Int): String {
        return if (distance < 1000) {
            "${distance}米"
        } else {
            "${"%.1f".format(distance / 1000.0)}公里"
        }
    }

    // 格式化时间
    private fun formatDuration(duration: Int): String {
        val minutes = duration / 60
        return when {
            minutes < 60 -> "${minutes}分钟"
            else -> "${minutes / 60}小时${minutes % 60}分钟"
        }
    }

    // 地理编码结果
    override fun onGetGeoCodeResult(result: GeoCodeResult?) {
        if (result == null || result.error!= SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "无法找到该地点，请尝试其他名称", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }

        endLatLng = result.location
        calculateRoute()
    }

    // 逆地理编码结果
    override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {}

    // POI搜索结果（提高终点搜索精度）
    override fun onGetPoiResult(result: PoiResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        if (result == null) {
            Toast.makeText(this, "搜索结果为空", Toast.LENGTH_SHORT).show()
            return
        }

        if (result.error!= SearchResult.ERRORNO.NO_ERROR) {
            val endAddress = binding.etEnd.text.toString().trim()
            val fullAddress = if (currentCity.isNullOrEmpty()) endAddress else "${currentCity}市${endAddress}"
            geoCoder?.geocode(GeoCodeOption().city(currentCity).address(fullAddress))
            return
        }

        // 筛选有效POI（使用for循环）
        val validPois = mutableListOf<PoiInfo>()
        for (i in result.allPoi.indices) {
            val poi = result.allPoi[i]
            if (poi.location!= null &&!poi.address.isNullOrEmpty()) {
                validPois.add(poi)
            }
        }

        if (validPois.isEmpty()) {
            Toast.makeText(this, "未找到有效地点", Toast.LENGTH_SHORT).show()
            return
        }

        // 排序POI（使用传统Comparator）
        validPois.sortWith(object : Comparator<PoiInfo> {
            override fun compare(poi1: PoiInfo, poi2: PoiInfo): Int {
                // 按类型优先级排序（基于typeCode）
                val typePriority1 = getPoiTypePriority(poi1)
                val typePriority2 = getPoiTypePriority(poi2)
                if (typePriority1!= typePriority2) {
                    return typePriority1 - typePriority2
                }

                // 按距离排序
                val distance1 = currentLatLng?.let {
                    calculateDistance(it.latitude, it.longitude, poi1.location.latitude, poi1.location.longitude)
                }?: Double.MAX_VALUE
                val distance2 = currentLatLng?.let {
                    calculateDistance(it.latitude, it.longitude, poi2.location.latitude, poi2.location.longitude)
                }?: Double.MAX_VALUE

                return distance1.compareTo(distance2)
            }
        })

        showPoiSelectionDialog(validPois)
    }
    private fun getPoiTypePriority(poi: PoiInfo): Int {
        return try {
            // 根据实际SDK的typeCode规则调整（此处为示例）
            when (poi.adCode / 1000) {
                1 -> 0 // 交通设施类
                2 -> 1 // 公共设施类
                else -> 2 // 其他类型
            }
        } catch (e: Exception) {
            2 // 异常时默认最低优先级
        }
    }

    private fun showPoiSelectionDialog(validPois: List<PoiInfo>) {
        val poiNames = validPois.map { poi ->
            val distance = currentLatLng?.let { latLng ->
                calculateDistance(
                    latLng.latitude, latLng.longitude,
                    poi.location.latitude, poi.location.longitude
                ).toInt()
            }?: 0
            "${poi.name}（${poi.type}）- ${poi.address}（${formatDistance(distance)}）"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, poiNames)

        AlertDialog.Builder(this)
            .setTitle("选择目的地（共${validPois.size}个结果）")
            .setAdapter(adapter) { dialog, which ->
                val selectedPoi = validPois[which]
                endLatLng = selectedPoi.location
                binding.etEnd.setText(selectedPoi.name)
                dialog.dismiss()
                calculateRoute() // 直接计算路线
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 计算两点之间的直线距离
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // POI详情结果
    override fun onGetPoiDetailResult(result: PoiDetailResult?) {}
    override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
    override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}

    // 生命周期管理
    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
        if (mLocationClient?.isStarted == false) {
            mLocationClient?.start()
        }
        refreshTravelRecordUI()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
        mLocationClient?.stop()
        mBaiduMap.isMyLocationEnabled = false
        routeSearch?.destroy()
        geoCoder?.destroy()
        poiSearch?.destroy()
        handler.removeCallbacksAndMessages(null)
    }
}

