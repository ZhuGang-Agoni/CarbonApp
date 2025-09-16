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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zg.carbonapp.Dao.RecognitionRecord
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.GarbageKnowledgeBase
import com.zg.carbonapp.Tool.MyToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.zg.carbonapp.Tool.BaiduImageApi
import com.zg.carbonapp.MMKV.UserChallengePhotoMMKV
import com.zg.carbonapp.Dao.UserChallengePhoto

class GarbageRecognitionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: android.widget.ImageView
    private lateinit var btnCamera: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var ivPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var loadingLayout: View

    // 拍照请求码、权限请求码
    private val REQUEST_IMAGE_CAPTURE = 1
    private val PERMISSION_REQUEST_CODE = 2
    // 当前拍照图片路径
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_recognition)

        // 设置状态栏透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

        initViews()
        initListeners()

        // 处理从搜索框传入的查询参数
        val searchQuery = intent.getStringExtra("search_query")
        if (!searchQuery.isNullOrEmpty()) {
            etSearch.setText(searchQuery)
            searchGarbage(searchQuery)
        }
    }

    // 初始化控件
    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        btnBack = findViewById(R.id.btn_back)
        btnCamera = findViewById(R.id.btn_camera)
        btnSearch = findViewById(R.id.btn_search)
        ivPreview = findViewById(R.id.iv_preview)
        tvResult = findViewById(R.id.tv_result)
        tvExplanation = findViewById(R.id.tv_explanation)
        loadingLayout = findViewById(R.id.loading_layout)
    }

    // 初始化事件监听（拍照、搜索、返回等）
    private fun initListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // 拍照按钮点击，先检查权限
        btnCamera.setOnClickListener {
            if (checkCameraPermission()) {
                takePhoto()
            } else {
                requestCameraPermission()
            }
        }

        // 搜索按钮点击，执行垃圾查询
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGarbage(query)
            } else {
                MyToast.sendToast("请输入要搜索的垃圾名称", this)
            }
        }

        // 输入框回车触发搜索
        etSearch.setOnEditorActionListener { _, _, _ ->
            btnSearch.performClick()
            true
        }
    }

    // 显示加载动画
    private fun showLoading() {
        runOnUiThread {
            loadingLayout.visibility = View.VISIBLE
        }
    }

    // 隐藏加载动画
    private fun hideLoading() {
        runOnUiThread {
            loadingLayout.visibility = View.GONE
        }
    }

    // 检查相机权限
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 动态申请相机权限
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    // 启动拍照Intent，保存图片到app私有目录
    private fun takePhoto() {
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath

        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }

        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    // 创建用于保存拍照图片的文件
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "GARBAGE_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    // 查询垃圾分类（本地知识库+API兜底）
    private fun searchGarbage(query: String) {
        showLoading()

        val results = GarbageKnowledgeBase.searchGarbage(query)
        if (results.isNotEmpty()) { // 本地知识库
            hideLoading()
            val result = results.first()
            showRecognitionResult(
                name = result.name,
                category = result.category,
                explanation = result.explanation,
                method = "search"
            )
        } else { // Deepseek Api兜底
            lifecycleScope.launch {
                try {
                    val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(query)
                    hideLoading()
                    showRecognitionResult(
                        name = query,
                        category = category,
                        explanation = explanation,
                        method = "search"
                    )
                } catch (e: Exception) {
                    hideLoading()
                    MyToast.sendToast("搜索失败: ${e.message}", this@GarbageRecognitionActivity)
                }
            }
        }
    }

    // 展示识别结果，保存记录（本地+后端预留），可加入挑战题库
    private fun showRecognitionResult(
        name: String,
        category: String, // 始终为非null
        explanation: String,
        method: String
    ) {
        tvResult.text = "$name - $category"
        tvExplanation.text = explanation

        // 保存识别记录
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

        // 显示结果对话框，增加"加入挑战题库"按钮（仅拍照识别时显示）
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

    // 获取分类对应的图标资源
    private fun getCategoryIcon(category: String): Int {
        return when (category) {
            "可回收物" -> R.drawable.ic_recyclable
            "有害垃圾" -> R.drawable.ic_hazardous
            "厨余垃圾" -> R.drawable.ic_kitchen
            "其他垃圾" -> R.drawable.ic_other
            else -> R.drawable.ic_other
        }
    }

    // 拍照/识别结果回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 智能拍照识别：调用百度API
            currentPhotoPath?.let { path ->
                showLoading()
                BaiduImageApi.recognizeImage(path) { keyword ->
                    runOnUiThread {
                        if (keyword != null) {
                            // 结合本地知识库/DeepSeek获取分类和说明
                            lifecycleScope.launch {
                                try {
                                    val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(keyword)
                                    hideLoading()
                                    showRecognitionResult(keyword, category, explanation, "camera")
                                } catch (e: Exception) {
                                    hideLoading()
                                    MyToast.sendToast("识别失败: ${e.message}", this@GarbageRecognitionActivity)
                                }
                            }
                        } else {
                            hideLoading()
                            MyToast.sendToast("识别失败，请重试", this)
                        }
                    }
                }
            }
        }
    }

    // 权限请求回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                MyToast.sendToast("需要相机权限才能拍照识别", this)
            }
        }
    }
}