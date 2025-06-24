package com.zg.carbonapp.Activity

import com.zg.carbonapp.Activity.LoginActivity


import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityForgetPasswordBinding

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgetPasswordBinding
    private val newPasswordEditText by lazy { binding.newPasswordEditText }
    private val backToLogin by lazy { binding.backToLogin }
    private val progressBar by lazy { binding.progressBar }
    private val codeEditText by lazy { binding.codeEditText }
    private val phoneEditText by lazy { binding.phoneEditText }
    private val newPasswordEditTextAgain by lazy { binding.newPasswordEditTextAgain }
    private val resetTextView by lazy { binding.resetTextView }
    private val sendCodeTextView by lazy { binding.sendCodeTextView }
    private var countDownTimer: CountDownTimer? = null


//    private var countDownTimer: CountDownTimer? = null
//    private val apiService by lazy { createApiService() } // 假设已定义网络请求服务

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()


    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

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
        //返回去登录界面
        backToLogin.setOnClickListener {
            IntentHelper.goIntent(this, LoginActivity::class.java)
        }

        sendCodeTextView.setOnClickListener {

            val phoneEdit = phoneEditText.text.toString()
            if (phoneEdit.isEmpty()){
                MyToast.sendToast("验证码发送失败，请先输入你绑定的电话",this)
            }
            else {//这里是一个具体的逻辑了 暂时先不管  //这里面要调用具体的api 先空出来


                startCountDown()//开始一个计时功能
            }

        }

        resetTextView.setOnClickListener {
            val newPasswordEdit = newPasswordEditText.text.toString()
            val newPasswordEditAgain = newPasswordEditTextAgain.text.toString()
            val phoneEdit = phoneEditText.text.toString()
            val codeEdit = codeEditText.text.toString()

            if (newPasswordEdit.isEmpty() ||
                newPasswordEditAgain.isEmpty() ||
                phoneEdit.isEmpty() ||
                codeEdit.isEmpty()
            ) {

                MyToast.sendToast("请将相关信息补充完整", this)
            } else {
                //这里应该还需要一个 将信息发给后端保存起来 暂时把位置先空出来


                MyToast.sendToast("重置成功", this)
                IntentHelper.goIntent(this, LoginActivity::class.java)
            }


        }
    }
}


