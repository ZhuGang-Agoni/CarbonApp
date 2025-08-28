package com.zg.carbonapp.ui.weather

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.zg.carbonapp.MMKV.CurrentWeatherMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.Tool.TravelRecommendationDialog
import com.zg.carbonapp.logic.model.Weather
import com.zg.carbonapp.logic.model.getSky
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherActivity : AppCompatActivity() {
    // 百度定位相关
    private lateinit var locationClient: LocationClient
    private val REQUEST_LOCATION_PERMISSION = 1001

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }
    lateinit var placeName: TextView
    lateinit var currentSky: TextView
    lateinit var currentTemp: TextView
    lateinit var currentAQI: TextView
    lateinit var nowLayout: ConstraintLayout
    lateinit var forecastLayout: LinearLayout
    lateinit var dressingText: TextView
    lateinit var coldRiskText: TextView
    lateinit var ultravioletText: TextView
    lateinit var carWashingText: TextView
    lateinit var weatherLayout: ScrollView
    lateinit var swipeRefresh: SwipeRefreshLayout
    lateinit var drawerLayout: DrawerLayout
    lateinit var backgroundImage: ImageView
//    lateinit var btnTravelRecommendation: Button // 添加出行建议按钮

    // 日期格式化工具
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
    private var isCurrentLocation = false // 标记是否是当前位置

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        // 百度地图隐私政策同意
        try {
            LocationClient.setAgreePrivacy(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 初始化定位和控件
        initBaiduLocation()
        initViews()

        // 检查Intent数据
        val hasIntentData = initDataFromIntent()
        if (!hasIntentData) {
            checkLocationPermission()
            isCurrentLocation = true // 标记为当前位置
        }

        // 观察天气数据
        observeWeatherData()

        // 初始化下拉刷新
        initSwipeRefresh()

        // 初始化抽屉
        initDrawer()

        // 添加出行建议按钮点击事件
//        btnTravelRecommendation.setOnClickListener {
//            showTravelRecommendation()
//        }
    }

    private fun initBaiduLocation() {
        locationClient = LocationClient(applicationContext)
        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setIsNeedAddress(true)
            setScanSpan(0)  // 只定位一次
            setOpenGps(true) // 开启GPS
            setCoorType("bd09ll") // 设置坐标类型
            setIsNeedLocationDescribe(true) // 获取位置描述
            setAddrType("all") // 获取详细地址信息
        }
        locationClient.locOption = option
        locationClient.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation) {
                Log.d("Location", "定位结果: ${location.addrStr}, 区县: ${location.district}")

                // 构建完整地址：中国 + 省 + 市 + 区/县
                val fullAddress = StringBuilder("中国")
                location.province?.let { fullAddress.append(" $it") }
                location.city?.let { fullAddress.append(" $it") }

                // 处理区/县级名称
                location.district?.let { district ->
                    val trimmedDistrict = when {
                        district.endsWith("区") -> district.substring(0, district.length - 1)
                        district.endsWith("市") -> district.substring(0, district.length - 1)
                        district.endsWith("县") -> district.substring(0, district.length - 1)
                        else -> district
                    }
                    fullAddress.append(" $trimmedDistrict")
                }

                // 保存定位信息
                viewModel.locationLng = location.longitude.toString()
                viewModel.locationLat = location.latitude.toString()
                viewModel.placeName = fullAddress.toString()

                Log.d("Location", "使用地点: ${viewModel.placeName}, 经纬度: ${viewModel.locationLng},${viewModel.locationLat}")
                refreshWeather()
            }
        })
    }

    private fun initViews() {
        placeName = findViewById(R.id.placeName)
        weatherLayout = findViewById(R.id.weatherLayout)
        carWashingText = findViewById(R.id.car_wash_text)
        ultravioletText = findViewById(R.id.ultravioletText)
        coldRiskText = findViewById(R.id.coldRiskText)
        dressingText = findViewById(R.id.dressingText)
        forecastLayout = findViewById(R.id.forecastLayout)
        nowLayout = findViewById(R.id.nowLayout)
        currentTemp = findViewById(R.id.currentTemp)
        currentAQI = findViewById(R.id.currentAQI)
        currentSky = findViewById(R.id.currentSky)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        drawerLayout = findViewById(R.id.drawerLayout)
        backgroundImage = findViewById(R.id.backgroundImage)

        // 添加出行建议按钮
//        btnTravelRecommendation = findViewById(R.id.btnTravelRecommendation)
    }

    private fun initDataFromIntent(): Boolean {
        val intentLng = intent.getStringExtra("location_lng")
        val intentLat = intent.getStringExtra("location_lat")
        val intentName = intent.getStringExtra("place_name")

        if (!intentLng.isNullOrEmpty() && !intentLat.isNullOrEmpty() && !intentName.isNullOrEmpty()) {
            viewModel.locationLng = intentLng
            viewModel.locationLat = intentLat
            viewModel.placeName = intentName
            return true
        }
        return false
    }

    private fun checkLocationPermission() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.start()
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_LOCATION_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                locationClient.start()
            } else {
                Toast.makeText(this, "需要定位权限才能获取当前天气", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeWeatherData() {
        viewModel.weatherLiveData.observe(this, Observer { result: Result<Weather> ->
            val weather = result.getOrNull()
            if (weather != null) {
                showWeatherInfo(weather)

                // 如果是当前位置，保存天气信息
                if (isCurrentLocation) {
                    CurrentWeatherMMKV.saveWeatherInfo(weather)
                }
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            swipeRefresh.isRefreshing = false
        })
    }

    private fun initSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.white)
        swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }
    }

    private fun initDrawer() {
        val navButton: Button = findViewById(R.id.navBtn)
        navButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
    }

    fun refreshWeather() {
        if (viewModel.locationLng.isNotEmpty() && viewModel.locationLat.isNotEmpty()) {
            viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
            swipeRefresh.isRefreshing = true
        }
    }

    private fun showWeatherInfo(weather: Weather) {
        // 显示地点名称
        placeName.text = viewModel.placeName

        val realtime = weather.realtime
        val daily = weather.daily

        // 使用实时天气数据
        val realtimeSkycon = realtime.skycon

        // 填充实时天气数据
        val currentTempText = "${realtime.temperature.toInt()}℃"
        currentTemp.text = currentTempText
        currentSky.text = getSky(realtimeSkycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        currentAQI.text = currentPM25Text

        // 设置背景图
        backgroundImage.setImageResource(getSky(realtimeSkycon).bg)

        // 清空之前的预报数据
        forecastLayout.removeAllViews()

        // 构建天气信息字符串
        val stringBuilder = StringBuilder().apply {
            append("地点：${viewModel.placeName}\n")
            append("实时温度：${currentTempText}\n")
            append("实时天气状况：${getSky(realtimeSkycon).info}\n")
            append("实时空气质量指数：${currentPM25Text}\n\n")
            append("未来几日天气预报：\n")
        }

        // 显示3天预报数据
        val uniqueDates = mutableSetOf<String>()
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in daily.skycon.indices) {
            if (i >= 3) break // 只显示3天

            val skycon = daily.skycon[i]
            val dateStr = dayFormat.format(skycon.date) // 使用更友好的日期格式

            // 跳过重复日期
            if (uniqueDates.contains(dateStr)) continue
            uniqueDates.add(dateStr)

            // 获取对应日期的温度数据
            val temperature = daily.temperature[i]
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()}℃"

            // 添加预报项到布局
            val view = layoutInflater.inflate(
                R.layout.forecast_item,
                forecastLayout, false
            )
            view.findViewById<TextView>(R.id.dateInfo).text = dateStr
            view.findViewById<ImageView>(R.id.skyIcon).setImageResource(getSky(skycon.value).icon)

            // 设置天气描述文本
            val skyInfoText = getSky(skycon.value).info
            view.findViewById<TextView>(R.id.skyInfo).text = skyInfoText

            view.findViewById<TextView>(R.id.temperatureInfo).text = tempText
            forecastLayout.addView(view)

            // 拼接字符串
            stringBuilder.append("日期：${dateStr}，天气：${skyInfoText}，温度：${tempText}\n")
        }

        // 生活指数 - 确保索引有效
        val lifeIndex = daily.lifeIndex
        coldRiskText.text = if (lifeIndex.coldRisk.isNotEmpty()) lifeIndex.coldRisk[0].desc else "暂无数据"
        dressingText.text = if (lifeIndex.dressing.isNotEmpty()) lifeIndex.dressing[0].desc else "暂无数据"
        ultravioletText.text = if (lifeIndex.ultraviolet.isNotEmpty()) lifeIndex.ultraviolet[0].desc else "暂无数据"
        carWashingText.text = if (lifeIndex.carWashing.isNotEmpty()) lifeIndex.carWashing[0].desc else "暂无数据"

        stringBuilder.append("\n生活指数建议：\n")
        stringBuilder.append("感冒风险：${coldRiskText.text}\n")
        stringBuilder.append("穿衣建议：${dressingText.text}\n")
        stringBuilder.append("紫外线强度：${ultravioletText.text}\n")
        stringBuilder.append("洗车建议：${carWashingText.text}\n")

        // 生成AI建议
        val weatherSummary = stringBuilder.toString()
        val aiSuggestionContainer = findViewById<LinearLayout>(R.id.aiSuggestionContainer)
        val tvAISuggestion = findViewById<TextView>(R.id.tvAISuggestion)
        aiSuggestionContainer.visibility = View.VISIBLE
        tvAISuggestion.text = "正在生成出行建议..."

        DeepSeekHelper().sendMessageStream(
            prompt = "根据以下天气信息，生成详细的绿色出行建议 符合低碳生活：\n\n$weatherSummary",
            onChar = { char ->
                runOnUiThread {
                    tvAISuggestion.append(char.toString())
                }
            },
            onComplete = {
                runOnUiThread {
                    tvAISuggestion.append(" ✓")
                }
            },
            onError = { error ->
                runOnUiThread {
                    tvAISuggestion.text = "建议生成失败：$error"
                }
            }
        )

        weatherLayout.visibility = View.VISIBLE
        Log.d("WeatherInfo", weatherSummary)
    }

//    private fun showTravelRecommendation() {
//        val weather = CurrentWeatherMMKV.getWeatherInfo()
//        if (weather != null) {
//            TravelRecommendationDialog(this, weather).show()
//        } else {
//            Toast.makeText(this, "无可用天气信息，请先获取当前位置天气", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient.stop()
    }
}