package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.Activity.ForgetPasswordActivity
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityLoginBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 通过视图绑定初始化布局
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun initListener(){

        loginButton.setOnClickListener {
            // 应该在点击时获取最新的输入值
            val userName = userNameEditText.text.toString()
            val userPassword = passwordEditText.text.toString()

            if (userPassword.isEmpty() || userName.isEmpty()) {
                MyToast.sendToast("请输入你的账号和密码", this)
            } else {
                // 这里应该是调用后端的一个api来进行验证 暂时简单一点

                if (userName == "admin" && userPassword == "123456") {
                    // 记住密码逻辑
                    val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
                    if (rememberPasswordCheckBox.isChecked) {
                        prefs.edit()
                            .putString("userName", userName)
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
                    MyToast.sendToast("登陆成功", this)
                    IntentHelper.goIntent(this, MainActivity::class.java)
                } else {
                    MyToast.sendToast("用户名或密码错误", this)
                }
            }
        }
        registerButton.setOnClickListener{
            IntentHelper.goIntent(this,RegisterActivity::class.java)
        }

        forgotPasswordButton.setOnClickListener{
             IntentHelper.goIntent(this,ForgetPasswordActivity::class.java)
        }

        phoneQuickLoginText.setOnClickListener{
            //只能快速登陆了
             IntentHelper.goIntent(this,QQuickLoginActivity::class.java)
        }


    }
}