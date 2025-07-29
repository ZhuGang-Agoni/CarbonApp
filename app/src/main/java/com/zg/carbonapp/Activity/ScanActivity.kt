package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.zg.carbonapp.databinding.ActivityScanBinding
import com.zg.carbonapp.Repository.CarbonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private val repository by lazy { CarbonRepository() }
    private var scanJob: Job? = null
    private val CAMERA_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查相机权限
        if (!hasCameraPermission()) {
            requestCameraPermission()
        } else {
            startScanner()
        }
    }

    private fun hasCameraPermission(): Boolean {
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
                startScanner()
            } else {
                Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScanner() {
        // 设置扫描回调
        binding.barcodeScanner.barcodeView.decoderFactory = null // 使用默认解码器
        binding.barcodeScanner.setStatusText("") // 清空状态文本
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { barcode ->
                    handleScannedBarcode(barcode)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // 可以添加可视化效果
            }
        })
    }

    private fun handleScannedBarcode(barcode: String) {
        // 暂停扫描器防止多次扫描
        binding.barcodeScanner.pause()

        scanJob?.cancel()
        scanJob = CoroutineScope(Dispatchers.Main).launch {
//            showLoading(true)

            try {
                val carbon = withContext(Dispatchers.IO) {
                    repository.getCarbonFootprint(barcode)
                }

                if (carbon != null) {
                    navigateToResult(carbon)
                } else {
                    Toast.makeText(this@ScanActivity, "未找到该物品的碳足迹数据", Toast.LENGTH_SHORT).show()
                    // 2秒后恢复扫描
                    delayResumeScanner(2000)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScanActivity, "查询失败: ${e.message}", Toast.LENGTH_SHORT).show()
                delayResumeScanner(2000)
            } finally {
//                showLoading(false)
            }
        }
    }

    private fun delayResumeScanner(delay: Long) {
        binding.root.postDelayed({
            binding.barcodeScanner.resume()
        }, delay)
    }

    private fun navigateToResult(carbon: com.zg.carbonapp.Entity.CarbonFootprint) {
        val intent = Intent(this, CarbonResultActivity::class.java)
        intent.putExtra("carbon", carbon)
        startActivity(intent)
        finish()
    }

//    private fun showLoading(show: Boolean) {
//        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
//    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
        scanJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.barcodeScanner.destroyDrawingCache()
        scanJob?.cancel()
    }
}