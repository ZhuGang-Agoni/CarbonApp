package com.zg.carbonapp.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.zg.carbonapp.Activity.LoginActivity
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.MMKV.TokenManager  // 保留引用，未删除
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentImBinding
import java.io.File
import java.util.Random

class ImFragment : Fragment() {
    private lateinit var binding: FragmentImBinding
    private var currentUser: User? = null
    private var tempUser: User? = null

    // 请求码
    private val REQUEST_CODE_AVATAR = 1001
    private val REQUEST_CODE_CAMERA = 1002
    private var photoFile:File?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
        loadUserInfo()
    }

    private fun initListener() {
        // 返回按钮
        binding.returnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // 退出登录按钮（注释Token相关逻辑，默认隐藏）
        binding.logoutText.setOnClickListener {
            // 注释：原退出登录逻辑，暂时禁用
            // showLogoutDialog()
        }

        // 通知设置
        binding.notifyLayout.setOnClickListener {
            MyToast.sendToast("通知设置功能开发中", requireContext())
        }

        // 账号安全
        binding.securityLayout.setOnClickListener {
            MyToast.sendToast("账号安全功能开发中", requireContext())
        }

        // 隐私信息展开/收起
        binding.privacyArrow.setOnClickListener {
            togglePrivacyContent()
        }

        // 保存按钮
        binding.userProfileSubmit.setOnClickListener {
            saveUserChanges()
        }

        // 头像点击事件（注释Token判断，直接启用）
        binding.ivAvatar.setOnClickListener {
            // 注释：原Token判断逻辑，暂时禁用
            /*val token = TokenManager.getToken()
            if (!token.isNullOrEmpty()) {*/
            showAvatarOptions()
            /*} else {
                MyToast.sendToast("请先登录", requireContext())
            }*/
        }

        // 密码修改（注释Token判断，直接启用）
        binding.userPasswordName.setOnClickListener {
            // 注释：原Token判断逻辑，暂时禁用
            /*val token = TokenManager.getToken()
            if (!token.isNullOrEmpty()) {*/
            showChangePasswordDialog()
            /*} else {
                MyToast.sendToast("请先登录", requireContext())
            }*/
        }

        // 用户名修改（注释Token判断，直接启用）
        binding.userNameEdit.setOnClickListener {
            // 注释：原Token判断逻辑，暂时禁用
            /*val token = TokenManager.getToken()
            if (!token.isNullOrEmpty()) {*/
            showEditDialog("用户名", binding.userNameEdit.text.toString()) { newValue ->
                binding.userNameEdit.text = newValue
                tempUser?.userName = newValue
            }
            /*} else {
                MyToast.sendToast("请先登录", requireContext())
            }*/
        }

        // 签名修改（注释Token判断，直接启用）
        binding.userSignatureEdit.setOnClickListener {
            // 注释：原Token判断逻辑，暂时禁用
            /*val token = TokenManager.getToken()
            if (!token.isNullOrEmpty()) {*/
            showEditDialog("个性签名", binding.userSignatureEdit.text.toString()) { newValue ->
                binding.userSignatureEdit.text = newValue
                tempUser?.signature = newValue
            }
            /*} else {
                MyToast.sendToast("请先登录", requireContext())
            }*/
        }

        // 登录提示点击事件（注释，强制隐藏）
        binding.loginPrompt.setOnClickListener {
            // 注释：原登录跳转逻辑，暂时禁用
            // IntentHelper.goIntent(requireContext(), LoginActivity::class.java)
        }
        // 强制隐藏登录提示
        binding.loginPrompt.visibility = View.GONE
    }

    private fun loadUserInfo() {
        // 从MMKV加载用户信息
        currentUser = UserMMKV.getUser()

        // 注释：原Token获取及登录状态判断逻辑，暂时禁用
        /*val token = TokenManager.getToken()
        val isLoggedIn = !token.isNullOrEmpty()*/
        // 强制设为"已登录"状态，所有功能可用
        val isLoggedIn = true

        // 如果本地没有用户数据，创建新用户并生成随机ID
        if (currentUser == null) {
            currentUser = User(
                userId = generateRandomUserId(),  // 生成随机唯一ID
                userName = "用户${System.currentTimeMillis() % 10000}",  // 随机用户名
                signature = "点击编辑个性签名",
                userQQ = "",  // 初始为空，让用户自行填写
                userTelephone = "",  // 初始为空，让用户自行填写
                userAvatar = "",
                carbonCount = 0,
                userPassword = "123456"
            )
            UserMMKV.saveUser(currentUser!!)  // 保存到MMKV
        }

        tempUser = currentUser?.copy()

        // 根据登录状态更新UI（此时isLoggedIn恒为true）
        updateUIByLoginState(isLoggedIn)
    }

    /**
     * 生成随机唯一用户ID
     * 格式: USER_时间戳_随机数 (确保本地唯一性)
     */
    private fun generateRandomUserId(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(9000) + 1000  // 4位随机数
        return "USER_${timestamp}_$random"
    }

    /**
     * 根据登录状态更新UI显示
     * @param isLoggedIn 是否登录（当前恒为true）
     */
    private fun updateUIByLoginState(isLoggedIn: Boolean) {
        currentUser?.let { user ->
            // 更新用户信息显示
            binding.userNameEdit.text = user.userName
            binding.userCount.text = user.userId
            binding.userQqName.text = user.userQQ.ifEmpty { "未设置" }
            binding.userPhoneName.text = user.userTelephone.ifEmpty { "未设置" }
            binding.userCarbonCount.text = user.carbonCount.toString()
            binding.userSignatureEdit.text = user.signature

            // 加载头像
            if (user.userAvatar.isNotEmpty()) {
                Glide.with(this)
                    .load(user.userAvatar)
                    .into(binding.ivAvatar)
            } else {
                // 显示默认头像
                binding.ivAvatar.setImageResource(R.drawable.default_avatar)
            }
        }

        // 控制功能可见性（因isLoggedIn恒为true，所有控件启用）
        val userInfoViews = listOf(
            binding.userNameEdit, binding.userPasswordName,
            binding.userSignatureEdit, binding.userProfileSubmit,
            binding.ivAvatar
        )
        userInfoViews.forEach {
            it.isEnabled = isLoggedIn
            it.alpha = if (isLoggedIn) 1.0f else 0.5f
        }

        // 显示/隐藏退出登录按钮（注释控制逻辑，强制隐藏）
        // binding.logoutText.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.logoutText.visibility = View.GONE  // 强制隐藏

        // 显示/隐藏登录提示（强制隐藏）
        // binding.loginPrompt.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.loginPrompt.visibility = View.GONE
    }

    // 注释：原退出登录对话框，暂时禁用
    /*private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？退出后可重新登录查看个人信息")
            .setPositiveButton("确定") { _, _ -> logout() }
            .setNegativeButton("取消", null)
            .show()
    }*/

    // 注释：原退出登录逻辑，暂时禁用
    /*private fun logout() {
        TokenManager.clearToken()  // 清除登录凭证
        IntentHelper.goIntent(requireContext(), LoginActivity::class.java)
        requireActivity().finish()
    }*/

    private fun togglePrivacyContent() {
        val isExpanded = binding.privacyContent.visibility == View.VISIBLE
        binding.privacyContent.visibility = if (isExpanded) View.GONE else View.VISIBLE
        binding.privacyArrow.rotation = if (isExpanded) 0f else 180f
    }

    private fun showAvatarOptions() {
        val options = arrayOf("拍照", "从相册选择", "取消")
        AlertDialog.Builder(requireContext())
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> selectFromGallery()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun takePhoto() {
        photoFile = createTempPhotoFile()
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile!!
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 临时授权
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_CAMERA)
        } else {
            MyToast.sendToast("无法打开相机", requireContext())
        }
    }

    // 新增：创建临时照片文件
    private fun createTempPhotoFile(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "CAMERA_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_AVATAR -> data?.data?.let { updateAvatar(it) }
                REQUEST_CODE_CAMERA -> {
                    photoFile?.let { file ->
                        val photoUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                        updateAvatar(photoUri) // 调用更新头像逻辑
                    }
                }
            }
        }
    }



    private fun showEditDialog(title: String, currentValue: String, onConfirm: (String) -> Unit) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_input, null)
        val etInput = view.findViewById<EditText>(R.id.et_signature)
        etInput.setText(currentValue)
        etInput.setSelection(currentValue.length)

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("修改$title")
            .setPositiveButton("确定") { _, _ ->
                val newValue = etInput.text.toString().trim()
                if (newValue.isEmpty()) {
                    MyToast.sendToast("输入不能为空", requireContext())
                } else {
                    onConfirm(newValue)
                    MyToast.sendToast("修改成功", requireContext())
                }
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOldPassword = view.findViewById<EditText>(R.id.et_old_password)
        val etNewPassword = view.findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = view.findViewById<EditText>(R.id.et_confirm_password)

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("修改密码")
            .setPositiveButton("确定") { dialog, _ ->
                val oldPassword = etOldPassword.text.toString().trim()
                val newPassword = etNewPassword.text.toString().trim()
                val confirmPassword = etConfirmPassword.text.toString().trim()

                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    MyToast.sendToast("密码不能为空", requireContext())
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    MyToast.sendToast("两次输入的密码不一致", requireContext())
                    return@setPositiveButton
                }

                if (oldPassword != currentUser?.userPassword) {
                    MyToast.sendToast("原密码不正确", requireContext())
                    return@setPositiveButton
                }

                tempUser?.userPassword = newPassword
                MyToast.sendToast("密码已修改，点击保存提交更改", requireContext())
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // 添加权限标志，避免URI访问权限问题
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_AVATAR)
    }

    // 2. 修改 updateAvatar 方法，修复权限申请逻辑（避免重复申请导致崩溃）
    private fun updateAvatar(uri: Uri) {
        try {
            // 仅在未获取过权限时申请，避免重复申请导致异常
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            // 已获取过权限或不需要权限时忽略
            e.printStackTrace()
        }

        tempUser?.userAvatar = uri.toString()
        Glide.with(this).load(uri).into(binding.ivAvatar)
        MyToast.sendToast("头像已更新，点击保存提交更改", requireContext())
    }
    // 2. 确保 saveUserChanges 方法正确保存（原有代码正确，无需修改，但需确认调用）
    private fun saveUserChanges() {
        if (tempUser == null) {
            MyToast.sendToast("用户信息为空", requireContext())
            return
        }
        UserMMKV.saveUser(tempUser!!) // 关键：持久化到MMKV
        currentUser = tempUser?.copy() // 同步当前用户
        MyToast.sendToast("保存成功", requireContext())
    }
}