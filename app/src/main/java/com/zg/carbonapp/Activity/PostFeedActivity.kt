package com.zg.carbonapp.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityPostFeedBinding
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
    private val MIN_CONTENT_LENGTH = 10 // 内容最小长度（避免无意义内容）
    private val MAX_CONTENT_LENGTH = 500 // 内容最大长度（防止过长）

    // 当前用户信息
    private val currentUser by lazy { UserMMKV.getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化进度条（新增）


        // 验证用户是否存在
        if (currentUser == null) {
            Toast.makeText(this, "用户信息获取失败，请重新登录", Toast.LENGTH_SHORT).show()
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

    // 检查并请求权限（清理无用的10086请求码逻辑）
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

    // 权限回调处理（清理无用的10086逻辑）
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
                    Toast.makeText(this, "需要相机/存储权限才能发布图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 打开图片选择器（相册或相机）- 无修改
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

    // 打开相册（支持多选）- 优化：图片路径去重
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 拍照 - 新增：拍照后加入媒体库（相册可见）
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
                    // 拍照后加入媒体库（新增）
                    galleryAddPic(it)
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

    // 创建临时图片文件 - 无修改
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

    // 新增：拍照图片加入媒体库（让相册可见）
    private fun galleryAddPic(photoFile: File) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val contentUri = Uri.fromFile(photoFile)
            mediaScanIntent.data = contentUri
            this.sendBroadcast(mediaScanIntent)
        }
    }

    // 处理相册和相机返回结果 - 优化：图片路径去重+有效性校验
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> handleGalleryResult(data)
                REQUEST_IMAGE_CAPTURE -> handleCameraResult()
            }
        }
    }

    // 处理相册返回结果 - 优化：去重+无效路径过滤
    private fun handleGalleryResult(data: Intent?) {
        if (data == null) return
        val newImages = mutableListOf<String>()

        // 解析相册返回的Uri
        if (data.clipData != null) {
            val clipData = data.clipData
            for (i in 0 until clipData!!.itemCount) {
                val uri = clipData.getItemAt(i).uri
                val filePath = getRealPathFromUri(uri)
                filePath?.takeIf { File(it).exists() }?.let { newImages.add(it) }
            }
        } else if (data.data != null) {
            val uri = data.data
            val filePath = getRealPathFromUri(uri!!)
            filePath?.takeIf { File(it).exists() }?.let { newImages.add(it) }
        }

        // 去重（避免重复选择同一张图）
        val uniqueImages = newImages.distinct()
        // 计算可添加数量
        val canAddCount = maxImageCount - selectedImagePaths.size
        val imagesToAdd = uniqueImages.take(canAddCount)
        // 更新选中列表（去重）
        selectedImagePaths = (selectedImagePaths + imagesToAdd).distinct().toMutableList()

        updateImageDisplay()

        // 提示用户结果
        when {
            imagesToAdd.isEmpty() -> Toast.makeText(this, "未选择有效图片", Toast.LENGTH_SHORT).show()
            imagesToAdd.size < uniqueImages.size ->
                Toast.makeText(this, "已达到最大选择数量${maxImageCount}张", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "成功选择${imagesToAdd.size}张图片", Toast.LENGTH_SHORT).show()
        }
    }

    // 处理相机返回结果 - 优化：无效路径过滤
    private fun handleCameraResult() {
        currentPhotoPath?.takeIf { File(it).exists() }?.let { path ->
            // 去重
            if (!selectedImagePaths.contains(path)) {
                selectedImagePaths.add(path)
                updateImageDisplay()
                Toast.makeText(this, "拍照成功", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "拍照图片获取失败", Toast.LENGTH_SHORT).show()
    }

    // 更新图片显示 - 优化：图片压缩（避免OOM）
    private fun updateImageDisplay() {
        binding.llSelectedImages.removeAllViews()

        // 无图片时显示选择按钮
        if (selectedImagePaths.isEmpty()) {
            binding.chooseImage.visibility = View.VISIBLE
            binding.llSelectedImages.visibility = View.GONE
            return
        }

        // 有图片时显示预览
        binding.chooseImage.visibility = View.GONE
        binding.llSelectedImages.visibility = View.VISIBLE

        selectedImagePaths.forEach { path ->
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                0,
                200.dpToPx(),
                1f
            ).apply {
                marginEnd = 8.dpToPx()
            }
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setBackgroundResource(R.drawable.border_rounded) // 加边框，美观

            // 点击删除图片
            imageView.setOnClickListener {
                selectedImagePaths.remove(path)
                updateImageDisplay()
            }

            // 优化：图片压缩（避免大图片导致OOM）
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true // 先获取图片尺寸
                    BitmapFactory.decodeFile(path, this)
                    // 计算采样率（宽度不超过200dp对应的像素）
                    val targetWidth = 200.dpToPx()
                    inSampleSize = calculateInSampleSize(this, targetWidth, targetWidth)
                    inJustDecodeBounds = false // 实际解码
                    inPreferredConfig = Bitmap.Config.RGB_565 // 减少内存占用
                }
                val bitmap = BitmapFactory.decodeFile(path, options)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("ImagePreview", "预览失败: $path", e)
                imageView.setImageResource(R.drawable.ic_image_error)
            }

            binding.llSelectedImages.addView(imageView)
        }

        // 未达最大数量时显示"添加"按钮
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

    // 新增：计算图片采样率（压缩用）
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // 找到最接近reqWidth/reqHeight的采样率
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 核心：发布动态（重点优化：字段补全、内容校验、加载状态）
    private fun publishFeed() {
        val content = binding.etContent.text.toString().trim()

        // 1. 内容校验
        when {
            content.isEmpty() && selectedImagePaths.isEmpty() -> {
                Toast.makeText(this, "内容或图片不能为空", Toast.LENGTH_SHORT).show()
                return
            }
            content.isNotEmpty() && content.length < MIN_CONTENT_LENGTH -> {
                Toast.makeText(this, "内容至少${MIN_CONTENT_LENGTH}个字符", Toast.LENGTH_SHORT).show()
                return
            }
            content.length > MAX_CONTENT_LENGTH -> {
                Toast.makeText(this, "内容最多${MAX_CONTENT_LENGTH}个字符", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 2. 显示加载状态（按钮禁用+进度条显示）
        binding.btnPublish.isEnabled = false
        binding.btnPublish.visibility = View.GONE

        // 3. 异步发布（避免阻塞主线程）
        Thread {
            try {
                // 3.1 获取现有动态
                val existingFeeds = MMKVManager.getAllFeeds().toMutableList()

                // 3.2 构建完整的UserFeed（补全所有字段）
                val newFeed = currentUser?.let { user ->
                    UserFeed(
                        feedId = generateRandomFeedId(),
                        userId = user.userId ?: "unknown_user",
                        username = user.userName ?: "未知用户",
                        avatar = user.userAvatar ?: "",
                        content = content,
                        images = selectedImagePaths.toList(), // 去重后的图片路径
                        likeCount = 0, // 初始点赞数0
                        commentCount = 0, // 初始评论数0
                        shareCount = 0, // 补全：初始分享数0（原逻辑漏了）
                        createTime = getCurrentTime(),
                        isLiked = false, // 初始未点赞
                        isSaved = false, // 初始未收藏
                        isCommented = false // 初始未评论
                    )
                } ?: throw Exception("用户信息缺失")

                // 3.3 添加到动态列表顶部（最新动态在前）
                existingFeeds.add(0, newFeed)

                // 3.4 保存到MMKV
                MMKVManager.saveAllFeeds(existingFeeds)

                // 3.5 主线程更新UI（发布成功）
                runOnUiThread {
                    Toast.makeText(this, "发布成功！", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                Log.e("PostFeedActivity", "发布失败", e)
                // 主线程更新UI（发布失败）
                runOnUiThread {
                    Toast.makeText(this, "发布失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                    // 恢复按钮状态
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.visibility = View.VISIBLE

                }

            }
        }.start()
    }

    // 生成随机动态ID - 无修改
    private fun generateRandomFeedId(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(9000) + 1000  // 4位随机数
        return "feed_${timestamp}_$random"
    }

    // 获取当前时间字符串 - 无修改
    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(Date())
    }

    // URI 转真实文件路径（兼容 MIUI）- 无修改
    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        // 兼容MIUI相册
        if (uri.authority?.contains("com.miui.gallery") == true) {
            return uri.path?.replace("%2F", "/")
        }
        return null
    }

    // dp 转 px 工具方法 - 无修改
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}