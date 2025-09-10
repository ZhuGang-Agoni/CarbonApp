package com.zg.carbonapp.View

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.zg.carbonapp.Service.GreenSpace
import com.zg.carbonapp.Service.CarbonData

class AROverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.GREEN
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.argb(180, 0, 0, 0)
    }
    
    private var carbonData: CarbonData? = null
    private var greenSpaces: List<GreenSpace> = emptyList()
    
    fun updateData(data: CarbonData) {
        this.carbonData = data
        this.greenSpaces = data.spaces
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制绿色空间边界框
        greenSpaces.forEach { space ->
            drawGreenSpaceBox(canvas, space)
        }
        
        // 绘制碳吸收信息面板
        carbonData?.let { data ->
            drawCarbonInfoPanel(canvas, data)
        }
    }
    
    private fun drawGreenSpaceBox(canvas: Canvas, space: GreenSpace) {
        val rect = space.bounds
        
        // 绘制边界框
        paint.color = when (space.type) {
            com.zg.carbonapp.Service.GreenSpaceType.TREE -> Color.GREEN
            com.zg.carbonapp.Service.GreenSpaceType.PARK -> Color.BLUE
            com.zg.carbonapp.Service.GreenSpaceType.GRASS -> Color.LTGRAY
            com.zg.carbonapp.Service.GreenSpaceType.GARDEN -> Color.MAGENTA
        }
        
        canvas.drawRect(rect, paint)
        
        // 绘制标签
        val label = when (space.type) {
            com.zg.carbonapp.Service.GreenSpaceType.TREE -> "🌳 树木"
            com.zg.carbonapp.Service.GreenSpaceType.PARK -> "🏞️ 公园"
            com.zg.carbonapp.Service.GreenSpaceType.GRASS -> "🌿 草地"
            com.zg.carbonapp.Service.GreenSpaceType.GARDEN -> "🌸 花园"
        }
        
        val labelBounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, labelBounds)
        
        // 绘制标签背景
        val labelRect = RectF(
            rect.left.toFloat(),
            rect.top - labelBounds.height() - 10f,
            rect.left + labelBounds.width() + 20f,
            rect.top.toFloat()
        )
        canvas.drawRect(labelRect, backgroundPaint)
        
        // 绘制标签文字
        canvas.drawText(label, rect.left + 10f, rect.top - 5f, textPaint)
    }
    
    private fun drawCarbonInfoPanel(canvas: Canvas, data: CarbonData) {
        val panelWidth = 400f
        val panelHeight = 200f
        val margin = 50f
        
        val panelRect = RectF(
            width - panelWidth - margin,
            margin,
            width - margin,
            margin + panelHeight
        )
        
        // 绘制面板背景
        canvas.drawRect(panelRect, backgroundPaint)
        
        // 绘制边框
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        canvas.drawRect(panelRect, paint)
        
        // 绘制标题
        textPaint.textSize = 36f
        textPaint.color = Color.GREEN
        canvas.drawText("🌱 碳吸收检测", panelRect.left + 20f, panelRect.top + 40f, textPaint)
        
        // 绘制数据
        textPaint.textSize = 28f
        textPaint.color = Color.WHITE
        
        val totalText = "总碳吸收: ${String.format("%.1f", data.totalAbsorption)} kg/年"
        canvas.drawText(totalText, panelRect.left + 20f, panelRect.top + 80f, textPaint)
        
        val spacesText = "检测到 ${data.spaces.size} 个绿色空间"
        canvas.drawText(spacesText, panelRect.left + 20f, panelRect.top + 120f, textPaint)
        
        // 绘制等效信息
        textPaint.textSize = 24f
        textPaint.color = Color.YELLOW
        val equivalentText = data.equivalent
        canvas.drawText(equivalentText, panelRect.left + 20f, panelRect.top + 160f, textPaint)
    }
} 