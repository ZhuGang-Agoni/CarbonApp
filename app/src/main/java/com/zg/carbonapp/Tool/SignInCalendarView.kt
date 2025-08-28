package com.zg.carbonapp.Tool

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.CalendarView
import com.zg.carbonapp.R
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class SignInCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CalendarView(context, attrs, defStyleAttr) {

    var signedDates: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val dotPaint = Paint().apply {
        color = context.getColor(R.color.primary)
        isAntiAlias = true
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val cellWidth by lazy { width / 7f } // 估算每个日期单元格宽度（一周7天）
    private val cellHeight by lazy { height / 7f } // 估算每个日期单元格高度（最多显示7行）

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas) // 先绘制原生日历
        drawSignedDots(canvas) // 再绘制标记（避免覆盖原生内容）
    }

    private fun drawSignedDots(canvas: Canvas) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 获取当月第一天是星期几（1=周日，2=周一...7=周六）
        calendar.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 遍历当月所有日期
        for (day in 1..getMaxDaysInMonth(currentYear, currentMonth)) {
            val dateStr = LocalDate.of(currentYear, currentMonth + 1, day).format(dateFormatter)
            if (signedDates.contains(dateStr)) {
                // 计算日期在日历中的位置（第几行第几列）
                val position = day + firstDayOfWeek - 2 // 转换为0开始的索引
                val row = position / 7 // 行（0开始）
                val column = position % 7 // 列（0开始）

                // 计算绘制坐标（基于估算的单元格大小）
                val centerX = (column * cellWidth + cellWidth / 2).toFloat()
                val centerY = (row * cellHeight + cellHeight * 0.7f).toFloat() // 日期文字下方
                val dotRadius = dp2px(3f) // 圆点半径

                canvas.drawCircle(centerX, centerY, dotRadius, dotPaint)
            }
        }
    }

    // 获取当月最大天数
    private fun getMaxDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            Calendar.FEBRUARY -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun dp2px(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}