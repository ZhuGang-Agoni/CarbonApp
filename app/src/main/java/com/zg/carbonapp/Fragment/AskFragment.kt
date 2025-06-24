package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentAskBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.text.Editable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class AskFragment : Fragment() {
    private lateinit var binding: FragmentAskBinding
    private var debounceJob: Job? = null // 防抖协程任务

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化按钮状态
        binding.btnSend.visibility = View.VISIBLE

        // 设置文本变化监听（带防抖）
        binding.inputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val content = s.toString().trim()
                // 输入内容变化后延迟300ms判断（防抖）
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // 延迟300ms
                    if (content.isNotEmpty()) {
                        binding.btnSend.visibility = View.VISIBLE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 设置按钮点击事件
        binding.btnSend.setOnClickListener {
            val editContent = binding.inputText.text.toString()
            if (editContent.isEmpty()) {
                MyToast.sendToast("请输入你要查找的相关信息", requireContext())
            } else {
                // 发送信息后隐藏按钮
                binding.btnSend.visibility = View.GONE
                // 清空输入框
                binding.inputText.setText("")

                DeepSeekHelper().sendMessage(editContent) { res ->
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.textRes.text = res
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob?.cancel() // 销毁Fragment时取消防抖任务
    }
}