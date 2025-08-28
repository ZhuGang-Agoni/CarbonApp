package com.zg.carbonapp.Tool

// ForestSceneManager.kt 中添加
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.Light.builder

object SceneLightHelper {
    // 添加默认环境光 + 点光源
    fun addDefaultLights(scene: Scene) {
        // 环境光（均匀照亮场景）
        val ambientLight = builder(Light.Type.DIRECTIONAL)
            .setColor(Color(0xFFFFFF)) // 白色
            .setIntensity(0.8f)        // 强度 0~1
            .build()
        scene.addChild(Node().apply { light = ambientLight })

        // 点光源（模拟太阳，从上方照射）
        val directionalLight = builder(Light.Type.DIRECTIONAL)
            .setColor(Color(0xFFFFFF))
            .setIntensity(1.2f)
//            .set(Vector3(0f, -1f, 0f)) // 光线方向（向下）
            .build()
        scene.addChild(Node().apply {
            light = directionalLight
            localPosition = Vector3(0f, 5f, 0f) // 光源位置（场景上方）
        })
    }
}