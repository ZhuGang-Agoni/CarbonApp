package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Service.RetrofitClient
import com.zg.carbonapp.databinding.ActivityPostFeedBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PostFeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostFeedBinding
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private val PERMISSION_REQUEST_CODE = 3
    private var selectedImagePaths = mutableListOf<String>() // 存储图片路径
    private val maxImageCount = 3
    private var currentPhotoPath: String? = null

    // 当前用户信息
    private val currentUser by lazy { UserMMKV.getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 验证用户是否存在
        if (currentUser == null) {
            Toast.makeText(this, "用户信息获取失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置返回按钮
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }



        // 设置图片选择区域点击事件
        binding.chooseImage.setOnClickListener {
            if (selectedImagePaths.size < maxImageCount) {
                checkAndRequestPermissions()
            } else {
                Toast.makeText(this, "最多只能选择${maxImageCount}张图片", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置发布按钮
        binding.btnPublish.setOnClickListener { publishFeed() }

        // 初始化图片区域
        updateImageDisplay()
    }

    // 检查并请求权限（区分 Android 版本）
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // 相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // 存储/媒体权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            openImageSelector()
        }
    }

    // 权限回调处理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    openImageSelector()
                } else {
                    Toast.makeText(this, "需要相关权限才能选择图片", Toast.LENGTH_SHORT).show()
                }
            }
            10086 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限已获取，可正常加载图片", Toast.LENGTH_SHORT).show()
                    updateImageDisplay()
                } else {
                    Toast.makeText(this, "缺少存储权限，图片可能无法加载", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 打开图片选择器（相册或相机）
    private fun openImageSelector() {
        val options = arrayOf<CharSequence>("拍照", "从相册选择")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("添加图片")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "拍照" -> dispatchTakePictureIntent()
                options[item] == "从相册选择" -> openGallery()
            }
            dialog.dismiss()
        }
        builder.show()
    }

    // 打开相册（支持多选）
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 拍照
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    // 创建临时图片文件
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // 处理相册和相机返回结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> handleGalleryResult(data)
                REQUEST_IMAGE_CAPTURE -> handleCameraResult()
            }
        }
    }

    // 处理相册返回结果（解析为路径）
    private fun handleGalleryResult(data: Intent?) {
        if (data == null) return

        val newImages = mutableListOf<String>()

        if (data.clipData != null) {
            val clipData = data.clipData
            for (i in 0 until clipData!!.itemCount) {
                val uri = clipData.getItemAt(i).uri
                val filePath = getRealPathFromUri(uri)
                filePath?.let { newImages.add(it) }
            }
        } else if (data.data != null) {
            val uri = data.data
            val filePath = getRealPathFromUri(uri!!)
            filePath?.let { newImages.add(it) }
        }

        val canAddCount = maxImageCount - selectedImagePaths.size
        val imagesToAdd = newImages.take(canAddCount)
        selectedImagePaths.addAll(imagesToAdd)
        updateImageDisplay()

        if (imagesToAdd.isEmpty()) {
            Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show()
        } else if (imagesToAdd.size < newImages.size) {
            Toast.makeText(this, "已达到最大选择数量${maxImageCount}张", Toast.LENGTH_SHORT).show()
        }
    }

    // 处理相机返回结果
    private fun handleCameraResult() {
        currentPhotoPath?.let { path ->
            selectedImagePaths.add(path)
            updateImageDisplay()
        }
    }

    // 更新图片显示
    private fun updateImageDisplay() {
        binding.llSelectedImages.removeAllViews()

        if (selectedImagePaths.isEmpty()) {
            binding.chooseImage.visibility = View.VISIBLE
            binding.llSelectedImages.visibility = View.GONE
            return
        }

        binding.chooseImage.visibility = View.GONE
        binding.llSelectedImages.visibility = View.VISIBLE

        for (path in selectedImagePaths) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                0,
                200.dpToPx(),
                1f
            )
            layoutParams.marginEnd = 8.dpToPx()
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            imageView.setOnClickListener {
                selectedImagePaths.remove(path)
                updateImageDisplay()
            }

            try {
                val bitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("ImagePreview", "预览失败: $path", e)
                imageView.setImageResource(R.drawable.ic_image_error)
            }

            binding.llSelectedImages.addView(imageView)
        }

        if (selectedImagePaths.size < maxImageCount) {
            val addImageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                0,
                200.dpToPx(),
                1f
            )
            addImageView.layoutParams = layoutParams
            addImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            addImageView.setBackgroundResource(R.drawable.border_rounded)
            addImageView.setImageResource(R.drawable.add_iamges)
            addImageView.setColorFilter(ContextCompat.getColor(this, R.color.gray))

            addImageView.setOnClickListener {
                checkAndRequestPermissions()
            }

            binding.llSelectedImages.addView(addImageView)
        }
    }

    // 发布动态
    private fun publishFeed() {
        val content = binding.etContent.text.toString().trim()

        if (content.isEmpty() && selectedImagePaths.isEmpty()) {
            Toast.makeText(this, "内容或图片不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "发布中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")

                // 准备文件
                val parts = selectedImagePaths.mapNotNull { path ->
                    try {
                        val file = File(path)
                        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                        MultipartBody.Part.createFormData("files", file.name, requestFile)
                    } catch (e: Exception) {
                        Log.e("PostFeedActivity", "文件处理失败: $path", e)
                        null
                    }
                }

                val response = RetrofitClient.instance.publishDynamic(
                    "Bearer $token",
                    content,
                    parts
                )

                withContext(Dispatchers.Main) {
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.text = "发布"

                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 1) {
                            Toast.makeText(this@PostFeedActivity, "发布成功", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val errorMsg = apiResponse?.message ?: "发布失败"
                            Toast.makeText(this@PostFeedActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            this@PostFeedActivity,
                            "网络请求失败: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.text = "发布"

                    Toast.makeText(
                        this@PostFeedActivity,
                        "发布失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("PostFeedActivity", "发布动态失败", e)
                }
            }
        }
    }


    private fun generateRandomUserId(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(9000) + 1000  // 4位随机数
        return "userFeed_${timestamp}_$random"
    }
    // 获取当前时间字符串
    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(Date())
    }

    // URI 转真实文件路径（兼容 MIUI）
    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        if (uri.authority?.contains("com.miui.gallery") == true) {
            return uri.path?.replace("%2F", "/")
        }
        return null
    }

    // dp 转 px 工具方法
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
