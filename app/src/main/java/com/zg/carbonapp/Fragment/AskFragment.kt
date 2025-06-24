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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.zg.carbonapp.R

class AskFragment : Fragment() {
    private lateinit var binding: FragmentAskBinding
    private var debounceJob: Job? = null // 防抖协程任务
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()

    // 消息类型
    enum class MessageType { USER, AI }
    // 消息数据类
    data class ChatMessage(val content: String, val type: MessageType)

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

        // 初始化RecyclerView
        chatAdapter = ChatAdapter(chatList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter

        // 初始化按钮状态
        binding.sendButton.visibility = View.VISIBLE

        // 设置文本变化监听（带防抖）
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val content = s.toString().trim()
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    if (content.isNotEmpty()) {
                        binding.sendButton.visibility = View.VISIBLE
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 设置按钮点击事件
        binding.sendButton.setOnClickListener {
            val editContent = binding.messageInput.text.toString()
            if (editContent.isEmpty()) {
                MyToast.sendToast("请输入你要查找的相关信息", requireContext())
            } else {
                // 添加用户消息到列表
                addMessage(editContent, MessageType.USER)
                binding.sendButton.visibility = View.GONE
                binding.messageInput.setText("")
                // 发送AI请求
                DeepSeekHelper().sendMessage(editContent) { res ->
                    CoroutineScope(Dispatchers.Main).launch {
                        addMessage(res, MessageType.AI)
                    }
                }
            }
        }
    }

    private fun addMessage(content: String, type: MessageType) {
        chatList.add(ChatMessage(content, type))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob?.cancel()
    }

    // RecyclerView Adapter
    class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            private const val TYPE_USER = 0
            private const val TYPE_AI = 1
        }
        override fun getItemViewType(position: Int): Int {
            return if (messages[position].type == MessageType.USER) TYPE_USER else TYPE_AI
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_USER) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
                AiViewHolder(view)
            }
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = messages[position]
            if (holder is UserViewHolder) {
                holder.content.text = msg.content
            } else if (holder is AiViewHolder) {
                holder.content.text = msg.content
            }
        }
        override fun getItemCount(): Int = messages.size
        class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tvUserContent)
        }
        class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val content: TextView = view.findViewById(R.id.tvAiContent)
        }
    }
}