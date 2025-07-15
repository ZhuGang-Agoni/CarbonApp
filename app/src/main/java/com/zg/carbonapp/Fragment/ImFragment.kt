package com.zg.carbonapp.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.zg.carbonapp.Activity.LoginActivity
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentImBinding

class ImFragment : Fragment() {
    private lateinit var binding: FragmentImBinding
    private var currentUser: User? = null
    private var tempUser: User? = null

    // 请求码
    private val REQUEST_CODE_AVATAR = 1001
    private val REQUEST_CODE_CAMERA = 1002

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

        // 退出登录
        binding.logoutText.setOnClickListener {
            showLogoutDialog()
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

        // 头像点击事件
        binding.ivAvatar.setOnClickListener {
            showAvatarOptions()
        }

        // 密码修改
        binding.userPasswordName.setOnClickListener {
            showChangePasswordDialog()
        }

        // 用户名修改
        binding.userNameEdit.setOnClickListener {
            showEditDialog("用户名", binding.userNameEdit.text.toString()) { newValue ->
                binding.userNameEdit.text = newValue
                tempUser?.userName = newValue
            }
        }

        // 签名修改
        binding.userSignatureEdit.setOnClickListener {
            showEditDialog("个性签名", binding.userSignatureEdit.text.toString()) { newValue ->
                binding.userSignatureEdit.text = newValue
                tempUser?.signature = newValue
            }
        }
    }

    private fun loadUserInfo() {
        // 尝试从MMKV加载用户信息
        currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            // 创建默认用户  由于后端还没有搭建起来 所以这个是个麻烦
            currentUser = User(
                userId = "123456",
                userName = "Agoni",
                signature = "点击编辑个性签名",
                userQQ = "1693573616@qq.com",
                userTelephone = "18073049251",
                userEvator = "",
                carbonCount = 100,
                userPassword = "123456"
            )
            UserMMKV.saveUser(currentUser!!)
        }

        tempUser = currentUser?.copy()
        updateUI(currentUser!!)
    }

    private fun updateUI(user: User) {
        binding.userNameEdit.text = user.userName
        binding.userCount.text = user.userId
        binding.userQqName.text = user.userQQ
        binding.userPhoneName.text = user.userTelephone
        binding.userCarbonCount.text = user.carbonCount.toString()
        binding.userSignatureEdit.text = user.signature

        // 加载头像
        if (user.userEvator.isNotEmpty()) {
            Glide.with(this)
                .load(user.userEvator)
                .into(binding.ivAvatar)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ -> logout() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun logout() {
        TokenManager.clearToken()
        UserMMKV.clearUser()
        IntentHelper.goIntent(requireContext(), LoginActivity::class.java)
        requireActivity().finish()
    }

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
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_CAMERA)
        } else {
            MyToast.sendToast("无法打开相机", requireContext())
        }
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_AVATAR)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_AVATAR -> data?.data?.let { updateAvatar(it) }
                REQUEST_CODE_CAMERA -> MyToast.sendToast("拍照功能需要进一步处理", requireContext())
            }
        }
    }

    private fun updateAvatar(uri: Uri) {
        tempUser?.userEvator = uri.toString()
        Glide.with(this).load(uri).into(binding.ivAvatar)
        MyToast.sendToast("头像已更新，点击保存提交更改", requireContext())
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

    private fun saveUserChanges() {
        if (tempUser == null) {
            MyToast.sendToast("用户信息为空", requireContext())
            return
        }

        // 保存到MMKV
        UserMMKV.saveUser(tempUser!!)
        currentUser = tempUser?.copy()
        MyToast.sendToast("保存成功", requireContext())
    }
}