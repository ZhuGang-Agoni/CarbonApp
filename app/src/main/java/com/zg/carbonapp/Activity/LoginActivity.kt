// LoginActivity.kt - 修改登录请求参数
package com.zg.carbonapp.Activity

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.Service.CarbonService
import com.zg.carbonapp.Service.LoginRequest
import com.zg.carbonapp.Service.RetrofitClient
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {
    // 自动生成的视图绑定类（基于布局文件名activity_login.xml）
    private lateinit var binding: ActivityLoginBinding
    // 使用 private + 懒加载方式初始化所有视图绑定
    private val loginButton by lazy { binding.loginButton }
    private val phoneQuickLoginText by lazy { binding.phoneQuickLoginText }
    private val forgotPasswordButton by lazy { binding.forgotPasswordButton }
    private val passwordEditText by lazy { binding.passwordEditText }
    private val registerButton by lazy { binding.registerButton }
    private val userNameEditText by lazy { binding.userNameEditText }
    private val userAgreementTextView by lazy { binding.userAgreementTextView }
    private val rememberPasswordCheckBox by lazy { binding.rememberPasswordCheckBox }
    private var countDownTimer: CountDownTimer? = null

    private val apiService: CarbonService
        get() = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 通过视图绑定初始化布局
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查是否已登录
        if (TokenManager.isLoggedIn()) {
            IntentHelper.goIntent(this, MainActivity::class.java)
            finish()
            return
        }

        // 记住密码功能：启动时自动填充
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val savedUser = prefs.getString("userName", "")
        val savedPwd = prefs.getString("userPwd", "")
        val remember = prefs.getBoolean("rememberPwd", false)
        userNameEditText.setText(savedUser)
        passwordEditText.setText(savedPwd)
        rememberPasswordCheckBox.isChecked = remember

        initListener()

    }


    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel() // 销毁页面时取消倒计时，避免内存泄漏
    }

    private fun initListener() {
        loginButton.setOnClickListener {
            val userTelephone = userNameEditText.text.toString()
            val userPassword = passwordEditText.text.toString()

            if (userPassword.isEmpty() || userTelephone.isEmpty()) {
                MyToast.sendToast("请输入你的账号和密码", this)
            } else {
                // 禁用登录按钮防止重复点击
                loginButton.isEnabled = false

                // 调用API进行登录
                lifecycleScope.launch {
                    try {
                        val response = apiService.login(LoginRequest(userTelephone, userPassword))
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.code == 1) {
                                // 保存token
                                apiResponse.data?.let { token ->
                                    TokenManager.setToken(token)
                                }
                                UserMMKV.setUserTelephone(userTelephone)

                                // 记住密码逻辑
                                val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
                                if (rememberPasswordCheckBox.isChecked) {
                                    prefs.edit()
                                        .putString("userName", userTelephone)
                                        .putString("userPwd", userPassword)
                                        .putBoolean("rememberPwd", true)
                                        .apply()
                                } else {
                                    prefs.edit()
                                        .remove("userName")
                                        .remove("userPwd")
                                        .putBoolean("rememberPwd", false)
                                        .apply()
                                }

                                MyToast.sendToast("登录成功", this@LoginActivity)
                                IntentHelper.goIntent(this@LoginActivity, MainActivity::class.java)
                                finish()
                            } else {
                                MyToast.sendToast(
                                    apiResponse?.message ?: "登录失败",
                                    this@LoginActivity
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is HttpException) {
                            when (e.code()) {
                                401 -> MyToast.sendToast("用户名或密码错误", this@LoginActivity)
                                500 -> MyToast.sendToast("服务器内部错误", this@LoginActivity)
                                else -> MyToast.sendToast("网络错误: ${e.message}", this@LoginActivity)
                            }
                        } else {
                            MyToast.sendToast("网络错误: ${e.message}", this@LoginActivity)
                        }
                    } finally {
                        // 重新启用登录按钮
                        loginButton.isEnabled = true
                    }
                }
            }
        }

        registerButton.setOnClickListener {
            IntentHelper.goIntent(this, RegisterActivity::class.java)
        }

        forgotPasswordButton.setOnClickListener {
            IntentHelper.goIntent(this, ForgetPasswordActivity::class.java)
        }

        phoneQuickLoginText.setOnClickListener {
            IntentHelper.goIntent(this, QQuickLoginActivity::class.java)
        }
    }
}