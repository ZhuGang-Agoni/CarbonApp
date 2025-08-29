package com.zg.carbonapp.Tool

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.R

class TimelineItemDecoration : RecyclerView.ItemDecoration() {
    private val lineWidth = 4.dpToPx()
    private val dotRadius = 8.dpToPx()

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = 40.dpToPx()
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom

        val paint = Paint().apply {
            color = ContextCompat.getColor(parent.context, R.color.timeline_line)
            strokeWidth = lineWidth.toFloat()
        }

        // 绘制垂直线
        c.drawLine(left.toFloat(), top.toFloat(), left.toFloat(), bottom.toFloat(), paint)

        // 绘制时间节点
        for (i in 0 until parent.childCount) {
            parent.getChildAt(i)?.let { child ->
                val childTop = child.top + (child.height / 2)
                c.drawCircle(
                    left.toFloat(),
                    childTop.toFloat(),
                    dotRadius.toFloat(),
                    paint
                )
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}