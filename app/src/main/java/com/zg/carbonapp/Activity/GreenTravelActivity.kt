package com.zg.carbonapp.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baidu.location.*
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.baidu.mapapi.search.route.*
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener
import com.baidu.mapapi.search.sug.SuggestionResult
import com.baidu.mapapi.search.sug.SuggestionSearch
import com.baidu.mapapi.search.sug.SuggestionSearchOption
import com.zg.carbonapp.Adapter.TravelRecordAdapter
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.MMKV.CurrentWeatherMMKV
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.TravelRecommendationDialog
import com.zg.carbonapp.com.baidu.mapapi.overlayutil.*
import com.zg.carbonapp.databinding.ActivityGreenTravelBinding
import com.zg.carbonapp.databinding.DialogRouteDetailBinding
import com.zg.carbonapp.ui.weather.WeatherActivity
import java.util.*
import kotlin.math.max

class GreenTravelActivity : AppCompatActivity(),
    BDLocationListener,
    OnGetRoutePlanResultListener,
    OnGetGeoCoderResultListener,
    OnGetSuggestionResultListener {

    private lateinit var binding: ActivityGreenTravelBinding
    private lateinit var travelAdapter: TravelRecordAdapter
    private var mMapView: MapView? = null
    private lateinit var mBaiduMap: BaiduMap
    private var mLocationClient: LocationClient? = null
    private var routeSearch: RoutePlanSearch? = null
    private var geoCoder: GeoCoder? = null
    private var suggestionSearch: SuggestionSearch? = null

    // 路线和标记相关
    private var currentLatLng: LatLng? = null // 当前定位坐标
    private var endLatLng: LatLng? = null // 终点坐标
    private var currentCity: String? = null // 当前城市
    private var endCity: String? = null // 终点城市

    // 覆盖物管理
    private var walkingRouteOverlay: WalkingRouteOverlay? = null
    private var bikingRouteOverlay: BikingRouteOverlay? = null
    private var drivingRouteOverlay: DrivingRouteOverlay? = null
    private var massTransitRouteOverlay: MassTransitRouteOverlay? = null

    // 记录当前路线信息（新增：保存路线描述用于记录）
    private var currentDistance: Int = 0
    private var currentDuration: Int = 0
    private var currentRouteDescription: String = "" // 当前路线的详细描述
    private var currentRouteTitle: String = "" // 当前路线的标题

    private val handler = Handler(Looper.getMainLooper())
    private val sugResults: MutableList<SuggestionResult.SuggestionInfo> = mutableListOf()

    private val TAG = "GreenTravelActivity"
    private val REQUEST_LOCATION_PERMISSION = 100

    private enum class RouteType { WALK, RIDE, BUS, DRIVE }
    private var currentRouteType: RouteType = RouteType.WALK

    // 交通枢纽类型
    private val transportHubKeywords = arrayOf(
        "火车站", "高铁站", "火车南站", "火车北站", "火车东站", "火车西站",
        "机场", "航站楼", "T1", "T2", "T3", "航站",
        "客运站", "汽车站", "长途汽车站"
    )

    // 输入框文本监听器
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.isNullOrEmpty()) {
                endLatLng = null
                endCity = null
                clearRouteAndMarkers()
                return
            }
            if (currentCity.isNullOrEmpty()) {
                Toast.makeText(this@GreenTravelActivity, "请先完成定位", Toast.LENGTH_SHORT).show()
                return
            }
            // 发起Sug检索
            suggestionSearch?.requestSuggestion(
                SuggestionSearchOption()
                    .city(currentCity)
                    .keyword(p0.toString().trim()))
        }
        override fun afterTextChanged(p0: Editable?) {}
    }

    // 定位回调
    override fun onReceiveLocation(location: BDLocation?) {
        location?.let {
            binding.btnLocate.text = "重新定位"
            binding.btnLocate.isEnabled = true

            val locData = MyLocationData.Builder()
                .accuracy(it.radius)
                .direction(it.direction)
                .latitude(it.latitude)
                .longitude(it.longitude)
                .build()
            mBaiduMap.setMyLocationData(locData)

            currentLatLng = LatLng(it.latitude, it.longitude)
            currentCity = it.city?.replace("市", "")

            binding.etStart.setText(it.addrStr)


            if (binding.etStart.text.isNotEmpty() && mBaiduMap.mapStatus.zoom < 10) {
                mBaiduMap.animateMapStatus(
                    MapStatusUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude), 16f
                    )
                )
            }
        }?: run {
            binding.btnLocate.text = "获取定位"
            binding.btnLocate.isEnabled = true
            Toast.makeText(this, "定位失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGreenTravelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 百度定位隐私政策
        LocationClient.setAgreePrivacy(true)



        binding.tvWeatherTip.setOnClickListener{
            IntentHelper.goIntent(this, WeatherActivity::class.java)
        }
        // 初始化百度地图
        mMapView = findViewById(R.id.bmapView)
        mBaiduMap = mMapView?.map!!
        mBaiduMap.isMyLocationEnabled = true
        mBaiduMap.uiSettings.apply {
            isCompassEnabled = true
            isZoomGesturesEnabled = true
        }

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

    private fun checkIfCityHasSubway(city: String?): Boolean {
        // 截至2025年已开通地铁的城市（含已运营线路）
        return city in listOf(
            // 一线及新一线城市
            "北京", "上海", "广州", "深圳", "成都", "武汉", "重庆", "南京",
            "杭州", "西安", "沈阳", "天津", "苏州", "郑州", "长沙", "大连",
            "青岛", "宁波", "东莞", "无锡",

            // 二线及省会城市
            "合肥", "福州", "厦门", "哈尔滨", "长春", "济南", "太原", "石家庄",
            "昆明", "南宁", "南昌", "贵阳", "兰州", "乌鲁木齐", "呼和浩特",
            "海口", "西宁", "银川",

            // 三线及经济发达城市
            "徐州", "常州", "南通", "佛山", "东莞", "惠州", "珠海", "中山",
            "温州", "绍兴", "嘉兴", "泉州", "烟台", "潍坊", "洛阳", "南通",
            "扬州", "镇江", "唐山", "秦皇岛", "包头", "株洲", "湘潭", "盐城",
            "绍兴", "湖州", "马鞍山", "安庆", "赣州", "上饶", "济宁", "威海"
        )
    }

    // 初始化搜索组件
    private fun initSearchComponents() {
        // 路线规划
        routeSearch = RoutePlanSearch.newInstance()
        routeSearch?.setOnGetRoutePlanResultListener(this)

        // 地理编码
        geoCoder = GeoCoder.newInstance()
        geoCoder?.setOnGetGeoCodeResultListener(this)

        // 地点输入提示
        suggestionSearch = SuggestionSearch.newInstance()
        suggestionSearch?.setOnGetSuggestionResultListener(this)
    }

    private fun initLocation() {
        mLocationClient = LocationClient(applicationContext)
        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setIsNeedAddress(true)
            setScanSpan(10000)
            setIsNeedLocationDescribe(true)
            isOpenGps = true
            setCoorType("bd09ll")
        }
        mLocationClient?.locOption = option
        mLocationClient?.registerLocationListener(this)
    }

    private fun initViews() {
        // 初始化适配器，添加点击事件
        travelAdapter = TravelRecordAdapter(emptyList(), this)
        binding.recyclerViewTravelRecord.apply {
            layoutManager = LinearLayoutManager(this@GreenTravelActivity)
            adapter = travelAdapter
            setHasFixedSize(true)
        }
        binding.cardResult.visibility = android.view.View.VISIBLE
    }

    private fun initListeners() {
        // 定位按钮
        binding.btnLocate.setOnClickListener { checkLocationPermission() }

        // 终点输入框
        binding.etEnd.addTextChangedListener(textWatcher)

        // 计算路线按钮
        binding.btnCalculate.setOnClickListener {
            val weather = CurrentWeatherMMKV.getWeatherInfo()
            if (weather != null) {
                // 计算直线距离（单位：米）
                val distanceMeters = calculateStraightDistance()
                val distanceKm = distanceMeters / 1000f // 转换为公里


                val isSameCity = currentCity != null && endCity != null &&
                        currentCity.equals(endCity, ignoreCase = true)
                val hasSubway = checkIfCityHasSubway(currentCity)

                // 判断是否是中午时段（11:00-14:00）
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val isNoon = hour in 11..14 // 11点到14点视为中午

                // 修改对话框调用，添加回调函数
                TravelRecommendationDialog(weather, distanceKm, isNoon,
                    isSameCity,hasSubway) {
                    // 对话框确定按钮点击后执行
                    calculateRouteWithCheck()
                }.show(
                    supportFragmentManager,
                    "TravelRecommendationDialog"
                )
            } else {
                Toast.makeText(this, "无可用天气信息，请先获取当前位置天气", Toast.LENGTH_SHORT).show()
            }
        }
        // 出行方式选择
        binding.travelModeGroup.setOnCheckedChangeListener { _, checkedId ->
            currentRouteType = when (checkedId) {
                binding.walkRadio.id -> RouteType.WALK
                binding.bikeRadio.id -> RouteType.RIDE
                binding.busRadio.id -> RouteType.BUS
                binding.driveRadio.id -> RouteType.DRIVE
                else -> RouteType.WALK
            }
        }

        // 刷新按钮
        binding.ivRefresh.setOnClickListener {
            refreshTravelRecordUI()
            Toast.makeText(this, "已刷新最新记录", Toast.LENGTH_SHORT).show()
        }
    }
    private fun calculateStraightDistance(): Float {
        return if (currentLatLng != null && endLatLng != null) {
            // 使用百度地图的距离计算工具
            com.baidu.mapapi.utils.DistanceUtil.getDistance(currentLatLng, endLatLng).toFloat()
        } else {
            (-1).toFloat()
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

    // 权限结果回调
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
                Toast.makeText(this, "需要定位权限", Toast.LENGTH_SHORT).show()
                binding.btnLocate.text = "获取定位"
                binding.btnLocate.isEnabled = true
            }
        }
    }

    // 清除路线覆盖物
    private fun clearRouteAndMarkers() {
        walkingRouteOverlay?.removeFromMap()
        bikingRouteOverlay?.removeFromMap()
        drivingRouteOverlay?.removeFromMap()
        massTransitRouteOverlay?.removeFromMap()
    }

    // 路线计算校验
    private fun calculateRouteWithCheck() {
        clearRouteAndMarkers()
        if (currentLatLng == null) {
            Toast.makeText(this, "请先定位", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }
        if (endLatLng == null) {
            Toast.makeText(this, "请选择目的地", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }
        calculateRoute()
    }

    // 计算路线
    private fun calculateRoute() {
        // 跨城判断：如果选择公交且是同市，则提示用户
//        if (currentRouteType == RouteType.BUS) {
//            if (currentCity!= null && endCity!= null &&
//                currentCity.equals(endCity, ignoreCase = true)) {
//                Toast.makeText(this, "同市建议选择其他出行方式（如驾车/骑行）", Toast.LENGTH_SHORT).show()
//                binding.btnCalculate.text = "计算路线"
//                binding.btnCalculate.isEnabled = true
//                return
//            }
//        }

        binding.btnCalculate.text = "计算中..."
        binding.btnCalculate.isEnabled = false
        val startNode = PlanNode.withLocation(currentLatLng)
        val endNode = PlanNode.withLocation(endLatLng)

        when (currentRouteType) {
            RouteType.WALK -> routeSearch?.walkingSearch(
                WalkingRoutePlanOption().from(startNode).to(endNode)
            )
            RouteType.RIDE -> routeSearch?.bikingSearch(
                BikingRoutePlanOption().from(startNode).to(endNode).ridingType(0)
            )
            RouteType.BUS -> routeSearch?.masstransitSearch(
                MassTransitRoutePlanOption().from(startNode).to(endNode)

            )
            RouteType.DRIVE -> routeSearch?.drivingSearch(
                DrivingRoutePlanOption().from(startNode).to(endNode)
            )
        }
    }

    // 显示路线详情对话框（复用方法，支持从记录点击唤起）
    private fun showRouteDetailDialog(title: String, content: String) {
        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogRouteDetailBinding.inflate(inflater)
        dialogBinding.routeTitle.text = title
        dialogBinding.routeDescription.text = content

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                // 只有从计算路线唤起时才保存记录（从历史记录点击不保存）
                if (currentRouteTitle == title) {
                    saveTravelRecord()
                }
            }
            .show()
    }

    // 保存出行记录（新增：保存路线描述和标题）
    private fun saveTravelRecord() {
        try {
            val existingRecord = TravelRecordManager.getRecords()
            val carbonCount = calculateCarbonEmission()
            val user = UserMMKV.getUser()?: return
            user.carbonCount += calculateCarbonPoints(carbonCount)
            UserMMKV.saveUser(user)

            val newItem = ItemTravelRecord(
                travelModel = getRouteTypeName(currentRouteType),
                travelRoute = "${binding.etStart.text} -> ${binding.etEnd.text}",
                carbonCount = carbonCount.toString(),
                distance = formatDistance(currentDistance),
                time = System.currentTimeMillis(),
                modelRavelTag = when (currentRouteType) {
                    RouteType.WALK -> "walk"
                    RouteType.RIDE -> "bike"
                    RouteType.BUS -> "bus"
                    RouteType.DRIVE -> "drive"
                },
                routeDescription = currentRouteDescription, // 保存路线描述
                routeTitle = currentRouteTitle, // 保存路线标题
                duration = formatDuration(currentDuration) // 保存时长
            )

            val newTotalCarbon = (existingRecord.totalCarbon.toDoubleOrNull()?: 0.0) + carbonCount / 1000.0
            val newRecordList = mutableListOf<ItemTravelRecord>().apply {
                add(newItem)
                addAll(existingRecord.list)
            }

            val userId = user.userId?: "default_user"
            val todayCarbon = calculateTodayCarbonFromList(newRecordList)

            TravelRecordManager.saveRecord(
                TravelRecord(
                    userId = userId,
                    totalCarbon = String.format("%.3f", newTotalCarbon),
                    todayCarbon = todayCarbon,
                    carbonPoint = user.carbonCount.toString(),
                    list = newRecordList
                )
            )
            refreshTravelRecordUI()
            Toast.makeText(this, "记录已保存，减碳 $carbonCount 克", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "保存失败: ${e.message}", e)
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 获取路线图标
    private fun getRouteIconResId(type: RouteType): Int {
        return try {
            when (type) {
                RouteType.WALK -> R.drawable.walk
                RouteType.RIDE -> R.drawable.bike
                RouteType.BUS -> R.drawable.bus
                RouteType.DRIVE -> R.drawable.drive_eta
            }
        } catch (e: Exception) {
            R.drawable.destination
        }
    }

    // 计算碳排放量
    private fun calculateCarbonEmission(): Int {
        val emissionPerKm = when (currentRouteType) {
            RouteType.WALK, RouteType.RIDE -> 0
            RouteType.BUS -> 82
            RouteType.DRIVE -> 171
        }
        return (currentDistance / 1000.0 * emissionPerKm).toInt()
    }

    // 计算碳积分
    private fun calculateCarbonPoints(carbonCount: Int) = max(carbonCount / 100, 1)

    // 获取路线名称
    private fun getRouteTypeName(type: RouteType) = when (type) {
        RouteType.WALK -> "步行"
        RouteType.RIDE -> "骑行"
        RouteType.BUS -> "跨城公交"
        RouteType.DRIVE -> "自驾"
    }

    // 刷新记录UI
    private fun refreshTravelRecordUI() {
        try {
            val record = TravelRecordManager.getRecords()
            val user = UserMMKV.getUser()
            user?.let { binding.tvCarbonPoints.text = "${it.carbonCount} 积分" }
            binding.tvTodayCarbon.text = "今日减碳 ${calculateTodayCarbonFromList(record.list)} kg"
            binding.tvTotalCarbon.text = "${record.totalCarbon} kg"
            travelAdapter.updateList(record.list.sortedByDescending { it.time })
        } catch (e: Exception) {
            Log.e(TAG, "刷新失败: ${e.message}")
            Toast.makeText(this, "获取记录失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 计算今日减碳
    private fun calculateTodayCarbonFromList(recordList: List<ItemTravelRecord>): String {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val todayTotal = recordList.filter { it.time >= todayStart }
            .sumOf { it.carbonCount.toIntOrNull()?: 0 }
        return "%.3f".format(todayTotal / 1000.0)
    }

    private fun initTravelRecord() = refreshTravelRecordUI()

    // 步行路线结果回调（统一对话框样式）
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
        currentRouteDescription = generateWalkRouteDescription(optimalRoute)
        currentRouteTitle = "步行路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})"

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的步行路线覆盖物
        walkingRouteOverlay = WalkingRouteOverlay(mBaiduMap)
        walkingRouteOverlay?.setData(optimalRoute)
        walkingRouteOverlay?.addToMap()
        walkingRouteOverlay?.zoomToSpan()

        // 显示对话框
        showRouteDetailDialog(currentRouteTitle, currentRouteDescription)
    }

    // 骑行路线结果回调（统一对话框样式）
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
        currentRouteDescription = generateRideRouteDescription(optimalRoute)
        currentRouteTitle = "骑行路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})"

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的骑行路线覆盖物
        bikingRouteOverlay = BikingRouteOverlay(mBaiduMap)
        bikingRouteOverlay?.setData(optimalRoute)
        bikingRouteOverlay?.addToMap()
        bikingRouteOverlay?.zoomToSpan()

        // 显示对话框
        showRouteDetailDialog(currentRouteTitle, currentRouteDescription)
    }

    // 驾车路线结果回调（统一对话框样式）
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
        currentRouteDescription = generateDrivingRouteDescription(optimalRoute)
        currentRouteTitle = "驾车路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})"

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 使用SDK提供的驾车路线覆盖物
        drivingRouteOverlay = DrivingRouteOverlay(mBaiduMap)
        drivingRouteOverlay?.setData(optimalRoute)
        drivingRouteOverlay?.addToMap()
        drivingRouteOverlay?.zoomToSpan()

        // 显示对话框
        showRouteDetailDialog(currentRouteTitle, currentRouteDescription)
    }

    // 跨城公交路线结果回调（保持原样式）
    override fun onGetMassTransitRouteResult(result: MassTransitRouteResult?) {
        binding.btnCalculate.text = "计算路线"
        binding.btnCalculate.isEnabled = true

        // 错误日志与基础校验
        if (result == null) {
            Log.e(TAG, "跨城公交结果为空")
            Toast.makeText(this, "跨城公交路线结果为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            Log.e(TAG, "跨城公交错误: ${result.error.name}")
            val errorMsg = when (result.error) {
                SearchResult.ERRORNO.NETWORK_ERROR -> "网络异常，请检查网络"
                SearchResult.ERRORNO.PERMISSION_UNFINISHED -> "权限未授权"
                SearchResult.ERRORNO.RESULT_NOT_FOUND -> "无符合条件的路线"
                else -> "路线计算失败: ${result.error.name}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }
        if (result.routeLines.isNullOrEmpty()) {
            Log.e(TAG, "跨城公交无可用路线")
            Toast.makeText(this, "没有找到跨城公交路线", Toast.LENGTH_SHORT).show()
            return
        }

        // 优化路线选择：优先考虑使用交通枢纽的路线
        val optimalRoute = findRouteWithTransportHubs(result.routeLines)

        // 校验路线所有路段的点数量是否满足要求
        val hasValidSegments = checkRouteSegmentsValidity(optimalRoute)
        if (!hasValidSegments) {
            Log.e(TAG, "跨城公交路线存在无效路段（点数量<2）")
            Toast.makeText(this, "路线数据异常，无法绘制完整路线", Toast.LENGTH_SHORT).show()
            // 仍然显示路线信息，但不绘制覆盖物
            val routeDescription = generateMassTransitRouteDescription(optimalRoute)
            currentRouteDescription = routeDescription
            currentRouteTitle = "跨城公交路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})"
            showRouteDetailDialog(currentRouteTitle, currentRouteDescription)
            return
        }

        clearRouteAndMarkers()
        currentDistance = optimalRoute.distance
        currentDuration = optimalRoute.duration
        currentRouteDescription = generateMassTransitRouteDescription(optimalRoute)
        currentRouteTitle = "跨城公交路线 (${formatDistance(optimalRoute.distance)}，${formatDuration(optimalRoute.duration)})"

        // 更新结果显示
        binding.tvDistanceResult.text = "距离：${formatDistance(currentDistance)}"
        binding.tvCarbonResult.text = "预计减碳：${String.format("%.3f", calculateCarbonEmission() / 1000.0)} kg"

        // 覆盖物处理
        massTransitRouteOverlay = MassTransitRouteOverlay(mBaiduMap)
        massTransitRouteOverlay?.setData(optimalRoute)
        try {
            massTransitRouteOverlay?.addToMap()
            massTransitRouteOverlay?.zoomToSpan()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "覆盖物绘制失败: ${e.message}", e)
            Toast.makeText(this, "路线绘制异常，已显示路线信息", Toast.LENGTH_SHORT).show()
            massTransitRouteOverlay?.removeFromMap()
        }

        // 显示对话框
        showRouteDetailDialog(currentRouteTitle, currentRouteDescription)
    }

    // 查找包含交通枢纽的路线（优先选择）
    private fun findRouteWithTransportHubs(routes: List<MassTransitRouteLine>): MassTransitRouteLine {
        // 首先尝试查找包含交通枢纽的路线
        val routesWithHubs = routes.filter { route ->
            route.newSteps?.any { subSteps ->
                subSteps.any { step ->
                    // 根据交通工具类型提取正确的站点名称（与generate方法逻辑一致）
                    val departureName = when (step.vehileType) {
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN ->
                            step.trainInfo?.departureStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH ->
                            step.coachInfo?.departureStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS ->
                            step.busInfo?.departureStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK ->
                            step.instructions?.split("步行到")?.getOrNull(0)?.removePrefix("从")?.trim()
                        else -> null
                    }

                    val arrivalName = when (step.vehileType) {
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN ->
                            step.trainInfo?.arriveStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH ->
                            step.coachInfo?.arriveStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS ->
                            step.busInfo?.arriveStation
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK ->
                            step.instructions?.split("步行到")?.getOrNull(1)?.trim()
                        else -> null
                    }

                    // 判断出发站或到达站是否为交通枢纽
                    isTransportHub(departureName) || isTransportHub(arrivalName)
                }
            } ?: false
        }

        // 如果有包含枢纽的路线，则返回第一条（通常是最优路线）
        if (routesWithHubs.isNotEmpty()) {
            return routesWithHubs[0]
        }

        // 如果没有包含枢纽的路线，则返回原始最优路线
        return routes[0]
    }

    // 判断地点是否是交通枢纽
    private fun isTransportHub(locationName: String?): Boolean {
        if (locationName.isNullOrEmpty()) return false
        return transportHubKeywords.any { keyword ->
            locationName.contains(keyword)
        }
    }

    // 校验路线所有路段的点数量是否满足要求
    private fun checkRouteSegmentsValidity(route: MassTransitRouteLine): Boolean {
        route.newSteps?.forEach { subSteps ->
            subSteps.forEach { step ->
                if (step.wayPoints.size < 2) {
                    return false
                }
            }
        }
        return true
    }

    // 步行路线描述（统一格式：与跨城公交一致）
    private fun generateWalkRouteDescription(route: WalkingRouteLine): String {
        val sb = StringBuilder()
        var stepCount = 1

        // 路线概览
        sb.append("路线概览：\n")
            .append("• 总距离：${formatDistance(route.distance)}\n")
            .append("• 预计时间：${formatDuration(route.duration)}\n")
            .append("• 交通工具：步行\n\n")

        // 详细步骤
        sb.append("详细路线：\n")
        route.allStep.forEachIndexed { index, step ->
            // 步骤类型描述
            val stepType = "🚶 步行"

            // 提取起点终点（从步骤描述中解析）
            val parts = step.instructions.split("，").firstOrNull()?.split("到")
            val departureName = parts?.getOrNull(0)?.removePrefix("从")?: "当前位置"
            val arrivalName = parts?.getOrNull(1)?: "下一地点"

            // 时间信息
            val durationText = if (step.duration > 0) "（约${formatDuration(step.duration)}）" else ""

            // 生成步骤描述
            sb.append("${stepCount}. $stepType: ")
                .append("$departureName → $arrivalName $durationText\n")

            // 添加详细说明
            if (step.instructions.isNotEmpty()) {
                sb.append("   - ${step.instructions}\n")
            }

            // 步骤间分隔（最后一步不需要）
            if (index < route.allStep.size - 1) {
                sb.append("\n⬇️ 下一段行程 ⬇️\n\n")
            }

            stepCount++
        }

        // 到达目的地
        sb.append("\n${stepCount}. 到达目的地\n")

        return sb.toString()
    }

    // 骑行路线描述（统一格式：与跨城公交一致）
    private fun generateRideRouteDescription(route: BikingRouteLine): String {
        val sb = StringBuilder()
        var stepCount = 1

        // 路线概览
        sb.append("路线概览：\n")
            .append("• 总距离：${formatDistance(route.distance)}\n")
            .append("• 预计时间：${formatDuration(route.duration)}\n")
            .append("• 交通工具：骑行\n\n")

        // 详细步骤
        sb.append("详细路线：\n")
        route.allStep.forEachIndexed { index, step ->
            // 步骤类型描述
            val stepType = "🚲 骑行"

            // 提取起点终点（从步骤描述中解析）
            val parts = step.instructions.split("，").firstOrNull()?.split("到")
            val departureName = parts?.getOrNull(0)?.removePrefix("从")?: "当前位置"
            val arrivalName = parts?.getOrNull(1)?: "下一地点"

            // 时间信息
            val durationText = if (step.duration > 0) "（约${formatDuration(step.duration)}）" else ""

            // 生成步骤描述
            sb.append("${stepCount}. $stepType: ")
                .append("$departureName → $arrivalName $durationText\n")

            // 添加详细说明
            if (step.instructions.isNotEmpty()) {
                sb.append("   - ${step.instructions}\n")
            }

            // 步骤间分隔（最后一步不需要）
            if (index < route.allStep.size - 1) {
                sb.append("\n⬇️ 下一段行程 ⬇️\n\n")
            }

            stepCount++
        }

        // 到达目的地
        sb.append("\n${stepCount}. 到达目的地\n")

        return sb.toString()
    }

    // 驾车路线描述（统一格式：与跨城公交一致）
    private fun generateDrivingRouteDescription(route: DrivingRouteLine): String {
        val sb = StringBuilder()
        var stepCount = 1

        // 路线概览
        sb.append("路线概览：\n")
            .append("• 总距离：${formatDistance(route.distance)}\n")
            .append("• 预计时间：${formatDuration(route.duration)}\n")
            .append("• 交通工具：自驾\n\n")

        // 详细步骤
        sb.append("详细路线：\n")
        route.allStep.forEachIndexed { index, step ->
            // 步骤类型描述
            val stepType = "🚗 自驾"

            // 提取起点终点（从步骤描述中解析）
            val parts = step.instructions.split("，").firstOrNull()?.split("到")
            val departureName = parts?.getOrNull(0)?.removePrefix("从")?: "当前位置"
            val arrivalName = parts?.getOrNull(1)?: "下一地点"

            // 距离和时间信息
            val distanceText = if (step.distance > 0) "${formatDistance(step.distance)}" else ""
            val durationText = if (step.duration > 0) "（约${formatDuration(step.duration)}）" else ""
            val extraInfo = if (distanceText.isNotEmpty() || durationText.isNotEmpty()) {
                " $distanceText $durationText".trim()
            } else ""

            // 生成步骤描述
            sb.append("${stepCount}. $stepType: ")
                .append("$departureName → $arrivalName$extraInfo\n")

            // 添加详细说明
            if (step.instructions.isNotEmpty()) {
                sb.append("   - ${step.instructions}\n")
            }

            // 步骤间分隔（最后一步不需要）
            if (index < route.allStep.size - 1) {
                sb.append("\n⬇️ 下一段行程 ⬇️\n\n")
            }

            stepCount++
        }

        // 到达目的地
        sb.append("\n${stepCount}. 到达目的地\n")

        return sb.toString()
    }

    // 生成跨城公交路线描述（保持原样式）
    private fun generateMassTransitRouteDescription(route: MassTransitRouteLine): String {
        val sb = StringBuilder()
        var stepCount = 1

        // 路线概览信息
        sb.append("路线概览：\n")
            .append("• 总距离：${formatDistance(route.distance)}\n")
            .append("• 预计时间：${formatDuration(route.duration)}\n")
            .append("• 交通工具：")

        // 统计交通工具类型
        val transportTypes = mutableSetOf<String>()
        route.newSteps?.forEach { subSteps ->
            subSteps.forEach { step ->
                when (step.vehileType) {
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN -> transportTypes.add("火车")
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_PLANE -> transportTypes.add("飞机")
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH -> transportTypes.add("长途大巴")
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS -> transportTypes.add("市内公交")
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK -> transportTypes.add("步行")
                    else -> transportTypes.add("公共交通")
                }
            }
        }
        sb.append(transportTypes.joinToString(" → ")).append("\n\n")

        // 详细步骤
        sb.append("详细路线：\n")
        route.newSteps?.forEachIndexed { legIndex, subSteps ->
            subSteps.forEachIndexed { stepIndex, step ->
                // 步骤类型描述
                val stepType = when (step.vehileType) {
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN -> "🚆 火车"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_PLANE -> "✈️ 飞机"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH -> "🚌 长途大巴"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS -> "🚌 公交"
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK -> "🚶 步行"
                    else -> "🚉 公共交通"
                }

                // 关键修正：根据交通工具类型提取中文站点名称
                val (departureName, arrivalName) = when (step.vehileType) {
                    // 火车：从 trainInfo 提取站点
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN -> {
                        val depart = step.trainInfo?.departureStation?: "未知起点"
                        val arrive = step.trainInfo?.arriveStation?: "未知终点"
                        Pair(depart, arrive)
                    }
                    // 长途大巴：从 coachInfo 提取站点
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH -> {
                        val depart = step.coachInfo?.departureStation?: "未知起点"
                        val arrive = step.coachInfo?.arriveStation?: "未知终点"
                        Pair(depart, arrive)
                    }
                    // 市内公交：从 busInfo 提取站点
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS -> {
                        val depart = step.busInfo?.departureStation?: "未知起点"
                        val arrive = step.busInfo?.arriveStation?: "未知终点"
                        Pair(depart, arrive)
                    }
                    // 步行：从 instructions 提取起点/终点（如“从A步行到B”）
                    MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK -> {
                        val parts = step.instructions?.split("步行到")?.map { it.trim() }
                        val depart = parts?.getOrNull(0)?.removePrefix("从")?: "步行起点"
                        val arrive = parts?.getOrNull(1)?: "步行终点"
                        Pair(depart, arrive)
                    }
                    // 其他类型默认
                    else -> Pair("未知起点", "未知终点")
                }

                // 时间信息
                val durationText = if (step.duration > 0) {
                    "（约${formatDuration(step.duration)}）"
                } else ""

                // 标记交通枢纽（基于正确的站点名称）
                val hubIndicator = if (isTransportHub(departureName) || isTransportHub(arrivalName)) {
                    "★ "
                } else ""

                // 生成步骤描述
                sb.append("${stepCount}. $hubIndicator$stepType: ")
                    .append("$departureName → $arrivalName $durationText\n")

                // 添加详细说明（如果有）
                if (!step.instructions.isNullOrEmpty()) {
                    sb.append("   - ${step.instructions}\n")
                }

                // 添加换乘提示（如果是换乘点）
                if (stepIndex < subSteps.size - 1) {
                    val nextStep = subSteps[stepIndex + 1]
                    val nextType = when (nextStep.vehileType) {
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_TRAIN -> "火车"
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_PLANE -> "飞机"
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_COACH -> "长途大巴"
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_BUS -> "公交"
                        MassTransitRouteLine.TransitStep.StepVehicleInfoType.ESTEP_WALK -> "步行"
                        else -> "下一交通工具"
                    }
                    sb.append("   ↓ 在$arrivalName 换乘$nextType ↓\n")
                }

                stepCount++
            }

            // 添加段间提示
            if (legIndex < route.newSteps.size - 1) {
                sb.append("\n⬇️ 下一段行程 ⬇️\n\n")
            }
        }

        sb.append("\n${stepCount}. 到达目的地\n")

        // 添加票价信息（如果有）
        route.price?.let {
            if (it > 0) {
                sb.append("\n票价参考：￥${String.format("%.1f", it)}元")
            }
        }

        return sb.toString()
    }

    // 格式化距离
    private fun formatDistance(distance: Int) = if (distance < 1000) "${distance}米"
    else "${"%.1f".format(distance / 1000.0)}公里"

    // 格式化时间
    private fun formatDuration(duration: Int): String {
        val minutes = duration / 60
        return if (minutes < 60) "${minutes}分钟" else "${minutes / 60}小时${minutes % 60}分钟"
    }

    // 地理编码结果
    override fun onGetGeoCodeResult(result: GeoCodeResult?) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "地点解析失败", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
            return
        }
        endLatLng = result.location

        // 从地址字符串提取城市（正则）
        var cityFromGeo = extractCityFromAddress(result.address)
        if (!cityFromGeo.isNullOrEmpty()) {
            endCity = cityFromGeo
            calculateRoute()
            return
        }

        // 发起异步反向地理编码
        if (endLatLng != null) {
            geoCoder?.reverseGeoCode(ReverseGeoCodeOption().location(endLatLng))
        } else {
            Toast.makeText(this, "无法获取终点城市信息", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
        }
    }

    // 辅助函数：从地址字符串提取城市
    private fun extractCityFromAddress(address: String?): String? {
        if (address.isNullOrEmpty()) return null
        // 匹配"XX市"格式（地极市/直辖市）
        val cityPattern = Regex("([^省]+?市)")
        val match = cityPattern.find(address)
        if (match != null) {
            return match.groupValues[1].replace("市", "")
        }
        // 匹配直辖市特殊格式
        val municipalityPattern = Regex("(北京|上海|天津|重庆).+")
        if (municipalityPattern.matches(address)) {
            return when {
                address.startsWith("北京") -> "北京"
                address.startsWith("上海") -> "上海"
                address.startsWith("天津") -> "天津"
                address.startsWith("重庆") -> "重庆"
                else -> null
            }
        }
        return null
    }

    // 逆地理编码结果
    override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {
        result?.let {
            val cityFromReverse = extractCityFromAddress(it.address)
            if (!cityFromReverse.isNullOrEmpty()) {
                endCity = cityFromReverse
                calculateRoute()
            } else {
                Toast.makeText(this, "无法获取终点城市信息", Toast.LENGTH_SHORT).show()
                binding.btnCalculate.text = "计算路线"
                binding.btnCalculate.isEnabled = true
            }
        } ?: run {
            Toast.makeText(this, "逆向地理编码失败", Toast.LENGTH_SHORT).show()
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true
        }
    }

    // Sug检索结果回调
    override fun onGetSuggestionResult(result: SuggestionResult?) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            return
        }
        sugResults.clear()
        result.allSuggestions.forEach { info ->
            if (info.pt != null) sugResults.add(info)
        }
        if (sugResults.isNotEmpty()) {
            showSugSelectionDialog()
        }
    }

    // 显示Sug检索结果对话框
    private fun showSugSelectionDialog() {
        val sugNames = sugResults.map { it.key }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sugNames)
        AlertDialog.Builder(this)
            .setTitle("选择目的地（共${sugResults.size}个结果）")
            .setAdapter(adapter) { dialog, which ->
                val selected = sugResults[which]
                endLatLng = selected.pt
                // 优先用Sug的城市信息
                if (!selected.city.isNullOrEmpty()) {
                    endCity = selected.city.replace("市", "")
                } else {
                    endCity = null
                    geoCoder?.reverseGeoCode(ReverseGeoCodeOption().location(selected.pt))
                }
                binding.etEnd.setText(selected.key)
                dialog.dismiss()
                Toast.makeText(this, "请点击计算路线获取路线信息", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 生命周期管理
    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
        if (mLocationClient?.isStarted == false) mLocationClient?.start()
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
        suggestionSearch?.destroy()
        handler.removeCallbacksAndMessages(null)
    }

    // 未使用的接口方法
    override fun onGetTransitRouteResult(result: TransitRouteResult?) {}
    override fun onGetIndoorRouteResult(result: IndoorRouteResult?) {}
    override fun onGetIntegralRouteResult(result: IntegralRouteResult?) {}
}