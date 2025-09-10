package com.zg.carbonapp.Service

import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


data class GreenSpace(
    val type: GreenSpaceType,
    val confidence: Float,
    val bounds: Rect,
    val carbonAbsorption: Double
)

enum class GreenSpaceType {
    TREE, PARK, GRASS, GARDEN
}

data class CarbonData(
    val totalAbsorption: Double,
    val spaces: List<GreenSpace>,
    val equivalent: String
)

class PseudoARService(private val context: Context) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private var isAnalyzing = false
    private var lastAnalysisTime = 0L
    private var currentAnalysisResult: CarbonData? = null
    private var isScanning = false  // 新增：扫描状态标志
    
    private var imageLabeler: com.google.mlkit.vision.label.ImageLabeler? = null
    
    // 碳吸收系数 (kg CO2/年)
    private val carbonCoefficients = mapOf(
        GreenSpaceType.TREE to 22.0,
        GreenSpaceType.PARK to 15.0,
        GreenSpaceType.GRASS to 0.5,
        GreenSpaceType.GARDEN to 1.2
    )
    
    // 绿色空间关键词 - 扩展更多关键词
    private val greenSpaceKeywords = mapOf(
        GreenSpaceType.TREE to listOf("tree", "plant", "forest", "wood", "leaf", "branch", "trunk", "foliage", "vegetation"),
        GreenSpaceType.PARK to listOf("park", "garden", "lawn", "grass", "green", "outdoor", "nature", "landscape"),
        GreenSpaceType.GRASS to listOf("grass", "lawn", "meadow", "field", "turf", "greenery"),
        GreenSpaceType.GARDEN to listOf("garden", "flower", "plant", "vegetation", "greenhouse", "botanical")
    )
    
    init {
        initResources()
    }
    
    private fun initResources() {
        if (cameraExecutor == null || cameraExecutor!!.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        if (imageLabeler == null) {
            imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        }
    }
    
    fun startARScan(lifecycleOwner: LifecycleOwner, onResult: (CarbonData) -> Unit) {
        if (isScanning) {
            Log.d("PseudoARService", "扫描已在进行中，跳过")
            return
        }
        
        isScanning = true
        initResources() // 确保资源可用
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(640, 480))
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                            processImage(imageProxy, onResult)
                        }
                    }
                
                cameraProvider?.unbindAll()
                
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                
                preview.setSurfaceProvider(null)
                
                Log.d("PseudoARService", "AR扫描开始")
                
            } catch (e: Exception) {
                Log.e("PseudoARService", "相机绑定失败", e)
                isScanning = false
            }
            
        }, ContextCompat.getMainExecutor(context))
    }
    
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy, onResult: (CarbonData) -> Unit) {
        // 控制分析频率，避免过于频繁
        val currentTime = System.currentTimeMillis()
        if (isAnalyzing || currentTime - lastAnalysisTime < 3000) {
            imageProxy.close()
            return
        }
        
        isAnalyzing = true
        lastAnalysisTime = currentTime
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            imageLabeler?.process(image)
                ?.addOnSuccessListener { labels ->
                    val greenSpaces = detectGreenSpaces(labels)
                    val carbonData = calculateCarbonAbsorption(greenSpaces)
                    currentAnalysisResult = carbonData
                    isAnalyzing = false
                    
                    // 只在检测到绿色空间时输出日志
                    if (greenSpaces.isNotEmpty()) {
                        Log.d("PseudoARService", "检测到 ${greenSpaces.size} 个绿色空间")
                    }
                    
                    onResult(carbonData)
                }
                ?.addOnFailureListener { e ->
                    Log.e("PseudoARService", "图像分析失败", e)
                    isAnalyzing = false
                    // 分析失败时使用模拟数据
                    val fallbackData = simulateARScan()
                    currentAnalysisResult = fallbackData
                    onResult(fallbackData)
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            isAnalyzing = false
        }
    }
    
    private fun detectGreenSpaces(labels: List<com.google.mlkit.vision.label.ImageLabel>): List<GreenSpace> {
        val greenSpaces = mutableListOf<GreenSpace>()
        
        labels.forEach { label ->
            val labelText = label.text.lowercase()
            val confidence = label.confidence
            
            // 检查是否匹配绿色空间关键词
            greenSpaceKeywords.forEach { (type, keywords) ->
                if (keywords.any { keyword -> labelText.contains(keyword) } && confidence > 0.3f) {
                    greenSpaces.add(
                        GreenSpace(
                            type = type,
                            confidence = confidence,
                            bounds = Rect(0, 0, 100, 100),
                            carbonAbsorption = carbonCoefficients[type] ?: 0.0
                        )
                    )
                }
            }
        }
        
        return greenSpaces
    }
    
    private fun calculateCarbonAbsorption(greenSpaces: List<GreenSpace>): CarbonData {
        val totalAbsorption = greenSpaces.sumOf { it.carbonAbsorption }
        
        val equivalent = when {
            totalAbsorption > 20 -> "相当于吸收一辆汽车行驶50公里的碳排放"
            totalAbsorption > 10 -> "相当于吸收一辆汽车行驶25公里的碳排放"
            totalAbsorption > 5 -> "相当于吸收一辆汽车行驶10公里的碳排放"
            else -> "相当于吸收一辆汽车行驶5公里的碳排放"
        }
        
        return CarbonData(
            totalAbsorption = totalAbsorption,
            spaces = greenSpaces,
            equivalent = equivalent
        )
    }
    
    fun stopARScan() {
        Log.d("PseudoARService", "停止AR扫描")
        isScanning = false
        isAnalyzing = false
        
        // 只解绑相机，不关闭资源
        cameraProvider?.unbindAll()
        imageAnalyzer = null
        camera = null
    }
    
    // 完全清理资源（只在Activity销毁时调用）
    fun cleanup() {
        Log.d("PseudoARService", "清理AR服务资源")
        stopARScan()
        
        imageLabeler?.close()
        imageLabeler = null
        
        cameraExecutor?.shutdown()
        cameraExecutor = null
        
        cameraProvider = null
    }
    
    // 分析单张图像
    fun analyzeImage(mediaImage: android.media.Image, rotationDegrees: Int, onResult: (CarbonData) -> Unit) {
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        
        imageLabeler?.process(image)
            ?.addOnSuccessListener { labels ->
                val greenSpaces = detectGreenSpaces(labels)
                val carbonData = calculateCarbonAbsorption(greenSpaces)
                currentAnalysisResult = carbonData
                onResult(carbonData)
            }
            ?.addOnFailureListener { e ->
                Log.e("PseudoARService", "图像分析失败", e)
                val fallbackData = simulateARScan()
                currentAnalysisResult = fallbackData
                onResult(fallbackData)
            }
    }
    
    // 获取当前分析结果
    fun getCurrentAnalysisResult(): CarbonData {
        return currentAnalysisResult ?: simulateARScan()
    }
    
    // 模拟AR扫描（用于演示）
    fun simulateARScan(): CarbonData {
        val simulatedSpaces = listOf(
            GreenSpace(
                type = GreenSpaceType.TREE,
                confidence = 0.85f,
                bounds = Rect(100, 150, 200, 250),
                carbonAbsorption = 22.0
            ),
            GreenSpace(
                type = GreenSpaceType.PARK,
                confidence = 0.78f,
                bounds = Rect(300, 200, 400, 300),
                carbonAbsorption = 15.0
            ),
            GreenSpace(
                type = GreenSpaceType.GRASS,
                confidence = 0.92f,
                bounds = Rect(150, 300, 250, 350),
                carbonAbsorption = 0.5
            )
        )
        
        return CarbonData(
            totalAbsorption = 37.5,
            spaces = simulatedSpaces,
            equivalent = "相当于吸收一辆汽车行驶30公里的碳排放"
        )
    }
} 