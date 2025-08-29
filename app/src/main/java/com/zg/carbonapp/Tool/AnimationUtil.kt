package com.zg.carbonapp.Tool

import android.view.View

object AnimationUtil {
    // 旋转动画
    fun rotateAnimation(view: View, duration: Long) {
        view.animate()
            .rotationBy(360f)
            .setDuration(duration)
            .start()
    }

    // 淡入动画
    fun fadeInAnimation(view: View, duration: Long) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
}