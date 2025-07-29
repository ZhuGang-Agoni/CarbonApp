package com.zg.carbonapp.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.*
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.route.*
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Adapter.TravelRecordAdapter
import com.zg.carbonapp.Dao.ItemTravelRecord
import com.zg.carbonapp.Dao.TravelRecord
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityGreenTravelBinding
import com.zg.carbonapp.databinding.DialogPoiListBinding
import com.zg.carbonapp.databinding.DialogRouteDetailBinding
import java.lang.Math.*
import kotlin.math.roundToInt

class GreenTravelActivity : AppCompatActivity(),
    AMapLocationListener,
    RouteSearch.OnRouteSearchListener {

    private lateinit var binding: ActivityGreenTravelBinding
    private lateinit var travelAdapter: TravelRecordAdapter // 列表适配器

    // 定位、地图相关
    private var locationClient: AMapLocationClient? = null
    private var poiSearch: PoiSearch? = null
    private var routeSearch: RouteSearch? = null
    private var startPoint: LatLonPoint? = null
    private var endPoint: LatLonPoint? = null
    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private lateinit var geocodeSearch: GeocodeSearch
    private var routePolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var currentCity: String? = null

    private val AMAP_KEY = "77760b774a262e67ef6ea8ce75a6701d"
    private val TAG = "GreenTravelActivity"

    private enum class RouteType { WALK, RIDE, BUS, SUBWAY }
    private var currentRouteType: RouteType = RouteType.WALK // 默认步行

    private var poiList: List<PoiItem> = emptyList()
    private var currentUserId = "default_user" // 默认用户ID


    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.isNullOrEmpty() || p1 > 0) {
                endPoint = null
                binding.cardResult.visibility = View.GONE
            }
        }
        override fun afterTextChanged(p0: Editable?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGreenTravelBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // 初始化地图
        initMap(savedInstanceState)
        // 初始化视图（含RecyclerView）
        initViews()
        // 初始化监听器
        initListeners()
        // 初始化出行记录
        initTravelRecord()
    }

    private fun initViews() {
        // 初始化RecyclerView
        travelAdapter = TravelRecordAdapter( emptyList<ItemTravelRecord>(),this)
        binding.recyclerViewTravelRecord.apply {
            layoutManager = LinearLayoutManager(this@GreenTravelActivity)
            adapter = travelAdapter
            setHasFixedSize(true)
        }
    }

    private fun initMap(savedInstanceState: Bundle?) {
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        MapsInitializer.setApiKey(AMAP_KEY)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        aMap = mapView.map?.apply {
            uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isZoomControlsEnabled = true
            }
            myLocationStyle = MyLocationStyle().showMyLocation(true)
            isMyLocationEnabled = true
        }

        geocodeSearch = GeocodeSearch(this).apply {
            setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(p0: GeocodeResult?, p1: Int) {}
                override fun onRegeocodeSearched(result: RegeocodeResult?, errorCode: Int) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                        binding.etStart.setText(result.regeocodeAddress.formatAddress)
                    }
                }
            })
        }
    }

    private fun initListeners() {
        // 定位按钮
        binding.btnLocate.setOnClickListener {
            if (hasLocationPermission()) startLocation()
            else requestLocationPermission()
        }

        // 终点输入框监听
        binding.etEnd.addTextChangedListener(textWatcher)
        binding.etEnd.setOnEditorActionListener { _, _, _ ->
            val keyword = binding.etEnd.text.toString().trim()
            if (keyword.isNotEmpty()) {
                searchPOI(keyword)
                true
            } else false
        }
        binding.etEnd.setOnClickListener {
            val keyword = binding.etEnd.text.toString().trim()
            if (poiList.isNotEmpty() && keyword.isNotEmpty()) {
                showPoiListDialog()
            } else if (keyword.isNotEmpty()) {
                searchPOI(keyword)
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
                binding.subwayRadio.id -> RouteType.SUBWAY
                else -> RouteType.WALK
            }
        }

        // 刷新按钮（更新出行记录）
        binding.ivRefresh.setOnClickListener {
            refreshTravelRecordUI()
            Toast.makeText(this, "已刷新最新记录", Toast.LENGTH_SHORT).show()
        }

        // 结果卡片点击显示路线详情
        binding.cardResult.setOnClickListener {
            // 实际应传入当前路线描述，这里简化处理
        }
    }

    private fun initTravelRecord() {

        // 首次加载数据
        refreshTravelRecordUI()
    }

    // 刷新出行记录UI（含RecyclerView）
    private fun refreshTravelRecordUI() {
        try {
            val record = TravelRecordManager.getRecords()
            // 更新碳账户信息 这里很细节
            val user=UserMMKV.getUser()
            if (user!=null){
                val points=user.carbonCount
                binding.tvCarbonPoints.text = "${points} 积分" // 碳积分
            }
            binding.tvTotalCarbon.text = "${record.totalCarbon} kg" // 累计减碳
            binding.tvTodayCarbon.text = "今日减碳 ${record.todayCarbon} kg" // 今日减碳

            // 更新RecyclerView数据
            travelAdapter.updateList(record.list.sortedByDescending { it.time }) // 按时间倒序
        } catch (e: Exception) {
            Log.e(TAG, "刷新出行记录失败: ${e.message}")
            Toast.makeText(this, "获取记录失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 显示POI选择对话框
    private fun showPoiListDialog() {
        if (poiList.isEmpty()) {
            Toast.makeText(this, "没有可选择的地点", Toast.LENGTH_SHORT).show()
            return
        }

        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogPoiListBinding.inflate(inflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("选择目的地")
            .setCancelable(true)
            .create()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            poiList.map { "${it.title} - ${it.snippet ?: "未知地址"}" }
        )
        dialogBinding.listView.adapter = adapter

        dialogBinding.listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPoi = poiList[position]
            val selectedPoint = selectedPoi.latLonPoint ?: run {
                Toast.makeText(this, "该地点无坐标", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@OnItemClickListener
            }

            // 校验坐标
            if (selectedPoint.latitude == 0.0 || selectedPoint.longitude == 0.0 ||
                selectedPoint.latitude !in -90.0..90.0 || selectedPoint.longitude !in -180.0..180.0
            ) {
                Toast.makeText(this, "坐标无效", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@OnItemClickListener
            }

            // 更新终点
            endPoint = selectedPoint
            binding.etEnd.setText(selectedPoi.title)
            // 添加终点标记
            val endLatLng = LatLng(selectedPoint.latitude, selectedPoint.longitude)
            endMarker?.remove()
            endMarker = aMap?.addMarker(
                MarkerOptions()
                    .position(endLatLng)
                    .title("终点")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatLng, 16f))

            dialog.dismiss()
            calculateRouteWithCheck()
        }

        dialog.show()
    }

    // 路线计算前校验
    private fun calculateRouteWithCheck() {
        if (!isValidPoint(startPoint)) {
            Toast.makeText(this, "请先定位获取起点", Toast.LENGTH_SHORT).show()
            return
        }
        if (endPoint == null) {
            Toast.makeText(this, "请选择终点", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isValidPoint(endPoint)) {
            Toast.makeText(this, "终点无效", Toast.LENGTH_SHORT).show()
            return
        }

        // 计算直线距离（小于50米提示过近）
        val distance = calculateDistance(
            startPoint!!.latitude, startPoint!!.longitude,
            endPoint!!.latitude, endPoint!!.longitude
        )
        if (distance < 50) {
            Toast.makeText(this, "起终点过近", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentCity.isNullOrEmpty()) {
            Toast.makeText(this, "请先定位获取城市", Toast.LENGTH_SHORT).show()
            return
        }

        // 开始计算路线
        calculateRoute()
    }

    // 检查坐标有效性
    private fun isValidPoint(point: LatLonPoint?): Boolean {
        return point != null &&
                point.latitude != 0.0 && point.longitude != 0.0 &&
                point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
    }

    // 开始定位
    @SuppressLint("MissingPermission")
    private fun startLocation() {
        binding.btnLocate.text = "定位中..."

        locationClient = AMapLocationClient(this).apply {
            setLocationOption(AMAP_LOCATION_OPTION)
            setLocationListener(this@GreenTravelActivity)
            startLocation()
        }
    }

    // 定位参数
    private val AMAP_LOCATION_OPTION by lazy {
        AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isNeedAddress = true
            httpTimeOut = 10000
        }
    }

    // 定位结果回调
    override fun onLocationChanged(location: AMapLocation?) {
        binding.btnLocate.text = "重新定位"

        location?.let {
            if (it.errorCode == 0) {
                startPoint = LatLonPoint(it.latitude, it.longitude)
                currentCity = it.city
                Log.d(TAG, "定位成功: ${it.latitude}, ${it.longitude}，城市: $currentCity")

                if (!isValidPoint(startPoint)) {
                    Toast.makeText(this, "定位坐标无效", Toast.LENGTH_SHORT).show()
                    return
                }

                // 添加起点标记
                val latLng = LatLng(it.latitude, it.longitude)
                startMarker?.remove()
                aMap?.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("起点")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                // 反向地理编码获取地址
                geocodeSearch.getFromLocationAsyn(RegeocodeQuery(startPoint, 200f, GeocodeSearch.AMAP))
            } else {
                Toast.makeText(this, "定位失败: ${it.errorInfo}", Toast.LENGTH_SHORT).show()
                startPoint = null
            }
        } ?: run {
            Toast.makeText(this, "定位结果为空", Toast.LENGTH_SHORT).show()
            startPoint = null
        }
    }

    // POI搜索
    private fun searchPOI(keyword: String) {
        binding.etEnd.isEnabled = false
        binding.btnCalculate.text = "搜索中..."
        binding.btnCalculate.isEnabled = false

        val query = PoiSearch.Query(keyword, "", currentCity).apply { // 限定当前城市搜索
            pageSize = 20
            pageNum = 1
            cityLimit = true
        }

        poiSearch = PoiSearch(this, query).apply {
            if (isValidPoint(startPoint)) {
                bound = PoiSearch.SearchBound(startPoint, 50000) // 以起点为中心，5公里内搜索
            }
            setOnPoiSearchListener(poiSearchListener)
            searchPOIAsyn()
        }
    }

    // POI搜索监听器
    private val poiSearchListener = object : PoiSearch.OnPoiSearchListener {
        override fun onPoiSearched(result: PoiResult?, errorCode: Int) {
            binding.etEnd.isEnabled = true
            binding.btnCalculate.text = "计算路线"
            binding.btnCalculate.isEnabled = true

            if (errorCode != AMapException.CODE_AMAP_SUCCESS) {
                val errorMsg = when (errorCode) {
                    10001 -> "Key无效"
                    12 -> "网络错误"
                    27 -> "无搜索结果"
                    else -> "错误码: $errorCode"
                }
                Toast.makeText(this@GreenTravelActivity, "搜索失败: $errorMsg", Toast.LENGTH_SHORT).show()
                poiList = emptyList()
                return
            }

            if (result == null || result.pois.isEmpty()) {
                Toast.makeText(this@GreenTravelActivity, "未找到地点", Toast.LENGTH_SHORT).show()
                poiList = emptyList()
                return
            }

            // 过滤无效坐标的POI
            poiList = result.pois.filter { poi ->
                val point = poi.latLonPoint
                point != null && point.latitude != 0.0 && point.longitude != 0.0
            }

            if (poiList.isNotEmpty()) {
                showPoiListDialog()
            } else {
                Toast.makeText(this@GreenTravelActivity, "无有效地点", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {}
    }

    // 计算路线
    private fun calculateRoute() {
        if (!isValidPoint(startPoint) || !isValidPoint(endPoint)) {
            Toast.makeText(this, "起点或终点无效", Toast.LENGTH_SHORT).show()
            return
        }

        val fromAndTo = RouteSearch.FromAndTo(startPoint, endPoint)
        routeSearch = RouteSearch(this).apply {
            setRouteSearchListener(this@GreenTravelActivity)
            when (currentRouteType) {
                RouteType.WALK -> calculateWalkRouteAsyn(RouteSearch.WalkRouteQuery(fromAndTo))
                RouteType.RIDE -> calculateRideRouteAsyn(RouteSearch.RideRouteQuery(fromAndTo))
                RouteType.BUS, RouteType.SUBWAY -> {
                    val busMode =  RouteSearch.BUS_DEFAULT
                    val busPolicy = RouteSearch.BUS_LEASE_CHANGE // 时间优先策略
                    calculateBusRouteAsyn(
                        RouteSearch.BusRouteQuery(fromAndTo, busMode, currentCity!!, busPolicy)
                    )
                }
            }
        }
    }

    // 计算两点距离（米）
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // 步行路线回调
    override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {
        handleRouteError(errorCode) {
            if (result?.paths?.isNotEmpty() == true) {
                val path = result.paths[0]
                drawWalkRoute(path)
                showRouteInfo("步行", path.distance, path.duration)
                // 生成路线描述
                val description = generateWalkRouteDescription(path)
                showRouteDetailDialog(description)
                // 保存记录
                saveTravelRecord(path.distance, path.duration, "walk")
            } else {
                Toast.makeText(this, "无步行路线", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 骑行路线回调
    override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) {
        handleRouteError(errorCode) {
            if (result?.paths?.isNotEmpty() == true) {
                val path = result.paths[0]
                drawRideRoute(path)
                showRouteInfo("骑行", path.distance, path.duration)
                val description = generateRideRouteDescription(path)
                showRouteDetailDialog(description)
                saveTravelRecord(path.distance, path.duration, "ride")
            } else {
                Toast.makeText(this, "无骑行路线", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 公交路线回调
    override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {
        handleRouteError(errorCode) {
            if (result?.paths?.isNotEmpty() == true) {
                val path = result.paths[0]
                drawBusRoute(path)
                val mode = if (currentRouteType == RouteType.SUBWAY) "地铁" else "公交"
                showRouteInfo(mode, path.distance, path.duration)
                val description = generateBusRouteDescription(path)
                showRouteDetailDialog(description)
                saveTravelRecord(path.distance, path.duration, if (currentRouteType == RouteType.SUBWAY) "subway" else "bus")
            } else {
                Toast.makeText(this, "无${if (currentRouteType == RouteType.SUBWAY) "地铁" else "公交"}路线", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDriveRouteSearched(p0: DriveRouteResult?, p1: Int) {}

    // 路线错误处理
    private fun handleRouteError(errorCode: Int, onSuccess: () -> Unit) {
        if (errorCode != AMapException.CODE_AMAP_SUCCESS) {
            val errorMsg = when (errorCode) {
                1201 -> "参数错误（缺少城市/策略）"
                1901 -> "坐标无效"
                1902 -> "无权限（检查Key）"
                32 -> "无此路线"
                else -> "错误码: $errorCode"
            }
            Toast.makeText(this, "${currentRouteType.name}路线失败: $errorMsg", Toast.LENGTH_SHORT).show()
            return
        }
        onSuccess()
    }

    // 生成步行路线描述
    private fun generateWalkRouteDescription(path: WalkPath): String {
        val steps = path.steps
        val description = StringBuilder("步行路线（${formatDistance(path.distance)}，${formatDuration(path.duration)}）\n\n")
        steps.forEachIndexed { index, step ->
            description.append("${index + 1}. ${getDirectionDescription(step.action)}，沿${step.road.ifEmpty { "当前道路" }}步行${formatDistance(step.distance)}\n")
        }
        description.append("${steps.size + 1}. 到达终点")
        return description.toString()
    }

    // 生成骑行路线描述
    private fun generateRideRouteDescription(path: RidePath): String {
        val steps = path.steps
        val description = StringBuilder("骑行路线（${formatDistance(path.distance)}，${formatDuration(path.duration)}）\n\n")
        steps.forEachIndexed { index, step ->
            description.append("${index + 1}. ${getDirectionDescription(step.action)}，沿${step.road.ifEmpty { "当前道路" }}骑行${formatDistance(step.distance)}\n")
        }
        description.append("${steps.size + 1}. 到达终点")
        return description.toString()
    }

    // 生成公交路线描述
    private fun generateBusRouteDescription(path: BusPath): String {
        val steps = path.steps
        val description = StringBuilder("${if (currentRouteType == RouteType.SUBWAY) "地铁" else "公交"}路线（${formatDistance(path.distance)}，${formatDuration(path.duration)}）\n\n")
        steps.forEachIndexed { index, step ->
            description.append("${index + 1}. ")
            if (step is BusStep) {
                step.walk?.let {
                    description.append("步行${formatDistance(it.distance)}（${getDirectionDescription(it.toString())}），")
                }
                step.busLine?.let {
                    description.append("乘坐${it.busLineName}，从${it.departureBusStation.busStationName}到${it.arrivalBusStation.busStationName}（${it.passStations.size + 1}站）\n")
                }
            }
        }
        description.append("${steps.size + 1}. 到达终点")
        return description.toString()
    }

    // 方向描述转换
    private fun getDirectionDescription(actionCode: String): String {
        return when (actionCode) {
            "0" -> "直行"
            "1" -> "左转"
            "2" -> "右转"
            "3" -> "左前方转弯"
            "4" -> "右前方转弯"
            "5" -> "左后方转弯"
            "6" -> "右后方转弯"
            "7" -> "掉头"
            else -> "直行"
        }
    }

    // 格式化距离（米→公里）
    private fun formatDistance(distance: Float): String {
        return if (distance < 1000) "${distance.roundToInt()}米" else "${"%.1f".format(distance / 1000)}公里"
    }

    // 格式化时长（秒→分/小时）
    private fun formatDuration(duration: Long): String {
        val minutes = duration / 60
        return if (minutes < 60) "${minutes}分钟" else "${minutes / 60}小时${minutes % 60}分钟"
    }

    // 显示路线详情对话框
    private fun showRouteDetailDialog(description: String) {
        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogRouteDetailBinding.inflate(inflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("路线详情")
            .setPositiveButton("确定") { d, _ -> d.dismiss() }
            .create()
        dialogBinding.routeDescription.text = description
        dialog.show()
    }

    // 绘制步行路线
    private fun drawWalkRoute(path: WalkPath) {
        val points = mutableListOf<LatLng>()
        path.steps.forEach { parsePolyline(it.polyline, points) }
        drawPolyline(points, Color.BLUE)
    }

    // 绘制骑行路线
    private fun drawRideRoute(path: RidePath) {
        val points = mutableListOf<LatLng>()
        path.steps.forEach { parsePolyline(it.polyline, points) }
        drawPolyline(points, Color.GREEN)
    }

    // 绘制公交路线
    private fun drawBusRoute(path: BusPath) {
        val points = mutableListOf<LatLng>()
        path.steps.forEach { step ->
            if (step is BusStep) {
                step.walk?.let { parsePolyline(it.polyline, points) }
                step.busLine?.let { parsePolyline(it.polyline, points) }
            }
        }
        drawPolyline(points, if (currentRouteType == RouteType.SUBWAY) Color.YELLOW else Color.RED)
    }

    // 绘制路线折线
    private fun drawPolyline(points: List<LatLng>, color: Int) {
        if (aMap == null || points.size < 2) return
        routePolyline?.remove()
        routePolyline = aMap!!.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(color)
                .geodesic(true)
        )
        // 调整地图视角
        val bounds = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
        aMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    // 解析坐标点
    private fun parsePolyline(polyline: List<LatLonPoint>, points: MutableList<LatLng>) {
        polyline.forEach { points.add(LatLng(it.latitude, it.longitude)) }
    }

    // 显示路线信息（距离、耗时）
    private fun showRouteInfo(mode: String, distance: Float, duration: Long) {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvDistanceResult.text = "距离：${"%.2f".format(distance / 1000)} 公里"
        binding.tvCarbonResult.text = "${mode}耗时：${formatDuration(duration)}"
    }

    // 保存出行记录、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、
    private fun saveTravelRecord(distance: Float, duration: Long, mode: String) {
        try {
            val currentRecord = TravelRecordManager.getRecords()
            val carbonCount = calculateCarbon(mode, distance)

            // 创建新记录项
            val newItem = ItemTravelRecord(
                travelModel = mode,
                travelRoute = "${binding.etStart.text} → ${binding.etEnd.text}",
                carbonCount = carbonCount.toString(),
                distance = "%.2f".format(distance / 1000),
                time = System.currentTimeMillis(),
                modelRavel = when (mode) {
                    "walk" -> R.drawable.walk
                    "ride" -> R.drawable.bike
                    "bus" -> R.drawable.bus
                    "subway" -> R.drawable.subway
                    else -> R.drawable.walk
                }
            )

            // 计算总碳减排（安全转换）
            val currentTotalCarbon = currentRecord.totalCarbon.toDoubleOrNull() ?: 0.0
            val newTotalCarbon = currentTotalCarbon + carbonCount

            // 计算今日碳减排（安全转换）
            val todayCarbon = calculateTodayCarbon(currentRecord, newItem, carbonCount)

            // 计算碳积分（安全转换）
            val newPoints = (newTotalCarbon / 100).toInt().toString()

            // 更新记录（确保userId不为空）
            val updatedRecord = currentRecord.copy(
                userId = currentRecord.userId.ifEmpty { "default_user" },
                totalCarbon = "%.2f".format(newTotalCarbon),
                todayCarbon = todayCarbon,
                carbonPoint = newPoints,
                list = listOf(newItem) + currentRecord.list.take(19)
            )

            // 保存记录
            TravelRecordManager.saveRecord(updatedRecord)
            refreshTravelRecordUI()
        } catch (e: Exception) {
            Log.e(TAG, "保存记录失败: ${e.message}", e)
            Toast.makeText(this, "记录保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 计算碳减排量（g）
    private fun calculateCarbon(mode: String, distance: Float): Int {
        // 参考值：不同出行方式相比自驾的碳减排量（每公里）
        return when (mode) {
            "walk", "ride" -> (distance * 150).roundToInt() // 步行/骑行：约150g/公里
            "bus" -> (distance * 80).roundToInt() // 公交：约80g/公里
            "subway" -> (distance * 100).roundToInt() // 地铁：约100g/公里
            else -> 0
        }
    }

    // 计算今日碳减排
    private fun calculateTodayCarbon(
        currentRecord: TravelRecord,
        newItem: ItemTravelRecord,
        newCarbon: Int
    ): String {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val todayTotal = currentRecord.list
            .filter { it.time >= todayStart }
            .sumOf { it.carbonCount.toIntOrNull() ?: 0 } + newCarbon
        return "%.2f".format(todayTotal.toDouble())
    }

    // 权限检查
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // 请求权限
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
    }

    // 权限回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            Toast.makeText(this, "需要定位权限", Toast.LENGTH_SHORT).show()
        }
    }

    // 生命周期管理
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