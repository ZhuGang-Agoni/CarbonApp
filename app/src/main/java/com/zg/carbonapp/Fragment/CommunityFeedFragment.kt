package com.zg.carbonapp.Fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Activity.PostFeedActivity
import com.zg.carbonapp.Adapter.CommunityFeedAdapter
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.FragmentCommunityFeedBinding
import com.zg.carbonapp.MMKV.MMKVManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommunityFeedFragment : Fragment() {
    private lateinit var binding: FragmentCommunityFeedBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var feedList: MutableList<UserFeed> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置发布按钮（仅打开发布页面，无登录检查）
        binding.fabPost.setOnClickListener {
            val intent = Intent(requireContext(), PostFeedActivity::class.java)
            startActivityForResult(intent, 1001)
        }

        initRecyclerView()
        loadFeeds()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadFeeds()
        }
    }

    private fun initRecyclerView() {
        adapter = CommunityFeedAdapter(
            onLikeClick = { position -> handleLikeClick(position) },
            onCommentClick = { position -> },
            onShareClick = { position -> },
            onAvatarClick = { position -> }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommunityFeedFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadFeeds() {
//        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val cachedFeeds = MMKVManager.getFeeds()

            with(Dispatchers.Main) {
                feedList = cachedFeeds.toMutableList()
                adapter.submitList(feedList)

//                binding.progressBar.visibility = View.GONE
                // 不进行任何登录相关判断，仅显示数据
            }
        }
    }

    private fun handleLikeClick(position: Int) {
        if (position < feedList.size) {
            val feed = feedList[position]
            feed.isLiked = !feed.isLiked
            feed.likeCount += if (feed.isLiked) 1 else -1
            adapter.notifyItemChanged(position)
            // 更新本地数据，不涉及登录状态
        }
    }
}