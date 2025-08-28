package com.zg.carbonapp.Activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.ar.sceneform.SceneView
import com.zg.carbonapp.Dao.TreeModel
import com.zg.carbonapp.MMKV.TreeMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.ForestSceneManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ForestActivity : AppCompatActivity(), ForestSceneManager.OnTreeClickListener {
    private lateinit var sceneView: SceneView
    private lateinit var sceneManager: ForestSceneManager
    private lateinit var trees: List<TreeModel>

    private lateinit var tvTreeCount: TextView
    private lateinit var tvTotalCarbon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest)

        // 初始化视图
        sceneView = findViewById(R.id.scene_view)
        tvTreeCount = findViewById(R.id.tv_tree_count)
        tvTotalCarbon = findViewById(R.id.tv_total_carbon)

        // 初始化场景管理器
        sceneManager = ForestSceneManager(this, sceneView)

        // 加载数据并确保至少有一棵初始树
        val storedTrees = TreeMMKV.getTrees()
        trees = if (storedTrees.isEmpty()) {
            // 仅添加这一行：如果没有树，初始化一棵默认树
            listOf(
                TreeModel(
                    id = UUID.randomUUID().toString(),
                    treeType = "初始树",
                    plantTime = System.currentTimeMillis(),
                    carbonReduction = 20.0 // 单棵树减排量
                ),
                TreeModel(
                    id = UUID.randomUUID().toString(),
                    treeType = "初始树",
                    plantTime = System.currentTimeMillis(),
                    carbonReduction = 20.0 // 单棵树减排量
                ),
                TreeModel(
                    id = UUID.randomUUID().toString(),
                    treeType = "初始树",
                    plantTime = System.currentTimeMillis(),
                    carbonReduction = 20.0 // 单棵树减排量
                )
            ).also {
                // 保存到MMKV，下次打开仍保留
                TreeMMKV.saveTrees(it)
            }
        } else {
            storedTrees
        }

        updateInfo() // 更新树木数量和碳减排

        sceneManager.initScene {
            // 模型加载完成后摆放树木（此时至少有1棵）
            sceneManager.placeAllTrees(trees)
        }

        // 绑定按钮事件
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_share).setOnClickListener { shareForest() }
    }

    // 以下代码完全不变
    private fun updateInfo() {
        val count = trees.size
        val totalCarbon = trees.sumOf { it.carbonReduction }
        tvTreeCount.text = "我的森林：$count 棵树"
        tvTotalCarbon.text = "总碳减排：%.1f kg".format(totalCarbon)
    }

    private fun shareForest() {
        if (trees.isEmpty()) {
            Toast.makeText(this, "你的森林还没有树哦~", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = sceneManager.captureScreenshot()
        saveAndShareImage(bitmap)
    }

    private fun saveAndShareImage(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "forest_$timeStamp.jpg"
            )
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, "我在低碳森林种了${trees.size}棵树，累计减排${trees.sumOf { it.carbonReduction }}kg CO₂！")
            }
            startActivity(Intent.createChooser(intent, "分享我的森林"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTreeClick(tree: TreeModel) {
        val time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tree.plantTime))
        Toast.makeText(
            this,
            "树种：${tree.treeType}\n种植时间：$time\n减排：${tree.carbonReduction}kg",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
    }
}