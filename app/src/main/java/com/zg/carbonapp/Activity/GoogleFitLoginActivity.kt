//package com.zg.carbonapp.Activity
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.fitness.FitnessOptions
//import com.zg.carbonapp.R
//import com.zg.carbonapp.Service.SensorManager
//import com.zg.carbonapp.Tool.MyToast
//
///**
// * Google Fit登录界面
// *
// * 功能说明：
// * 1. 引导用户授权Google Fit权限
// * 2. 检查用户是否已经登录Google Fit
// * 3. 处理Google Fit权限请求结果
// *
// * 使用场景：
// * - 用户首次使用步数统计功能时
// * - 用户需要重新授权Google Fit权限时
// */
//class GoogleFitLoginActivity : AppCompatActivity() {
//    companion object {
//        private const val TAG = "GoogleFitLoginActivity"
//        // Google Fit权限请求码，用于标识权限请求结果
//        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
//    }
//
//    // 传感器管理器，用于处理Google Fit相关操作
//    private lateinit var sensorManager: SensorManager
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_google_fit_login)
//
//        // 初始化传感器管理器
//        sensorManager = SensorManager(this)
//
//        // 检查用户是否已经登录Google Fit
//        if (sensorManager.isGoogleFitSignedIn()) {
//            MyToast.sendToast("已登录Google Fit", this)
//            finish() // 如果已登录，直接关闭当前界面
//            return
//        }
//
//        // 设置Google Fit登录按钮的点击事件
//        findViewById<android.widget.Button>(R.id.btnGoogleFitLogin).setOnClickListener {
//            requestGoogleFitPermissions()
//        }
//    }
//
//    /**
//     * 请求Google Fit权限
//     *
//     * 流程说明：
//     * 1. 获取Google Fit服务配置
//     * 2. 检查当前账号是否已有权限
//     * 3. 如果没有权限，弹出Google账号选择界面
//     * 4. 用户选择账号并授权后，在onActivityResult中处理结果
//     */
//    private fun requestGoogleFitPermissions() {
//        // 获取Google Fit服务配置（包含需要的数据类型权限）
//        val fitnessOptions = sensorManager.getGoogleFitService().getFitnessOptions()
//        // 获取当前Google账号
//        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
//
//        // 检查是否已有权限
//        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
//            // 没有权限，请求用户授权
//            GoogleSignIn.requestPermissions(
//                this,
//                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // 请求码，用于标识这次权限请求
//                account, // 用户选择的Google账号
//                fitnessOptions // 需要的数据类型权限配置
//            )
//        } else {
//            // 已有权限，直接提示并关闭界面
//            MyToast.sendToast("已获得Google Fit权限", this)
//            finish()
//        }
//    }
//
//    /**
//     * 处理Google Fit权限请求结果
//     *
//     * @param requestCode 请求码，用于识别是哪次权限请求
//     * @param resultCode 结果码，表示权限请求是否成功
//     * @param data 返回的数据，包含用户选择的账号信息
//     */
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        // 检查是否是Google Fit权限请求的结果
//        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
//            val fitnessOptions = sensorManager.getGoogleFitService().getFitnessOptions()
//            val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
//
//            // 检查用户是否授权成功
//            if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
//                MyToast.sendToast("Google Fit登录成功", this)
//                finish() // 登录成功，关闭当前界面
//            } else {
//                MyToast.sendToast("Google Fit登录失败或被拒绝", this)
//                // 用户拒绝授权，界面保持打开状态，用户可以重试
//            }
//        }
//    }
//}