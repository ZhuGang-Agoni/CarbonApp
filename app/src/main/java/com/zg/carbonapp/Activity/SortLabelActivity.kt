package com.zg.carbonapp.Activity


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zg.carbonapp.databinding.ActivitySortLabelBinding

class SortLabelActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySortLabelBinding
    private val CAMERA_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySortLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置按钮监听器
        binding.btnScanBarcode.setOnClickListener {
            if (checkCameraPermission()) {
                startActivity(Intent(this, ScanActivity::class.java))
            } else {
                requestCameraPermission()
            }
        }

        binding.btnObjectRecognition.setOnClickListener {
            if (checkCameraPermission()) {
//                startActivity(Intent(this, ObjectRecognitionActivity::class.java))
                Toast.makeText(this,"功能正在开发中",Toast.LENGTH_SHORT).show()
            } else {
                requestCameraPermission()
            }
        }

        binding.btnManualInput.setOnClickListener {
            startActivity(Intent(this, ManualInputActivity::class.java))
        }

        binding.btnCarbonLedger.setOnClickListener {
            startActivity(Intent(this, CarbonLedgerActivity::class.java))
        }

        // 加载累计减碳量
        loadTotalReducedCarbon()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，但不知道用户点击了哪个按钮，所以不自动跳转
                Toast.makeText(this, "相机权限已启用", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要相机权限才能使用识别功能", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadTotalReducedCarbon() {
        // 模拟加载累计减碳量
        binding.tvTotalReduced.text = "累计减碳: 5.32 kg"
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        loadTotalReducedCarbon()
    }
}