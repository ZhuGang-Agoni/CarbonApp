package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.zg.carbonapp.Dao.EnvData
import com.zg.carbonapp.R
import com.zg.carbonapp.ViewModel.EnergyStatisticsViewModel
import com.zg.carbonapp.databinding.ActivityEnergyAssistantBinding


class EnergyAssistantActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEnergyAssistantBinding
    private lateinit var viewModel: EnergyStatisticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化布局绑定（注意：布局文件名需改为activity_energy_assistant.xml）
        binding = ActivityEnergyAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[EnergyStatisticsViewModel::class.java]

        // 初始化模型（传入当前Activity上下文）
        viewModel.initModel(this)

        // 观察环境数据变化，更新UI
        viewModel.envData.observe(this) { data ->
            updateUIData(data)
        }

        // 观察节能建议变化
        viewModel.aiSuggestion.observe(this) { suggestion ->
            binding.suggestionText.text = suggestion
        }

        // 观察数据更新提示
        viewModel.updateNotification.observe(this) { message ->
            message?.let {
                // 显示带"生成建议"按钮的Snackbar
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("生成新建议") {
                        viewModel.generateSuggestion()
                    }
                    .setActionTextColor(resources.getColor(android.R.color.holo_blue_light))
                    .show()
            }
        }

        // 生成建议按钮点击事件
        binding.generateBtn.setOnClickListener {
            val currentData = viewModel.envData.value
            if (currentData?.predictedTemp == 0f || currentData.predictedHumidity == 0f) {
                // 预测数据异常时提示
                Toast.makeText(this, "预测数据异常，无法生成建议", Toast.LENGTH_SHORT).show()
            } else {
                binding.suggestionText.text = "正在获取节能建议..."
                viewModel.generateSuggestion()
            }
        }
    }

    // 更新UI数据
    private fun updateUIData(data: EnvData) {
        // 电池信息
        binding.batteryTempValue.text = String.format("%.1f℃", data.batteryTemp)
        binding.batteryLevelValue.text = "${data.batteryLevel}%"
        binding.chargingStatus.text = if (data.isCharging) "是" else "否"

        // 环境信息
        binding.lightLevelValue.text = "${data.lightLevel.toInt()}lx"
        binding.tempChangeRateValue.text = String.format("%.2f℃/min", data.tempChangeRate)
        binding.isNightStatus.text = if (data.isNight) "是" else "否"
        binding.pressureValue.text = String.format("%.2fhPa", data.pressure)

        // 预测信息
        binding.predictedTempValue.text = String.format("%.1f℃", data.predictedTemp)
        binding.predictedHumidityValue.text = String.format("%.1f%%", data.predictedHumidity)
    }

    // 生命周期：恢复传感器监听
    override fun onResume() {
        super.onResume()
        viewModel.startSensors(this)
        // 页面恢复时刷新数据
        viewModel.loadBatteryData(this)
    }

    // 生命周期：暂停传感器监听
    override fun onPause() {
        super.onPause()
        viewModel.stopSensors()
    }

    // 生命周期：销毁时清理资源
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopSensors()
    }
}