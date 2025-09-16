package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
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
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.GarbageKnowledgeBase
import com.zg.carbonapp.Tool.MyToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import com.zg.carbonapp.Tool.BaiduImageApi
import com.zg.carbonapp.MMKV.UserChallengePhotoMMKV
import com.zg.carbonapp.Dao.UserChallengePhoto

class GarbageRecognitionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnSearch: ImageButton
    private lateinit var ivPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var loadingOverlay: RelativeLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var btnHistory : ImageView

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
        btnBack = findViewById(R.id.backButton)
        btnCamera = findViewById(R.id.btn_camera)
        btnSearch = findViewById(R.id.btn_search)
        ivPreview = findViewById(R.id.iv_preview)
        tvResult = findViewById(R.id.tv_result)
        tvExplanation = findViewById(R.id.tv_explanation)
        loadingOverlay = findViewById(R.id.loading_overlay)
        progressBar = findViewById(R.id.progress_bar)
        btnHistory = findViewById(R.id.btn_history)


    }

    // 初始化事件监听（拍照、搜索、返回等）
    private fun initListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // 拍照按钮点击，先检查权限
        btnCamera.setOnClickListener {
            if (checkCameraPermission()) {
                showLoading()
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

        btnHistory.setOnClickListener {
            val intent = Intent(this, RecognitionHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    // 显示加载动画
    private fun showLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.VISIBLE
            btnCamera.isEnabled = false
        }
    }

    // 隐藏加载动画
    private fun hideLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            btnCamera.isEnabled = true
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

    // 创建用于保存拍照图片的文件
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "GARBAGE_${timeStamp}_"
        val storageDir = getExternalFilesDir(null) ?: throw Exception("存储目录不可用")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    // 图片压缩方法（精确匹配控件尺寸）
    private fun getScaledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 查询垃圾分类（本地知识库+API兜底）
    private fun searchGarbage(query: String) {
        showLoading()
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
        } else {
            lifecycleScope.launch {
                try {
                    val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(query)
                    showRecognitionResult(query, category, explanation, "search")
                } catch (e: Exception) {
                    MyToast.sendToast("查询失败，请检查网络", this@GarbageRecognitionActivity)
                } finally {
                    hideLoading()
                }
            }
        }
    }

    // 展示识别结果，保存记录（本地+后端预留），可加入挑战题库
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

    // 拍照回调：核心优化图片贴合逻辑
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                currentPhotoPath?.let { path ->
                    // 监听ImageView布局完成事件，确保获取准确尺寸
                    ivPreview.viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            // 移除监听器，避免重复调用
                            ivPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            runOnUiThread {
                                try {
                                    // 获取ImageView实际宽高
                                    val targetWidth = ivPreview.width
                                    val targetHeight = ivPreview.height

                                    if (targetWidth > 0 && targetHeight > 0) {
                                        // 1. 获取图片原始尺寸
                                        val options = BitmapFactory.Options().apply {
                                            inJustDecodeBounds = true
                                        }
                                        BitmapFactory.decodeFile(path, options)
                                        val imageWidth = options.outWidth
                                        val imageHeight = options.outHeight

                                        // 2. 计算图片与ImageView的比例，动态调整ImageView尺寸
                                        val imageRatio =
                                            imageWidth.toFloat() / imageHeight.toFloat()
                                        val viewRatio =
                                            targetWidth.toFloat() / targetHeight.toFloat()

                                        val params = ivPreview.layoutParams
                                        if (imageRatio > viewRatio) {
                                            // 图片更宽，按宽度适配，调整高度
                                            params.height = (targetWidth / imageRatio).toInt()
                                        } else {
                                            // 图片更高，按高度适配，调整宽度
                                            params.width = (targetHeight * imageRatio).toInt()
                                        }
                                        ivPreview.layoutParams = params

                                        // 3. 按调整后的尺寸压缩图片
                                        val bitmap =
                                            getScaledBitmap(path, params.width, params.height)
                                        bitmap?.let {
                                            // 4. 使用合适的缩放模式：FIT_XY填充整个控件（无空白）
                                            ivPreview.scaleType = ImageView.ScaleType.FIT_XY
                                            ivPreview.setImageBitmap(it)
                                        } ?: run {
                                            MyToast.sendToast(
                                                "无法加载照片",
                                                this@GarbageRecognitionActivity
                                            )
                                            ivPreview.setImageResource(R.drawable.ic_ai_recognition_color)
                                        }
                                    } else {
                                        // 备选方案：使用默认尺寸
                                        val defaultSize = 300
                                        val bitmap = getScaledBitmap(path, defaultSize, defaultSize)
                                        bitmap?.let {
                                            ivPreview.scaleType = ImageView.ScaleType.FIT_XY
                                            ivPreview.setImageBitmap(it)
                                        } ?: run {
                                            MyToast.sendToast(
                                                "无法加载照片",
                                                this@GarbageRecognitionActivity
                                            )
                                            ivPreview.setImageResource(R.drawable.ic_ai_recognition_color)
                                        }
                                    }
                                } catch (e: Exception) {
                                    MyToast.sendToast(
                                        "预览图处理失败",
                                        this@GarbageRecognitionActivity
                                    )
                                    ivPreview.setImageResource(R.drawable.ic_ai_recognition_color)
                                }
                            }

                            // 执行识别逻辑
                            MyToast.sendToast("识别中...", this@GarbageRecognitionActivity)
                            BaiduImageApi.recognizeImage(path) { keyword ->
                                if (keyword != null) {
                                    lifecycleScope.launch {
                                        try {
                                            val (category, explanation) = GarbageKnowledgeBase.getKnowledgeWithDeepSeek(
                                                keyword
                                            )
                                            showRecognitionResult(
                                                keyword,
                                                category,
                                                explanation,
                                                "camera"
                                            )
                                        } catch (e: Exception) {
                                            MyToast.sendToast(
                                                "识别结果解析失败",
                                                this@GarbageRecognitionActivity
                                            )
                                        } finally {
                                            hideLoading()
                                        }
                                    }
                                } else {
                                    runOnUiThread {
                                        MyToast.sendToast(
                                            "识别失败，请重试",
                                            this@GarbageRecognitionActivity
                                        )
                                        hideLoading()
                                    }
                                }
                            }
                        }
                    })
                } ?: run {
                    MyToast.sendToast("照片路径错误", this)
                    hideLoading()
                }
            } else {
                runOnUiThread {
                    ivPreview.setImageResource(R.drawable.ic_ai_recognition_color)
                }
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

    override fun onDestroy() {
        super.onDestroy()
        val drawable = ivPreview.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        ivPreview.setImageDrawable(null)
    }
}