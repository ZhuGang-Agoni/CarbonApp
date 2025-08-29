package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.DeepSeekHelper
import com.zg.carbonapp.Tool.MyToast
import com.zg.carbonapp.databinding.FragmentAskBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AskFragment : Fragment() {
    private lateinit var binding: FragmentAskBinding
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private var debounceJob: Job? = null
    private var currentAiJob: Job? = null // 用于取消当前AI请求
    private var currentAiContent = "" // 记录当前AI消息的累积内容
    private var aiMessagePosition = -1 // 当前AI消息在列表中的位置

    // 消息数据类
    enum class MessageType { USER, AI }
    data class ChatMessage(var content: String, val type: MessageType) // content用var便于修改

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAskBinding.inflate(inflater, container, false)
        initRecyclerView()
        initEvent()
        return binding.root
    }

    // 初始化RecyclerView
    private fun initRecyclerView() {
        chatAdapter = ChatAdapter(chatList)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }
    }

    // 初始化事件监听
    private fun initEvent() {
        // 输入框文本变化监听（控制发送按钮显示）
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val content = s?.toString()?.trim() ?: ""
                debounceJob?.cancel()
                debounceJob = MainScope().launch {
                    delay(300) // 防抖延迟
                    binding.sendButton.visibility = if (content.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 发送按钮点击事件
        binding.sendButton.setOnClickListener {
            val input = binding.messageInput.text.toString().trim()
            if (input.isEmpty()) {
                MyToast.sendToast("请输入内容", requireContext())
                return@setOnClickListener
            }

            // 清空输入框
            binding.messageInput.text?.clear()
            // 添加用户消息
            addUserMessage(input)
            // 发起AI流式请求
            startAiStreaming(input)
        }
    }

    // 添加用户消息
    private fun addUserMessage(content: String) {
        chatList.add(ChatMessage(content, MessageType.USER))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        scrollToBottom()
    }

    // 发起AI流式请求
    private fun startAiStreaming(prompt: String) {
        currentAiJob?.cancel()
        currentAiContent = ""

        // 添加"思考中"占位消息
        chatList.add(ChatMessage("AI正在思考...", MessageType.AI))
        aiMessagePosition = chatList.size - 1
        chatAdapter.notifyItemInserted(aiMessagePosition)
        scrollToBottom()

        // 发起流式请求（强制按字符显示）
        val helper = DeepSeekHelper()
        currentAiJob = MainScope().launch {
            helper.sendMessageStream(
                prompt = prompt,
                charDelay = 70, // 调整字符显示速度（毫秒）
                onChar = { char -> // 每次只接收一个字符
                    currentAiContent += char
                    if (aiMessagePosition != -1 && aiMessagePosition < chatList.size) {
                        chatList[aiMessagePosition].content = currentAiContent
                        chatAdapter.notifyItemChanged(aiMessagePosition)
                        scrollToBottom()
                    }
                },
                onComplete = {
                    // 可选：添加完成标记
                },
                onError = { error ->
                    if (aiMessagePosition != -1) {
                        chatList[aiMessagePosition].content = "错误: $error"
                        chatAdapter.notifyItemChanged(aiMessagePosition)
                    }
                }
            )
        }
    }

    // 滚动到列表底部
    private fun scrollToBottom() {
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
    }

    // 适配器（展示聊天消息）
    inner class ChatAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {


           val TYPE_USER = 0
           val TYPE_AI = 1

        override fun getItemViewType(position: Int): Int {
            return if (messages[position].type == MessageType.USER) TYPE_USER else TYPE_AI
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_USER) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_ai, parent, false)
                AiViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            when (holder) {
                is UserViewHolder -> holder.content.text = message.content
                is AiViewHolder -> holder.content.text = message.content
            }
        }

        override fun getItemCount() = messages.size

        inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.chat_message)
        }

        inner class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tvAiContent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消所有协程任务，避免内存泄漏
        debounceJob?.cancel()
        currentAiJob?.cancel()
    }
}



