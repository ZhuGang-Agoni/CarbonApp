package com.zg.carbonapp.Activity

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zg.carbonapp.Service.CarbonService
import com.zg.carbonapp.Service.RetrofitClient
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val registerTextView by lazy { binding.registerTextView }
    private val backToLogin by lazy { binding.backToLogin }
    private val codeEditText by lazy { binding.codeEditText }
    private val progressBar by lazy { binding.progressBar }
    private val phoneEditText by lazy { binding.phoneEditText }
    private val passwordEditText by lazy { binding.passwordEditText }
    private val userName by lazy { binding.userNameEditText }
    private val passwordEditTextAgain by lazy { binding.passwordEditTextAgain }
    private val sendCodeTextView by lazy { binding.sendCodeTextView }
    private var countDownTimer: CountDownTimer? = null

    private val apiService: CarbonService
        get() = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    //这是一个计时功能
    private fun startCountDown() {
        countDownTimer?.cancel() // 取消可能存在的旧倒计时
        var count = 60
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sendCodeTextView.isEnabled = false
                sendCodeTextView.text = "${count--}s后重发"
            }

            override fun onFinish() {
                sendCodeTextView.isEnabled = true
                sendCodeTextView.text = "发送验证码"
            }
        }.start()
    }

    private fun initListener() {
        backToLogin.setOnClickListener {
            IntentHelper.goIntent(this, LoginActivity::class.java)
        }

        sendCodeTextView.setOnClickListener {
            val phoneEdit = phoneEditText.text.toString()
            if (phoneEdit.isEmpty()) {
                MyToast.sendToast("验证码发送失败，请先输入绑定的手机号", this)
            } else {
                // 由于验证码功能尚未实现，我们只启动倒计时UI
                startCountDown() // 开始计时
                MyToast.sendToast("验证码已发送", this)
            }
        }

        registerTextView.setOnClickListener{
            val newPasswordEdit = passwordEditText.text.toString()
            val newPasswordEditAgain = passwordEditTextAgain.text.toString()
            val phoneEdit = phoneEditText.text.toString()
            val codeEdit = codeEditText.text.toString()
            val userNameText = userName.text.toString()

            if (newPasswordEdit.isEmpty() ||
                newPasswordEditAgain.isEmpty() ||
                phoneEdit.isEmpty() ||
                codeEdit.isEmpty() ||
                userNameText.isEmpty()
            ) {
                MyToast.sendToast("请将上面信息补充完整", this)
            } else if (newPasswordEdit != newPasswordEditAgain) {
                MyToast.sendToast("两次输入的密码不一致", this)
            } else {
                // 调用注册API
                lifecycleScope.launch {
                    try {
                        // 显示进度条
                        progressBar.visibility = android.view.View.VISIBLE
                        registerTextView.isEnabled = false

                        // 调用注册API - 注意：这里使用手机号作为email
                        val response = apiService.register(
                            userName = userNameText,
                            email = phoneEdit, // 使用手机号作为email
                            userPassword = newPasswordEdit
                        )

                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.code == 200) {
                                MyToast.sendToast("注册成功", this@RegisterActivity)
                                IntentHelper.goIntent(this@RegisterActivity, LoginActivity::class.java)
                                finish()
                            } else {
                                MyToast.sendToast(apiResponse?.message ?: "注册失败", this@RegisterActivity)
                            }
                        } else {
                            MyToast.sendToast("注册失败: ${response.message()}", this@RegisterActivity)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is HttpException) {
                            when (e.code()) {
                                400 -> MyToast.sendToast("请求参数错误", this@RegisterActivity)
                                409 -> MyToast.sendToast("用户名或手机号已存在", this@RegisterActivity)
                                500 -> MyToast.sendToast("服务器内部错误", this@RegisterActivity)
                                else -> MyToast.sendToast("网络错误: ${e.message}", this@RegisterActivity)
                            }
                        } else {
                            MyToast.sendToast("网络错误: ${e.message}", this@RegisterActivity)
                        }
                    } finally {
                        // 隐藏进度条并重新启用注册按钮
                        progressBar.visibility = android.view.View.GONE
                        registerTextView.isEnabled = true
                    }
                }
            }
        }
    }
}