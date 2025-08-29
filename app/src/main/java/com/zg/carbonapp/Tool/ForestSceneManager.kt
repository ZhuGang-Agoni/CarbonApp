package com.zg.carbonapp.Tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color as ArColor
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.zg.carbonapp.Dao.TreeModel
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.SceneLightHelper
import kotlin.math.cos
import kotlin.math.sin
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ForestSceneManager(private val context: Context, private val sceneView: SceneView) {
    private val treeNodes = mutableListOf<Node>()
    private lateinit var treeRenderable: ModelRenderable
    private lateinit var groundNode: Node
    private val scene: Scene = sceneView.scene

    fun initScene(onReady: () -> Unit) {
        createCubeAsTree { renderable ->
            treeRenderable = renderable
            createGround()
            // 关键：添加光源！
            SceneLightHelper.addDefaultLights(scene)
            onReady()
        }
    }

    // 创建一个绿色立方体作为树木替代物
    private fun createCubeAsTree(callback: (ModelRenderable) -> Unit) {
        MaterialFactory.makeOpaqueWithColor(context, ArColor(0xFF4CAF50.toInt())) // 绿色
            .thenAccept { material ->
                // 创建一个类似树木形状的立方体（高大于宽）
                val treeCube = ShapeFactory.makeCube(
                    Vector3(0.3f, 1.5f, 0.3f),  // 尺寸：宽、高、深
                    Vector3(0f, 0.75f, 0f),     // 中心点位置（y轴向上偏移一半高度）
                    material
                )
                callback(treeCube)
            }
    }

    private fun createGround() {
        MaterialFactory.makeOpaqueWithColor(context, ArColor(context.getColor(R.color.ground_color)))
            .thenAccept { material ->
                // 使用立方体创建扁平的平面效果
                val groundRenderable = ShapeFactory.makeCube(
                    Vector3(10f, 0.01f, 10f), // 非常薄的立方体模拟平面
                    Vector3(0f, -0.005f, 0f), // 稍微下移确保树木在上面
                    material
                )
                groundNode = Node().apply {
                    renderable = groundRenderable
                    localPosition = Vector3(0f, 0f, 0f)
                    name = "ground"
                }
                scene.addChild(groundNode)
            }
    }

    fun placeAllTrees(trees: List<TreeModel>) {
        treeNodes.forEach { scene.removeChild(it) }
        treeNodes.clear()

        val count = trees.size
        if (count == 0) return

        val radius = 2f + (count * 0.3f).coerceAtMost(8f)

        trees.forEachIndexed { index, tree ->
            val angle = (index * 2 * Math.PI / count).toFloat()
            val x = radius * cos(angle).toFloat()
            val z = radius * sin(angle).toFloat()

            val treeNode = Node().apply {
                renderable = treeRenderable
                localPosition = Vector3(x, 0.1f, z)
                localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
                localScale = Vector3(0.8f, 0.8f, 0.8f)
                name = "tree_${tree.id}"
            }

            treeNode.setOnTapListener { _, _ ->
                (context as? OnTreeClickListener)?.onTreeClick(tree)
            }

            scene.addChild(treeNode)
            treeNodes.add(treeNode)
        }

        val camera = scene.camera
        camera.localPosition = Vector3(0f, 3f, radius + 2f)
        camera.localRotation = Quaternion.lookRotation(
            Vector3(0f, -1f, -radius),
            Vector3(0f, 1f, 0f)
        )
    }

    fun captureScreenshot(): Bitmap {
        // 处理无效尺寸的情况
        if (sceneView.width <= 0 || sceneView.height <= 0) {
            return createPlaceholderBitmap(300, 300, Color.GREEN)
        }

        // 创建目标位图
        val bitmap = Bitmap.createBitmap(
            sceneView.width,
            sceneView.height,
            Bitmap.Config.ARGB_8888
        ) ?: return createPlaceholderBitmap(300, 300, Color.GREEN)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureWithPixelCopy(bitmap)
        } else {
            // Android O以下版本使用备选方案
            captureLegacyScreenshot()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureWithPixelCopy(targetBitmap: Bitmap): Bitmap {
        val latch = CountDownLatch(1)
        var success = false

        // 在主线程执行PixelCopy请求
        Handler(Looper.getMainLooper()).post {
            try {
                PixelCopy.request(
                    sceneView as SurfaceView,
                    targetBitmap,
                    { copyResult ->
                        success = (copyResult == PixelCopy.SUCCESS)
                        latch.countDown()
                    },
                    Handler(Looper.getMainLooper()))
            } catch (e: IllegalStateException) {
                // 处理视图未附加的情况
                latch.countDown()
            }
        }

        // 等待截图完成（最多等待2秒）
        latch.await(2, TimeUnit.SECONDS)
        return if (success) targetBitmap else createPlaceholderBitmap(
            sceneView.width,
            sceneView.height,
            Color.GRAY
        )
    }

    private fun captureLegacyScreenshot(): Bitmap {
        // 创建备用位图
        val width = if (sceneView.width > 0) sceneView.width else 300
        val height = if (sceneView.height > 0) sceneView.height else 300

        return createPlaceholderBitmap(width, height, context.getColor(R.color.ground_color))
    }

    private fun createPlaceholderBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    interface OnTreeClickListener {
        fun onTreeClick(tree: TreeModel)
    }
}
