package com.zg.carbonapp.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.ui.place.PlaceFragment
import com.zg.carbonapp.ui.weather.WeatherActivity

class TravelHelperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_travel_helper)
        IntentHelper.goIntent(this,WeatherActivity::class.java)
//        supportFragmentManager.beginTransaction().replace(R.id.container, PlaceFragment()).commit()
    }
}