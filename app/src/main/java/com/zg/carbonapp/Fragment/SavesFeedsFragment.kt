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
import com.zg.carbonapp.Activity.UserCommentActivity
import com.zg.carbonapp.Adapter.CommunityFeedAdapter
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.databinding.FragmentSavesFeedsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavesFeedsFragment : Fragment() {
    private lateinit var binding: FragmentSavesFeedsBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var savedFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002

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
        loadSavedFeeds() // 初始化加载收藏列表
    }

    /**
     * 初始化下拉刷新和RecyclerView
     */
    private fun setupRefreshAndRecycler() {
        // 下拉刷新
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadSavedFeeds() }
        }

        // 适配器
        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) },
            onShareClick = { handleSaveClick(it) }, // 收藏列表点击=取消收藏
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SavesFeedsFragment.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * 加载收藏列表 - 从MMKV获取，同步allFeeds最新状态（评论数/点赞状态）
     */
    private fun loadSavedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取MMKV中的收藏列表和所有动态（用于同步最新状态）
                val mmkvSavedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds()
                val likedFeeds = MMKVManager.getLikedFeeds()

                // 2. 同步状态：用allFeeds的最新数据更新收藏列表
                val syncedSavedFeeds = mmkvSavedFeeds.mapNotNull { savedFeed ->
                    // 从allFeeds中找对应动态（确保评论数/基础信息最新）
                    val latestFeed = allFeeds.firstOrNull { it.feedId == savedFeed.feedId } ?: savedFeed
                    // 补充点赞状态（从likedFeeds获取）
                    latestFeed.copy(
                        isLiked = likedFeeds.any { it.feedId == latestFeed.feedId },
                        commentCount = MMKVManager.getCommentsByFeedId(latestFeed.feedId).size
                    )
                }.sortedByDescending { it.createTime }.toMutableList() // 按创建时间倒序

                // 3. 主线程更新UI
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    savedFeedList = syncedSavedFeeds
                    adapter.submitList(savedFeedList)
                    // 空列表提示
                    binding.tvEmpty.visibility = if (syncedSavedFeeds.isEmpty()) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                Log.e("SavesFeeds", "加载收藏列表失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "加载收藏列表失败", Toast.LENGTH_SHORT).show()
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
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

    /**
     * 处理点赞点击 - 同步MMKV的likedFeeds和allFeeds
     */
    private fun handleLikeClick(position: Int) {
        if (position >= savedFeedList.size || !isAdded) return
        val targetFeed = savedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取当前点赞状态和MMKV中的点赞列表
                val isCurrentlyLiked = targetFeed.isLiked
                val currentLikedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()

                // 2. 更新点赞列表
                if (!isCurrentlyLiked) {
                    // 点赞：加入列表（去重）
                    if (!currentLikedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentLikedFeeds.add(targetFeed)
                    }
                } else {
                    // 取消点赞：移除列表
                    currentLikedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }

                // 3. 更新allFeeds中对应动态的点赞状态
                val feedIndexInAll = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
                if (feedIndexInAll != -1) {
                    val updatedFeed = allFeeds[feedIndexInAll].copy(
                        isLiked = !isCurrentlyLiked,
                        likeCount = if (!isCurrentlyLiked) {
                            allFeeds[feedIndexInAll].likeCount + 1
                        } else {
                            allFeeds[feedIndexInAll].likeCount - 1
                        }
                    )
                    allFeeds[feedIndexInAll] = updatedFeed
                    // 同步更新内存列表
                    savedFeedList[position] = updatedFeed
                }

                // 4. 保存到MMKV
                MMKVManager.saveLikedFeeds(currentLikedFeeds)
                MMKVManager.saveAllFeeds(allFeeds)

                // 5. 主线程更新UI
                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        if (!isCurrentlyLiked) "点赞成功" else "取消点赞",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("SavesFeeds", "点赞操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理取消收藏 - 从MMKV的savedFeeds移除，同步allFeeds
     */
    private fun handleSaveClick(position: Int) {
        if (position >= savedFeedList.size || !isAdded) return
        val targetFeed = savedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取MMKV中的收藏列表和所有动态
                val currentSavedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()

                // 2. 从收藏列表移除当前动态
                currentSavedFeeds.removeAll { it.feedId == targetFeed.feedId }

                // 3. 更新allFeeds中对应动态的收藏状态
                val feedIndexInAll = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
                if (feedIndexInAll != -1) {
                    val updatedFeed = allFeeds[feedIndexInAll].copy(
                        isSaved = false,
                        shareCount = allFeeds[feedIndexInAll].shareCount - 1
                    )
                    allFeeds[feedIndexInAll] = updatedFeed
                }

                // 4. 保存到MMKV（持久化）
                MMKVManager.saveSavedFeeds(currentSavedFeeds)
                MMKVManager.saveAllFeeds(allFeeds)

                // 5. 主线程更新UI：移除列表项 + 刷新
                withContext(Dispatchers.Main) {
                    val newSavedList = savedFeedList.toMutableList()
                    newSavedList.removeAt(position)
                    savedFeedList = newSavedList
                    adapter.submitList(savedFeedList)
                    // 空列表提示更新
                    binding.tvEmpty.visibility = if (newSavedList.isEmpty()) View.VISIBLE else View.GONE
                    Toast.makeText(requireContext(), "取消收藏成功", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("SavesFeeds", "取消收藏失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "取消收藏失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理评论点击 - 跳转评论页
     */
    private fun handleCommentClick(position: Int) {
        if (position >= savedFeedList.size || !isAdded) return
        val targetFeed = savedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    /**
     * 处理头像点击
     */
    private fun handleAvatarClick(position: Int) {
        if (position < savedFeedList.size && isAdded) {
            val username = savedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 评论返回后 - 刷新列表
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            loadSavedFeeds()
        }
    }

    /**
     * 页面重新可见时 - 刷新数据
     */
    override fun onResume() {
        super.onResume()
        loadSavedFeeds()
    }
}