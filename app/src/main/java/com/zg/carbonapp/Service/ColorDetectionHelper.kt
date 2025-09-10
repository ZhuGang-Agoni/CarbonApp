package com.zg.carbonapp.Service

import android.graphics.*
import androidx.camera.core.ImageProxy

class ColorDetectionHelper {
    
    companion object {
        // 绿色范围定义 (HSV)
        private val GREEN_LOWER = floatArrayOf(35f, 50f, 50f)  // 浅绿色
        private val GREEN_UPPER = floatArrayOf(85f, 255f, 255f) // 深绿色
        
        // 检测绿色区域
        fun detectGreenAreas(bitmap: Bitmap): List<Rect> {
            val greenAreas = mutableListOf<Rect>()
            val width = bitmap.width
            val height = bitmap.height
            
            // 转换为HSV颜色空间
            val hsvBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(hsvBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            val pixels = IntArray(width * height)
            hsvBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val hsv = FloatArray(3)
            var greenPixelCount = 0
            
            // 扫描像素检测绿色
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    Color.colorToHSV(pixel, hsv)
                    
                    if (isGreenPixel(hsv)) {
                        greenPixelCount++
                    }
                }
            }
            
            // 如果绿色像素超过阈值，认为检测到绿色区域
            val greenRatio = greenPixelCount.toFloat() / (width * height)
            if (greenRatio > 0.1f) { // 10%以上为绿色
                greenAreas.add(Rect(0, 0, width, height))
            }
            
            return greenAreas
        }
        
        private fun isGreenPixel(hsv: FloatArray): Boolean {
            return hsv[0] in GREEN_LOWER[0]..GREEN_UPPER[0] &&
                   hsv[1] in GREEN_LOWER[1]..GREEN_UPPER[1] &&
                   hsv[2] in GREEN_LOWER[2]..GREEN_UPPER[2]
        }
        
        // 从ImageProxy转换为Bitmap
        fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
} 