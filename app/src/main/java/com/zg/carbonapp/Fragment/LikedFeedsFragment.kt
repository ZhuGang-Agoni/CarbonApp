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
import com.zg.carbonapp.databinding.FragmentLikedFeedsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikedFeedsFragment : Fragment() {
    private lateinit var binding: FragmentLikedFeedsBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var likedFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002  // 与CommunityFeedFragment保持一致的请求码

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLikedFeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRefreshAndRecycler()
        loadLikedFeeds()
    }

    // 关键修复1：使用与全局Fragment完全一致的评论数刷新逻辑
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val feedId = data?.getStringExtra("feedId")
            val newCommentCount = data?.getIntExtra("newCommentCount", 0) ?: 0

            // 1. 先从全量数据中获取最新的动态对象（确保数据绝对新鲜）
            lifecycleScope.launch(Dispatchers.IO) {
                val allFeeds = MMKVManager.getAllFeeds()
                val updatedFeed = allFeeds.firstOrNull { it.feedId == feedId }

                withContext(Dispatchers.Main) {
                    // 2. 先局部刷新UI（优先保证体验）
                    updatedFeed?.let { updateLocalFeed(it) }
                    // 3. 再全量刷新列表（确保数据最终一致）
                    loadLikedFeeds()
                }
            }
        }
    }

    // 关键修复2：直接使用全量数据中的对象替换当前列表中的对象
    private fun updateLocalFeed(updatedFeed: UserFeed) {
        val targetIndex = likedFeedList.indexOfFirst { it.feedId == updatedFeed.feedId }
        if (targetIndex != -1) {
            // 直接替换整个对象，而不仅仅是评论数
            likedFeedList[targetIndex] = updatedFeed
            adapter.notifyItemChanged(targetIndex)
        }
    }

    private fun setupRefreshAndRecycler() {
        // 下拉刷新配置
        binding.swipeRefresh.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light
            )
            setOnRefreshListener { loadLikedFeeds() }
        }

        // 适配器配置
        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) },
            onShareClick = { handleSaveClick(it) },
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LikedFeedsFragment.adapter
            setHasFixedSize(true)
        }
    }

    // 关键修复3：加载逻辑与全量数据强关联
    private fun loadLikedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 先获取全量最新数据
                val allFeeds = MMKVManager.getAllFeeds()
                // 2. 再获取点赞列表的ID集合（只存ID，避免数据不一致）
                val likedFeedIds = MMKVManager.getLikedFeeds().map { it.feedId }.toSet()
                // 3. 从全量数据中筛选出点赞的动态（保证数据是最新的）
                val filteredFeeds = allFeeds
                    .filter { likedFeedIds.contains(it.feedId) }
                    .sortedByDescending { it.createTime }
                    .toMutableList()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    likedFeedList = filteredFeeds
                    adapter.submitList(likedFeedList)
                }
            } catch (e: Exception) {
                Log.e("LikedFeeds", "加载失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) Toast.makeText(requireContext(), "加载点赞动态失败", Toast.LENGTH_SHORT).show()
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

    // 点赞逻辑（保持与全局Fragment一致）
    private fun handleLikeClick(position: Int) {
        if (position >= likedFeedList.size) return
        val targetFeed = likedFeedList[position]
        val newLikeState = !targetFeed.isLiked
        val currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 更新全量数据
            val allFeeds = MMKVManager.getAllFeeds().toMutableList()
            val allFeedIndex = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
            if (allFeedIndex != -1) {
                allFeeds[allFeedIndex] = allFeeds[allFeedIndex].copy(
                    isLiked = newLikeState,
                    likeCount = allFeeds[allFeedIndex].likeCount + if (newLikeState) 1 else -1
                )
                MMKVManager.saveAllFeeds(allFeeds)
            }

            // 更新点赞列表（只存ID集合，减少数据冗余）
            val likedFeeds = MMKVManager.getLikedFeeds().toMutableList()
            if (newLikeState) {
                if (!likedFeeds.any { it.feedId == targetFeed.feedId }) {
                    allFeeds[allFeedIndex]?.let { likedFeeds.add(it) }
                }
            } else {
                likedFeeds.removeAll { it.feedId == targetFeed.feedId }
            }
            MMKVManager.saveLikedFeeds(likedFeeds)

            // 刷新UI
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                loadLikedFeeds() // 直接重新加载列表，确保数据一致
                Toast.makeText(
                    requireContext(),
                    if (newLikeState) "点赞成功" else "取消点赞",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleCommentClick(position: Int) {
        if (position >= likedFeedList.size) return
        val targetFeed = likedFeedList[position]
        val currentUser = UserMMKV.getUser()

        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    private fun handleSaveClick(position: Int) {
        if (position >= likedFeedList.size) return
        val targetFeed = likedFeedList[position]
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
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        if (newSaveState) "收藏成功" else "取消收藏",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleAvatarClick(position: Int) {
        if (position < likedFeedList.size && isAdded) {
            val username = likedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }
}
