package com.zg.carbonapp.Fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.zg.carbonapp.Activity.UserCommentActivity
import com.zg.carbonapp.Adapter.CommunityFeedAdapter
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.databinding.FragmentSavesFeedsBinding
//import com.zg.carbonapp.databinding.FragmentSavedFeedsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavesFeedsFragment : Fragment() {
    private lateinit var binding: FragmentSavesFeedsBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var savedFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002  // 保持与全局Fragment一致

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavesFeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRefreshAndRecycler()
        loadSavedFeeds()
    }

    // 核心修复：与全局Fragment相同的评论数刷新逻辑
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val feedId = data?.getStringExtra("feedId")
            val newCommentCount = data?.getIntExtra("newCommentCount", 0) ?: 0

            // 1. 从全量数据获取最新动态
            lifecycleScope.launch(Dispatchers.IO) {
                val allFeeds = MMKVManager.getAllFeeds()
                val updatedFeed = allFeeds.firstOrNull { it.feedId == feedId }

                withContext(Dispatchers.Main) {
                    // 2. 局部刷新UI
                    updatedFeed?.let { updateLocalFeed(it) }
                    // 3. 全量刷新确保一致
                    loadSavedFeeds()
                }
            }
        }
    }

    // 直接替换对象而非仅更新评论数
    private fun updateLocalFeed(updatedFeed: UserFeed) {
        val targetIndex = savedFeedList.indexOfFirst { it.feedId == updatedFeed.feedId }
        if (targetIndex != -1) {
            savedFeedList[targetIndex] = updatedFeed
            adapter.notifyItemChanged(targetIndex)
        }
    }

    private fun setupRefreshAndRecycler() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light
            )
            setOnRefreshListener { loadSavedFeeds() }
        }

        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) },
            onShareClick = { handleSaveClick(it) },
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SavesFeedsFragment.adapter
            setHasFixedSize(true)
        }
    }

    // 关键修复：从全量数据中筛选收藏内容，确保数据新鲜
    private fun loadSavedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取全量最新数据
                val allFeeds = MMKVManager.getAllFeeds()
                // 2. 获取收藏的ID集合
                val savedFeedIds = MMKVManager.getSavedFeeds().map { it.feedId }.toSet()
                // 3. 从全量数据中筛选，保证数据是最新的
                val filteredFeeds = allFeeds
                    .filter { savedFeedIds.contains(it.feedId) }
                    .sortedByDescending { it.createTime }
                    .toMutableList()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    savedFeedList = filteredFeeds
                    adapter.submitList(savedFeedList)
                }
            } catch (e: Exception) {
                Log.e("SavedFeeds", "加载失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) Toast.makeText(requireContext(), "加载收藏动态失败", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded && binding.swipeRefresh.isRefreshing) {
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun handleLikeClick(position: Int) {
        if (position >= savedFeedList.size) return
        val targetFeed = savedFeedList[position]
        val newLikeState = !targetFeed.isLiked
        val currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val allFeeds = MMKVManager.getAllFeeds().toMutableList()
            val index = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
            if (index != -1) {
                allFeeds[index] = allFeeds[index].copy(
                    isLiked = newLikeState,
                    likeCount = allFeeds[index].likeCount + if (newLikeState) 1 else -1
                )
                MMKVManager.saveAllFeeds(allFeeds)

                val likedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                if (newLikeState) {
                    if (!likedFeeds.any { it.feedId == targetFeed.feedId }) {
                        likedFeeds.add(allFeeds[index])
                    }
                } else {
                    likedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }
                MMKVManager.saveLikedFeeds(likedFeeds)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        if (newLikeState) "点赞成功" else "取消点赞",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleSaveClick(position: Int) {
        if (position >= savedFeedList.size) return
        val targetFeed = savedFeedList[position]
        val newSaveState = !targetFeed.isSaved
        val currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val allFeeds = MMKVManager.getAllFeeds().toMutableList()
            val index = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
            if (index != -1) {
                allFeeds[index] = allFeeds[index].copy(
                    isSaved = newSaveState,
                    shareCount = allFeeds[index].shareCount + if (newSaveState) 1 else -1
                )
                MMKVManager.saveAllFeeds(allFeeds)

                val savedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                if (newSaveState) {
                    if (!savedFeeds.any { it.feedId == targetFeed.feedId }) {
                        savedFeeds.add(allFeeds[index])
                    }
                } else {
                    savedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }
                MMKVManager.saveSavedFeeds(savedFeeds)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadSavedFeeds() // 重新加载列表确保数据一致
                    Toast.makeText(
                        requireContext(),
                        if (newSaveState) "收藏成功" else "取消收藏",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleCommentClick(position: Int) {
        if (position >= savedFeedList.size) return
        val targetFeed = savedFeedList[position]
        val currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    private fun handleAvatarClick(position: Int) {
        if (position < savedFeedList.size && isAdded) {
            val username = savedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }
}
