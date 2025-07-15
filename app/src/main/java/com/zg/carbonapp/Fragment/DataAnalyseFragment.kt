package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.FragmentDataAnalysisBinding


class DataAnalyseFragment : Fragment() {

    private lateinit var binding: FragmentDataAnalysisBinding
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    //这边是前端的一个实际地址
    private val baseUrl = "https://baidu.com"  // 示例：如 "file:///android_asset/carbon_visual.html"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDataAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initListener()
        initWebView()
        initTimeFilterButtons()
    }

    private fun initListener(){
        binding.hint.setOnClickListener{
             //弹出一个对话框

            AlertDialog.Builder(requireContext())
                .setTitle("数据提示")  // 标题关联时间范围
                .setMessage("开发中 继续努力！！！")                     // 动态加载提示内容
                .setPositiveButton("好的") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()




        }
    }

    private fun initViews() {
        webView = binding.webviewCarbonData
        progressBar = binding.progressBar
    }


    private fun initWebView() {
        val webSettings: WebSettings = webView.settings
        // 支持JavaScript（前端可视化通常需要JS）
        webSettings.javaScriptEnabled = true
        // 支持缩放
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        // 自适应屏幕
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        // 允许跨域访问（如果前端需要调用接口）
        webSettings.allowUniversalAccessFromFileURLs = true

        // 监听加载进度
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // 处理页面跳转（在当前WebView打开，不跳转到浏览器）
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
        }

        // 初始加载今日数据
        loadCarbonData("today")
    }


    private fun initTimeFilterButtons() {
        // 今日按钮
        binding.btnToday.setOnClickListener {
            updateButtonStyle(it as MaterialButton, binding.btnWeek, binding.btnMonth)
            loadCarbonData("today")
        }
        // 本周按钮
        binding.btnWeek.setOnClickListener {
            updateButtonStyle(it as MaterialButton, binding.btnToday, binding.btnMonth)
            loadCarbonData("week")
        }
        // 本月按钮
        binding.btnMonth.setOnClickListener {
            updateButtonStyle(it as MaterialButton, binding.btnToday, binding.btnWeek)
            loadCarbonData("month")
        }
    }

    /**
     * 加载对应时间范围的碳数据（通过URL参数传递给前端）
     * @param timeRange 时间范围：today/week/month
     */

    private fun loadCarbonData(timeRange: String) {
        // 拼接URL参数（前端页面需要根据该参数展示对应数据）
//        val url = "$baseUrl?time=$timeRange"
        val url="https://siyuan.fucheng.online/"
        webView.loadUrl(url)
    }

    /**
     * 更新按钮样式（选中状态加粗边框）
     */
    private fun updateButtonStyle(
        selectedBtn: MaterialButton,
        unselectedBtn1: MaterialButton,
        unselectedBtn2: MaterialButton
    ) {
        //这个是那个啥 拐角还是啥
        selectedBtn.strokeWidth = 2
        selectedBtn.setTextColor(resources.getColor(R.color.black))  // 选中状态文字变黑

        unselectedBtn1.strokeWidth = 1
        unselectedBtn1.setTextColor(resources.getColor(R.color.gray))  // 未选中状态文字变灰

        unselectedBtn2.strokeWidth = 1
        unselectedBtn2.setTextColor(resources.getColor(R.color.gray))
    }

    // 处理WebView生命周期（避免内存泄漏）
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView.destroy()  // 销毁WebView释放资源
    }
}