package com.zg.carbonapp.Activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityResearchResBinding
import java.io.ByteArrayOutputStream

class ResearchResActivity : AppCompatActivity() {
    private lateinit var binding:ActivityResearchResBinding
    private lateinit var webView:WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityResearchResBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

         webView=binding.webView

        with(webView.settings) {
            javaScriptEnabled=true
            domStorageEnabled=true

            allowFileAccess=true
            allowContentAccess=true
            allowFileAccessFromFileURLs=true
            allowUniversalAccessFromFileURLs=true

            useWideViewPort=true
            loadWithOverviewMode=true
        }
         //添加接口
        webView.addJavascriptInterface(javaScriptInterface(),"AndroidInterface")

       webView.webViewClient=object :WebViewClient(){
           override fun onPageFinished(view: WebView?, url: String?) {
               super.onPageFinished(view, url)
               super.onPageFinished(view, url)
               Log.d("WebView", "Page finished loading")

               // 确保页面完全加载后再加载图片
               view?.postDelayed({
                   //这里的loadImageFromDrawable是自定义方法
                   loadImageFromDrawable(R.drawable.img) // 替换为你的图片资源ID
               }, 1000)
           }
       }

        // 加载本地 HTML 文件
        webView.loadUrl("file:///android_asset/3d_viewer.html")

           }

    @SuppressLint("SuspiciousIndentation")
    private fun loadImageFromDrawable(img: Int) {
          val bitmap=BitmapFactory.decodeResource(resources,img)

          if (bitmap!=null){
            val byteOut=ByteArrayOutputStream()

            //开始压缩
            bitmap.compress(Bitmap.CompressFormat.PNG,100,byteOut)
            val byteArray=byteOut.toByteArray()

            val jsCode="""
                 if (typeof window.loadImageFromAndroid !== 'undefined') {
                    window.loadImageFromAndroid([${byteArray.joinToString(",")}]);
                } else {
                    console.error('loadImageFromAndroid function not defined');
                }
                
                
            """.trimIndent()
              webView.post{
                  webView.evaluateJavascript(jsCode,{ result ->
                      Log.d("JSExecution", "Image loading result: $result")

                  })
              }
          }else{
              showError("加载失败")
          }
    }
private fun showError(message:String){
    Log.e("ResearchResActivity",message)
    webView.evaluateJavascript("alert('失败')",null)
}
    inner class javaScriptInterface{
        @JavascriptInterface
        fun log(message:String){
            Log.e("ResearchResActivity",message)
        }

    }
}