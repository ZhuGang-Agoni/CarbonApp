package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityRegisterBinding

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
//    private val apiService by lazy { createApiService() } // 假设已定义网络请求服务

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



                startCountDown()//开始计时
            }
        }


        registerTextView.setOnClickListener{
            val newPasswordEdit = passwordEditText.text.toString()
            val newPasswordEditAgain = passwordEditTextAgain.text.toString()
            val phoneEdit = phoneEditText.text.toString()
            val codeEdit = codeEditText.text.toString()
            val userName=userName.text.toString()

            if (newPasswordEdit.isEmpty()||
                newPasswordEditAgain.isEmpty()||
                phoneEdit.isEmpty()||
                codeEdit.isEmpty()||
                userName.isEmpty()){

                MyToast.sendToast("请将上面信息补充完整",this)

            }
            else {
                //这里应该有一个api逻辑 用于将 先关信息发送给后端保存起来（暂时先不管）


                MyToast.sendToast("注册成功",this)
                IntentHelper.goIntent(this,LoginActivity::class.java)

            }
        }
    }
}

