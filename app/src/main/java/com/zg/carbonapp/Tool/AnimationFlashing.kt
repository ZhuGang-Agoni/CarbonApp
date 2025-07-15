package com.zg.carbonapp.Tool

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.zg.carbonapp.R

object AnimationFlashing {
    fun flashView(view: View, highlightColor: Int) {
//     获取原始的一个颜色
        val originalColor = R.color.green_dark

        // 2. 创建颜色过渡动画（原始色→高亮色→原始色）
        val colorAnimator = ValueAnimator.ofArgb(
            originalColor,    // 开始颜色：原始色
            highlightColor,   // 中间颜色：高亮色
            originalColor     // 结束颜色：原始色
        ).apply {
            duration = 500    // 动画总时长（500毫秒，快速闪烁）
            interpolator = AccelerateDecelerateInterpolator() // 加速减速插值器，更自然
            addUpdateListener { animator ->
                // 3. 实时更新View的背景色
                val color = animator.animatedValue as Int
                view.setBackgroundColor(color)
            }
        }

        // 4. 启动动画
        colorAnimator.start()
    }
}