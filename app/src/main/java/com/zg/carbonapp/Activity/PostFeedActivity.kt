
package com.zg.carbonapp.Activity

import android.Manifest
import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager

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
    private var selectedImageUris = mutableListOf<Uri>()
    private val maxImageCount = 3
    private var currentPhotoPath: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置返回按钮
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置图片选择区域点击事件
        binding.chooseImage.setOnClickListener {
            if (selectedImageUris.size < maxImageCount) {
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

    // 检查并请求权限
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            openImageSelector()
        }
    }
    private fun getImage():String{
        var resId: Int = com.zg.carbonapp.R.drawable.img

        var packageName: String = this.getPackageName()
        var uri: Uri = Uri.parse("android.resource://$packageName/$resId")
        var uriString: String = uri.toString() // 转为String
        return uriString
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageSelector()
            } else {
                Toast.makeText(this, "需要相机和存储权限才能添加图片", Toast.LENGTH_SHORT).show()
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

    // 打开相册
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

    // 处理相册返回结果
    private fun handleGalleryResult(data: Intent?) {
        if (data == null) return

        val newImages = mutableListOf<Uri>()

        if (data.clipData != null) {
            // 多选图片
            val clipData = data.clipData
            for (i in 0 until clipData!!.itemCount) {
                val imageUri = clipData.getItemAt(i).uri
                newImages.add(imageUri)
            }
        } else if (
    data.data != null) {
        // 单选图片
        val imageUri = data.data
        newImages.add(imageUri!!)
    }

    // 限制不超过最大数量
    val canAddCount = maxImageCount - selectedImageUris.size
    val imagesToAdd = newImages.take(canAddCount)
    selectedImageUris.addAll(imagesToAdd)

    // 更新图片显示
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
        val file = File(path)
        val uri = Uri.fromFile(file)
        selectedImageUris.add(uri)
        updateImageDisplay()
    }
}

// 更新图片显示
private fun updateImageDisplay() {
    binding.llSelectedImages.removeAllViews()

    if (selectedImageUris.isEmpty()) {
        binding.chooseImage.visibility = View.VISIBLE
        binding.llSelectedImages.visibility = View.GONE
        return
    }

    binding.chooseImage.visibility = View.GONE
    binding.llSelectedImages.visibility = View.VISIBLE

    // 添加已选图片
    for (uri in selectedImageUris) {
        val imageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(
            0,
            200.dpToPx(),
            1f // 等比例分配宽度
        )
        layoutParams.marginEnd = 8.dpToPx()
//        imageView.layoutToPx()
        imageView.layoutParams = layoutParams
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP


        // 设置点击事件（删除图片）
        imageView.setOnClickListener {
            selectedImageUris.remove(uri)
            updateImageDisplay()
        }

        Glide.with(this)
            .load(uri)
//            .placeholder(R.drawable.ic_image_placeholder)
//            .error(R.drawable.ic_image_error)
            .into(imageView)

        binding.llSelectedImages.addView(imageView)
    }

    // 如果还有剩余名额，添加添加图片按钮
    if (selectedImageUris.size < maxImageCount) {
        val addImageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(
            0,
            200.dpToPx(),
            1f // 等比例分配宽度
        )
        addImageView.layoutParams = layoutParams
        addImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
//        addImageView.setBackgroundResource(R.drawable.border_rounded)
        addImageView.setImageResource(com.zg.carbonapp.R.drawable.add_iamges)
        addImageView.setColorFilter(ContextCompat.getColor(this, com.zg.carbonapp.R.color.gray))

        addImageView.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.llSelectedImages.addView(addImageView)
    }
}

// 发布动态
private fun publishFeed() {
    val content = binding.etContent.text.toString().trim()

    if (content.isEmpty() && selectedImageUris.isEmpty()) {
        Toast.makeText(this, "内容或图片不能为空", Toast.LENGTH_SHORT).show()
        return
    }

    // 创建新动态 这边我暂时写死 以后还要改的
    val newFeed = UserFeed(
        userId = UUID.randomUUID().toString(),
        username = "Agoni",
        avatar =getImage(),
        content = content,
        images = selectedImageUris.map { it.toString() },
        likeCount = 0,
        commentCount = 0,
        shareCount = 0,
        createTime = getCurrentTime(),
        isLiked = false
    )

    // 保存到MMKV
    val feeds = MMKVManager.getFeeds().toMutableList()
    feeds.add(0, newFeed)
    MMKVManager.saveFeeds(feeds)

    // 返回结果给前一个Activity
    setResult(RESULT_OK)
    finish()
}

// 获取当前时间字符串
private fun getCurrentTime(): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    return format.format(Date())
}

// dp转px工具方法
private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
//package com.zg.carbonapp.Activity
//
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.view.View
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import com.bumptech.glide.Glide
//import com.github.mikephil.charting.BuildConfig
//
//import com.zg.carbonapp.Dao.UserFeed
//import com.zg.carbonapp.MMKV.MMKVManager
//import com.zg.carbonapp.R
//import com.zg.carbonapp.databinding.ActivityPostFeedBinding
//import java.io.File
//import java.io.IOException
//import java.text.SimpleDateFormat
//import java.util.*
//
//class PostFeedActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityPostFeedBinding
//    private val PICK_IMAGE_REQUEST = 1
//    private val REQUEST_IMAGE_CAPTURE = 2
//    private var selectedImageUris = mutableListOf<Uri>()
//    private val maxImageCount = 3
//    private var currentPhotoPath: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityPostFeedBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // 设置返回按钮
//        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
//
//        // 设置图片选择区域点击事件
//        binding.chooseImage.setOnClickListener {
//            if (selectedImageUris.size < maxImageCount) {
//                openImageSelector()
//            } else {
//                Toast.makeText(this, "最多只能选择${maxImageCount}张图片", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        // 设置发布按钮
//        binding.btnPublish.setOnClickListener { publishFeed() }
//
//        // 初始化图片区域
//        updateImageDisplay()
//    }
//
//    // 打开图片选择器（相册或相机）
//    private fun openImageSelector() {
//        val options = arrayOf<CharSequence>("拍照", "从相册选择", "取消")
//
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("添加图片")
//        builder.setItems(options) { dialog, item ->
//            when {
//                options[item] == "拍照" -> dispatchTakePictureIntent()
//                options[item] == "从相册选择" -> openGallery()
//                options[item] == "取消" -> dialog.dismiss()
//            }
//        }
//        builder.show()
//    }
//
//    // 打开相册
//    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
//    }
//
//    // 拍照
//    private fun dispatchTakePictureIntent() {
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            takePictureIntent.resolveActivity(packageManager)?.also {
//                val photoFile: File? = try {
//                    createImageFile()
//                } catch (ex: IOException) {
//                    Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show()
//                    null
//                }
//                photoFile?.also {
//                    val photoURI: Uri = FileProvider.getUriForFile(
//                        this,
//                        "${BuildConfig.APPLICATION_ID}.fileprovider",
//                        it
//                    )
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//                }
//            }
//        }
//    }
//
//    // 创建临时图片文件
//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile(
//            "JPEG_${timeStamp}_",
//            ".jpg",
//            storageDir
//        ).apply {
//            currentPhotoPath = absolutePath
//        }
//    }
//
//    // 处理相册和相机返回结果
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (resultCode == RESULT_OK) {
//            when (requestCode) {
//                PICK_IMAGE_REQUEST -> handleGalleryResult(data)
//                REQUEST_IMAGE_CAPTURE -> handleCameraResult()
//            }
//        }
//    }
//
//    // 处理相册返回结果
//    private fun handleGalleryResult(data: Intent?) {
//        if (data == null) return
//
//        val newImages = mutableListOf<Uri>()
//
//        if (data.clipData != null) {
//            // 多选图片
//            val clipData = data.clipData
//            for (i in 0 until clipData!!.itemCount) {
//                val imageUri = clipData.getItemAt(i).uri
//                newImages.add(imageUri)
//            }
//        } else if (data.data != null) {
//            // 单选图片
//            val imageUri = data.data
//            newImages.add(imageUri!!)
//        }
//
//        // 限制不超过最大数量
//        val canAddCount = maxImageCount - selectedImageUris.size
//        val imagesToAdd = newImages.take(canAddCount)
//        selectedImageUris.addAll(imagesToAdd)
//
//        // 更新图片显示
//        updateImageDisplay()
//
//        if (imagesToAdd.isEmpty()) {
//            Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show()
//        } else if (imagesToAdd.size < newImages.size) {
//            Toast.makeText(this, "已达到最大选择数量${maxImageCount}张", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // 处理相机返回结果
//    private fun handleCameraResult() {
//        currentPhotoPath?.let { path ->
//            val file = File(path)
//            val uri = Uri.fromFile(file)
//            selectedImageUris.add(uri)
//            updateImageDisplay()
//        }
//    }
//
//    // 更新图片显示
//    private fun updateImageDisplay() {
//        binding.llSelectedImages.removeAllViews()
//
//        if (selectedImageUris.isEmpty()) {
//            binding.chooseImage.visibility = View.VISIBLE
//            binding.llSelectedImages.visibility = View.GONE
//            return
//        }
//
//        binding.chooseImage.visibility = View.GONE
//        binding.llSelectedImages.visibility = View.VISIBLE
//
//        // 添加已选图片
//        for (uri in selectedImageUris) {
//            val imageView = ImageView(this)
//            val layoutParams = LinearLayout.LayoutParams(
//                0,
//                200.dpToPx(),
//                1f // 等比例分配宽度
//            )
//            layoutParams.marginEnd = 8.dpToPx()
//            imageView.layoutParams = layoutParams
//            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
//            imageView.setBackgroundResource(R.drawable.img_2)
//
//            // 设置点击事件（删除图片）
//            imageView.setOnClickListener {
//                selectedImageUris.remove(uri)
//                updateImageDisplay()
//            }
//
//            Glide.with(this)
//                .load(uri)
//                .placeholder(R.drawable.img_2)
//                .error(R.drawable.img_2)
//                .into(imageView)
//
//            binding.llSelectedImages.addView(imageView)
//        }
//
//        // 如果还有剩余名额，添加添加图片按钮
//        if (selectedImageUris.size < maxImageCount) {
//            val addImageView = ImageView(this)
//            val layoutParams = LinearLayout.LayoutParams(
//                0,
//                200.dpToPx(),
//                1f // 等比例分配宽度
//            )
//            addImageView.layoutParams = layoutParams
//            addImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
//            addImageView.setBackgroundResource(R.drawable.img_2)
//            addImageView.setImageResource(R.drawable.img_2)
//            addImageView.setColorFilter(ContextCompat.getColor(this, R.color.gray))
//
//            addImageView.setOnClickListener {
//                openImageSelector()
//            }
//
//            binding.llSelectedImages.addView(addImageView)
//        }
//    }
//
//    // 发布动态
//    private fun publishFeed() {
//        val content = binding.etContent.text.toString().trim()
//          if (content.isEmpty() && selectedImageUris.isEmpty()) {
//            Toast.makeText(this, "内容或图片不能为空", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 创建新动态
//        val newFeed = UserFeed(
//            userId = UUID.randomUUID().toString(),
//            username = "当前用户", // 从用户信息中获取
//            avatar = "https://picsum.photos/seed/currentuser/100/100", // 从用户信息中获取
//            content = content,
//            images = selectedImageUris.map { it.toString() },
//            likeCount = 0,
//            commentCount = 0,
//            shareCount = 0,
//            createTime = getCurrentTime(),
//            isLiked = false
//        )
//
//        // 保存到MMKV
//        val feeds = MMKVManager.getFeeds().toMutableList()
//        feeds.add(0, newFeed) // 添加到列表开头
//        MMKVManager.saveFeeds(feeds)
//
//        // 返回结果给前一个Activity
//        setResult(RESULT_OK)
//        finish()
//    }
//
//    // 获取当前时间字符串
//    private fun getCurrentTime(): String {
//        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
//        return format.format(Date())
//    }
//
//    // dp转px工具方法
//    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
//}