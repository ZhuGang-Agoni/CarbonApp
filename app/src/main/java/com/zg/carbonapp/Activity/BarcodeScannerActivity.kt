package com.zg.carbonapp.Activity
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.zg.carbonapp.DB.ProductCarbonDB
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import java.util.concurrent.Executors
class BarcodeScannerActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null
    private val REQUEST_CAMERA = 1001
    private val barcodeReader = MultiFormatReader ().apply {
        val hints = HashMap<DecodeHintType, Any>()
        hints [DecodeHintType.POSSIBLE_FORMATS] = listOf (BarcodeFormat.EAN_13)
        hints [DecodeHintType.TRY_HARDER] = true
        setHints (hints)
    }
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var isScanning = true // 扫描状态锁
    private var currentImage: Image? = null // 跟踪当前获取的图像
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = this
        startScanAnimation()
    }
    private fun startScanAnimation() {
        val scanLine = findViewById<View>(R.id.scanLine)
        val anim = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0.8f
        )
        anim.duration = 2000
        anim.repeatCount = Animation.INFINITE
        anim.repeatMode = Animation.REVERSE
        scanLine.startAnimation(anim)
    }
    override fun onResume () {
        super.onResume ()
        startBackgroundThread ()
        isScanning = true
        currentImage = null // 重置图像引用
        if (textureView.isAvailable) {
            openCamera ()
        } else {
            textureView.surfaceTextureListener = this
        }
    }
    override fun onPause () {
        closeCamera ()
        stopBackgroundThread ()
// 确保所有图像都被关闭
        currentImage?.close ()
        currentImage = null
        super.onPause ()
    }
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("BarcodeScannerBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("BarcodeScanner", "Error stopping background thread", e)
        }
    }
    @SuppressLint ("MissingPermission")
    private fun openCamera () {
        val cameraManager = getSystemService (CAMERA_SERVICE) as CameraManager
        try {
// 选择后置摄像头
            val cameraId = cameraManager.cameraIdList.first { id ->
                val chars = cameraManager.getCameraCharacteristics (id)
                chars [CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK
            }
            val characteristics = cameraManager.getCameraCharacteristics (cameraId)
            val map = characteristics [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
// 选择合适的预览尺寸
            val previewSize = map.getOutputSizes (SurfaceTexture::class.java).firstOrNull {
                it.width == 1280 && it.height == 720
            } ?: map.getOutputSizes (SurfaceTexture::class.java).first ()
            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)
// 增加 maxImages 到 4，降低资源竞争
            imageReader = ImageReader.newInstance (
                previewSize.width, previewSize.height,
                ImageFormat.YUV_420_888, 4
            ).apply {
                setOnImageAvailableListener ({reader ->
                    if (!isScanning) return@setOnImageAvailableListener
                    try {
// 先关闭之前的图像
                        currentImage?.close ()
// 获取最新图像
                        currentImage = reader.acquireLatestImage ()
                        currentImage?.let {processImage (it) }
                    } catch (e: IllegalStateException) {
                        Log.e ("BarcodeScanner", "Too many images acquired", e)
// 尝试关闭所有图像并恢复
                        recoverImageReader (reader)
                    }
                }, backgroundHandler)
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA
                )
                return
            }
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession(camera, surface)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Log.e("CameraError", "Camera error: $error")
                    runOnUiThread {
                        Toast.makeText(this@BarcodeScannerActivity,
                            "相机打开失败: $error", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraError", "Camera access exception: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e("CameraError", "Illegal argument exception: ${e.message}", e)
        } catch (e: SecurityException) {
            Log.e("CameraError", "Security exception: ${e.message}", e)
        }
    }
    // 恢复 ImageReader 状态，关闭所有未处理的图像
    private fun recoverImageReader (reader: ImageReader) {
        try {
// 循环关闭所有可用图像
            while (true) {
                val image = reader.acquireNextImage ()
                image?.close () ?: break
            }
            Log.d ("BarcodeScanner", "ImageReader recovered")
        } catch (e: Exception) {
            Log.e ("BarcodeScanner", "Error recovering ImageReader", e)
        }
    }
    private fun createCameraPreviewSession(camera: CameraDevice, surface: Surface) {
        try {
            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            previewRequestBuilder.addTarget(imageReader!!.surface)
// 设置自动对焦和曝光
            previewRequestBuilder [CaptureRequest.CONTROL_AF_MODE] =
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            previewRequestBuilder [CaptureRequest.CONTROL_AE_MODE] =
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            camera.createCaptureSession(
                listOf(surface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            val previewRequest = previewRequestBuilder.build()
                            session.setRepeatingRequest(previewRequest, null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e("CameraError", "Failed to start preview: ${e.message}", e)
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CameraError", "Configuration failed")
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            Log.e("CameraError", "Access exception: ${e.message}", e)
        }
    }
    private fun processImage(image: Image) {
        if (!isScanning) {
            image.close()
            currentImage = null
            return
        }
        Executors.newSingleThreadExecutor().execute {
            try {
                val planes = image.planes
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()
                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)
// 旋转图像以匹配屏幕方向
                val rotatedData = rotateNV21 (nv21, image.width, image.height, 90)
                val rotatedWidth = image.height
                val rotatedHeight = image.width
// 定义扫描区域（中央区域）
                val scanBoxWidth = (rotatedWidth * 0.6).toInt ()
                val scanBoxHeight = (rotatedHeight * 0.3).toInt ()
                val left = (rotatedWidth - scanBoxWidth) / 2
                val top = (rotatedHeight - scanBoxHeight) / 2
                val source = PlanarYUVLuminanceSource(
                    rotatedData,
                    rotatedWidth,
                    rotatedHeight,
                    left,
                    top,
                    scanBoxWidth,
                    scanBoxHeight,
                    false
                )
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val result = barcodeReader.decodeWithState(bitmap)

                // 在processImage()扫描成功后的逻辑中
                runOnUiThread {
                    isScanning = false
                    findViewById<View>(R.id.scanLine).clearAnimation()
                    captureSession?.stopRepeating()
                    val barcode = result.text
                    Toast.makeText(this, "扫描成功: $barcode", Toast.LENGTH_LONG).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        // 查询产品（自动匹配品牌）
                        val product = ProductCarbonDB.getProductByBarcode(barcode)
                        if (product != null) {
                            // 跳转到结果页
                            val intent = Intent(this, CarbonResultActivity::class.java)
                            intent.putExtra("product", product)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "未找到该产品的碳足迹数据", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }, 1500)
                }
            } catch (e: NotFoundException) {
// 未识别到条码，继续扫描
            } catch (e: Exception) {
                Log.e("BarcodeDebug", "解析错误: ${e.message}", e)
            } finally {
// 确保图像被关闭
                image.close ()
                currentImage = null
            }
        }
    }
    private fun rotateNV21(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        val result = ByteArray(data.size)
        if (rotation == 90) {
            var i = 0
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    result[i++] = data[y * width + x]
                }
            }
            val uvSize = width * height
            i = uvSize
            for (x in 0 until width step 2) {
                for (y in height / 2 - 1 downTo 0) {
                    result[i++] = data[uvSize + (y * width + x)]
                    result[i++] = data[uvSize + (y * width + x + 1)]
                }
            }
        } else {
            return data
        }
        return result
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.isNotEmpty () && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                if (textureView.isAvailable) {
                    openCamera ()
                }
            } else {
                Toast.makeText (this, "相机权限被拒绝，无法扫描", Toast.LENGTH_SHORT).show ()
                finish ()
            }
        }
    }
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        openCamera()
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        imageReader?.close()
        imageReader = null
        cameraDevice?.close()
        cameraDevice = null
    }
}
//package com.zg.carbonapp.Activity
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.ImageFormat
//import android.graphics.SurfaceTexture
//import android.hardware.camera2.*
//import android.media.Image
//import android.media.ImageReader
//import android.os.Bundle
//import android.os.Handler
//import android.os.HandlerThread
//import android.os.Looper
//import android.util.Log
//import android.util.Size
//import android.view.Surface
//import android.view.TextureView
//import android.view.View
//import android.view.animation.Animation
//import android.view.animation.TranslateAnimation
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.google.zxing.*
//import com.google.zxing.common.HybridBinarizer
//import com.zg.carbonapp.R
//import com.zg.carbonapp.Tool.IntentHelper
//import java.util.concurrent.Executors
//
//class BarcodeScannerActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
//
//    private lateinit var textureView: TextureView
//    private var cameraDevice: CameraDevice? = null
//    private var imageReader: ImageReader? = null
//    private var captureSession: CameraCaptureSession? = null
//    private val REQUEST_CAMERA = 1001
//    private val barcodeReader = MultiFormatReader().apply {
//        val hints = HashMap<DecodeHintType, Any>()
//        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.EAN_13)
//        hints[DecodeHintType.TRY_HARDER] = true
//        setHints(hints)
//    }
//    private var backgroundHandler: Handler? = null
//    private var backgroundThread: HandlerThread? = null
//    private var isScanning = true // 扫描状态锁：防止重复解码
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_barcode_scanner)
//        textureView = findViewById(R.id.textureView)
//        textureView.surfaceTextureListener = this
//
//        startScanAnimation()
//    }
//
//    private fun startScanAnimation() {
//        val scanLine = findViewById<View>(R.id.scanLine)
//        val anim = TranslateAnimation(
//            Animation.RELATIVE_TO_PARENT, 0f,
//            Animation.RELATIVE_TO_PARENT, 0f,
//            Animation.RELATIVE_TO_PARENT, 0f,
//            Animation.RELATIVE_TO_PARENT, 0.8f
//        )
//        anim.duration = 2000
//        anim.repeatCount = Animation.INFINITE
//        anim.repeatMode = Animation.REVERSE
//        scanLine.startAnimation(anim)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        startBackgroundThread()
//        isScanning = true // 恢复扫描状态
//        if (textureView.isAvailable) {
//            openCamera()
//        } else {
//            textureView.surfaceTextureListener = this
//        }
//    }
//
//    override fun onPause() {
//        closeCamera()
//        stopBackgroundThread()
//        super.onPause()
//    }
//
//    private fun startBackgroundThread() {
//        backgroundThread = HandlerThread("BarcodeScannerBackground").also { it.start() }
//        backgroundHandler = Handler(backgroundThread!!.looper)
//    }
//
//    private fun stopBackgroundThread() {
//        backgroundThread?.quitSafely()
//        backgroundThread = null
//        backgroundHandler = null
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun openCamera() {
//        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
//        try {
//            val cameraId = cameraManager.cameraIdList.first { id ->
//                val chars = cameraManager.getCameraCharacteristics(id)
//                chars[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK
//            }
//
//            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
//            val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
//            val previewSize = map.getOutputSizes(SurfaceTexture::class.java).firstOrNull {
//                it.width == 1280 && it.height == 720
//            } ?: map.getOutputSizes(SurfaceTexture::class.java).first()
//
//            val texture = textureView.surfaceTexture
//            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
//            val surface = Surface(texture)
//
//            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
//            imageReader?.setOnImageAvailableListener({ reader ->
//                val image = reader.acquireLatestImage()
//                image?.let { processImage(it) }
//            }, backgroundHandler)
//
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                return
//            }
//            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
//                override fun onOpened(camera: CameraDevice) {
//                    cameraDevice = camera
//                    createCameraPreviewSession(camera, surface)
//                }
//
//                override fun onDisconnected(camera: CameraDevice) {
//                    camera.close()
//                    cameraDevice = null
//                }
//
//                override fun onError(camera: CameraDevice, error: Int) {
//                    camera.close()
//                    cameraDevice = null
//                    Log.e("CameraError", "Camera error: $error")
//                }
//            }, backgroundHandler)
//
//        } catch (e: CameraAccessException) {
//            Log.e("CameraError", "Camera access exception: ${e.message}")
//        } catch (e: IllegalArgumentException) {
//            Log.e("CameraError", "Illegal argument exception: ${e.message}")
//        } catch (e: SecurityException) {
//            Log.e("CameraError", "Security exception: ${e.message}")
//        }
//    }
//
//    private fun createCameraPreviewSession(camera: CameraDevice, surface: Surface) {
//        try {
//            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//            previewRequestBuilder.addTarget(surface)
//            previewRequestBuilder.addTarget(imageReader!!.surface)
//
//            // 自动对焦 + 自动曝光
//            previewRequestBuilder[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//            previewRequestBuilder[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//
//            camera.createCaptureSession(
//                listOf(surface, imageReader!!.surface),
//                object : CameraCaptureSession.StateCallback() {
//                    override fun onConfigured(session: CameraCaptureSession) {
//                        if (cameraDevice == null) return
//                        captureSession = session
//                        try {
//                            val previewRequest = previewRequestBuilder.build()
//                            session.setRepeatingRequest(previewRequest, null, backgroundHandler)
//                        } catch (e: CameraAccessException) {
//                            Log.e("CameraError", "Failed to start preview: ${e.message}")
//                        }
//                    }
//
//                    override fun onConfigureFailed(session: CameraCaptureSession) {
//                        Log.e("CameraError", "Configuration failed")
//                    }
//                },
//                null
//            )
//        } catch (e: CameraAccessException) {
//            Log.e("CameraError", "Access exception: ${e.message}")
//        }
//    }
//
//    private fun processImage(image: Image) {
//        if (!isScanning) { // 已扫描成功，跳过后续帧
//            image.close()
//            return
//        }
//
//        Executors.newSingleThreadExecutor().execute {
//            try {
//                val planes = image.planes
//                val yBuffer = planes[0].buffer
//                val uBuffer = planes[1].buffer
//                val vBuffer = planes[2].buffer
//
//                val ySize = yBuffer.remaining()
//                val uSize = uBuffer.remaining()
//                val vSize = vBuffer.remaining()
//
//                val nv21 = ByteArray(ySize + uSize + vSize)
//                yBuffer.get(nv21, 0, ySize)
//                vBuffer.get(nv21, ySize, vSize)
//                uBuffer.get(nv21, ySize + vSize, uSize)
//
//                val rotatedData = rotateNV21(nv21, image.width, image.height, 90)
//                val rotatedWidth = image.height
//                val rotatedHeight = image.width
//
//                val scanBoxWidth = (rotatedWidth * 0.6).toInt()
//                val scanBoxHeight = (rotatedHeight * 0.3).toInt()
//                val left = (rotatedWidth - scanBoxWidth) / 2
//                val top = (rotatedHeight - scanBoxHeight) / 2
//
//                val source = PlanarYUVLuminanceSource(
//                    rotatedData,
//                    rotatedWidth,
//                    rotatedHeight,
//                    left,
//                    top,
//                    scanBoxWidth,
//                    scanBoxHeight,
//                    false
//                )
//                val bitmap = BinaryBitmap(HybridBinarizer(source))
//                val result = barcodeReader.decodeWithState(bitmap)
//
//                runOnUiThread {
//                    isScanning = false // 锁定状态，防止重复解码
//
//                    // 1. 停止扫描线动画
//                    val scanLine = findViewById<View>(R.id.scanLine)
//                    scanLine.clearAnimation()
//
//                    // 2. 停止相机预览（避免继续采集图像）
//                    captureSession?.stopRepeating()
//
//                    // 3. 显示长提示（3.5秒）
//                    Toast.makeText(this, "扫描成功: ${result.text}", Toast.LENGTH_LONG).show()
//
//                    // 4. 延迟1.5秒跳转（等Toast显示完，用户看清楚）
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        IntentHelper.goIntent(this, CarbonResultActivity::class.java)
//                        intent.putExtra("barcode", result.text)
//                        setResult(RESULT_OK, intent)
//                        finish()
//                    }, 1500)
//                }
//            } catch (e: NotFoundException) {
//                // 未找到条码，继续扫描（啥都不做）
//            } catch (e: Exception) {
//                Log.e("BarcodeDebug", "解析错误: ${e.message}", e)
//            } finally {
//                image.close()
//            }
//        }
//    }
//
//    private fun rotateNV21(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
//        val result = ByteArray(data.size)
//        if (rotation == 90) {
//            var i = 0
//            for (x in 0 until width) {
//                for (y in height - 1 downTo 0) {
//                    result[i++] = data[y * width + x]
//                }
//            }
//            val uvSize = width * height
//            i = uvSize
//            for (x in 0 until width step 2) {
//                for (y in height / 2 - 1 downTo 0) {
//                    result[i++] = data[uvSize + (y * width + x)]
//                    result[i++] = data[uvSize + (y * width + x + 1)]
//                }
//            }
//        } else {
//            return data
//        }
//        return result
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CAMERA) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (textureView.isAvailable) {
//                    openCamera()
//                }
//            } else {
//                Toast.makeText(this, "相机权限被拒绝，无法扫描", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        }
//    }
//
//    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//        openCamera()
//    }
//
//    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
//
//    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
//
//    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
//
//    private fun closeCamera() {
//        captureSession?.close()
//        captureSession = null
//        imageReader?.close()
//        imageReader = null
//        cameraDevice?.close()
//        cameraDevice = null
//    }
//}
