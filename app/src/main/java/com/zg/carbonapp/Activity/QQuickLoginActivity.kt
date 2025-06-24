package com.zg.carbonapp.Activity

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.ActivityQqQuickLoginBinding

class QQuickLoginActivity : AppCompatActivity() {
    private lateinit var binding:ActivityQqQuickLoginBinding

    private val qqLoginButton by lazy { binding.loginByQQ }
    private val qqEditText by lazy { binding.qqEditText }
    private val codeEditText by lazy { binding.codeEditText }
    private val sendCodeButton by lazy { binding.sendCodeTextView }
    private var countDownTimer: CountDownTimer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         binding=ActivityQqQuickLoginBinding.inflate(layoutInflater)
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
            sendCodeButton.isEnabled = false
              sendCodeButton .text = "${count--}s后重发"
            }

            override fun onFinish() {
            sendCodeButton.isEnabled = true
            sendCodeButton.text = "发送验证码"
            }
        }.start()
    }

    private fun initListener(){
          qqLoginButton.setOnClickListener{
               val qqEditText=qqEditText.text.toString()
               val codeEditText=codeEditText.text.toString()

               if (qqEditText.isEmpty()||codeEditText.isEmpty()){
                    MyToast.sendToast("请先将信息补充完整",this)

               }
              else {
                  // api

                  if(qqEditText=="1693573616@qq.com"&&codeEditText=="1234"){
                      IntentHelper.goIntent(this,MainActivity::class.java)
                  }

               }
          }

        sendCodeButton.setOnClickListener{
            //上面是调用api发送验证码

            startCountDown()//开始计时 这是必须的 然后的哈那就是另说了
        }
    }
}