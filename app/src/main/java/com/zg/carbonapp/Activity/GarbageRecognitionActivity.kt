package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zg.carbonapp.Dao.RecognitionRecord
import com.zg.carbonapp.Dao.UserChallengePhoto
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.MMKV.UserChallengePhotoMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.BaiduImageApi
import com.zg.carbonapp.Tool.GarbageKnowledgeBase
import com.zg.carbonapp.Tool.MyToast
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GarbageRecognitionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnSearch: ImageButton
    private lateinit var tvResult: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var loadingOverlay: RelativeLayout
    private lateinit var progressBar: ProgressBar

    private val REQUEST_IMAGE_CAPTURE = 1
    private val PERMISSION_REQUEST_CODE = 2
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_recognition)

        // 状态栏透明设置
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

        initViews()
        initListeners()

        val searchQuery = intent.getStringExtra("search_query")
        if (!searchQuery.isNullOrEmpty()) {
            etSearch.setText(searchQuery)
            searchGarbage(searchQuery)
        }
    }

    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        btnBack = findViewById(R.id.backButton)
        btnCamera = findViewById(R.id.btn_camera)
        btnSearch = findViewById(R.id.btn_search)
        tvResult = findViewById(R.id.tv_result)
        tvExplanation = findViewById(R.id.tv_explanation)
        loadingOverlay = findViewById(R.id.loading_overlay)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun initListeners() {
        btnBack.setOnClickListener { finish() }

        btnCamera.setOnClickListener {
            if (checkCameraPermission()) {
                showLoading()
                takePhoto()
            } else {
                requestCameraPermission()
            }
        }

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGarbage(query)
            } else {
                MyToast.sendToast("请输入要搜索的垃圾名称", this)
            }
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            btnSearch.performClick()
            true
        }
    }

    private fun showLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.VISIBLE
            btnCamera.isEnabled = false
            btnSearch.isEnabled = false
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            btnCamera.isEnabled = true
            btnSearch.isEnabled = true
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun takePhoto() {
        val photoFile = try {
            createImageFile()
        } catch (e: Exception) {
            MyToast.sendToast("创建照片失败，请重试", this)
            hideLoading()
            return
        }
        currentPhotoPath = photoFile.absolutePath

        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            MyToast.sendToast("未找到相机应用", this)
            hideLoading()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "GARBAGE_${timeStamp}_"
        val storageDir = getExternalFilesDir(null) ?: throw Exception("存储目录不可用")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun searchGarbage(query: String) {
        showLoading()

        // 首先尝试本地搜索
        val results = GarbageKnowledgeBase.searchGarbage(query)
        if (results.isNotEmpty()) {
            val result = results.first()
            showRecognitionResult(
                name = result.name,
                category = result.category,
                explanation = result.explanation,
                method = "search"
            )
            hideLoading()
            return
        }

        // 本地没有找到，使用网络搜索
        lifecycleScope.launch {
            try {
                val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(query)
                showRecognitionResult(query, category, explanation, "search")
            } catch (e: Exception) {
                MyToast.sendToast("查询失败: ${e.message}", this@GarbageRecognitionActivity)
            } finally {
                hideLoading()
            }
        }
    }

    private fun showRecognitionResult(
        name: String,
        category: String,
        explanation: String,
        method: String
    ) {
        runOnUiThread {
            findViewById<View>(R.id.layout_result).visibility = View.VISIBLE
            tvResult.text = "$name - $category"
            tvExplanation.text = explanation

            val record = RecognitionRecord(
                id = UUID.randomUUID().toString(),
                garbageName = name,
                category = category,
                explanation = explanation,
                imageUrl = currentPhotoPath,
                recognitionMethod = method,
                timestamp = System.currentTimeMillis()
            )
            GarbageRecordMMKV.saveRecognitionRecord(record)

            val builder = AlertDialog.Builder(this)
                .setTitle("识别结果")
                .setMessage("垃圾名称：$name\n分类：$category\n\n$explanation")
                .setPositiveButton("知道了", null)

            if (method == "camera" && currentPhotoPath != null) {
                builder.setNegativeButton("加入挑战题库") { _, _ ->
                    UserChallengePhotoMMKV.save(
                        UserChallengePhoto(
                            imagePath = currentPhotoPath!!,
                            correctCategory = category,
                            explanation = explanation,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    MyToast.sendToast("已加入挑战题库", this)
                }
            }
            builder.show()
        }
    }

    // 拍照回调：移除图片预览逻辑，直接进行识别
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                currentPhotoPath?.let { path ->
                    MyToast.sendToast("识别中...", this)

                    // 直接进行识别，不显示图片
                    BaiduImageApi.recognizeImage(path) { keyword ->
                        if (keyword != null) {
                            lifecycleScope.launch {
                                try {
                                    val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(keyword)
                                    showRecognitionResult(keyword, category, explanation, "camera")
                                } catch (e: Exception) {
                                    MyToast.sendToast("识别结果解析失败: ${e.message}", this@GarbageRecognitionActivity)
                                } finally {
                                    hideLoading()
                                }
                            }
                        } else {
                            runOnUiThread {
                                MyToast.sendToast("识别失败，请重试", this@GarbageRecognitionActivity)
                                hideLoading()
                            }
                        }
                    }
                } ?: run {
                    MyToast.sendToast("照片路径错误", this)
                    hideLoading()
                }
            } else {
                MyToast.sendToast("已取消拍照", this)
                hideLoading()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLoading()
                takePhoto()
            } else {
                MyToast.sendToast("需要相机权限才能拍照识别", this)
            }
        }
    }
}