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
import com.zg.carbonapp.databinding.FragmentLikedFeedsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikedFeedsFragment : Fragment() {
    private lateinit var binding: FragmentLikedFeedsBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var likedFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002

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
        loadLikedFeeds() // 初始化加载点赞列表
    }

    /**
     * 初始化下拉刷新和RecyclerView
     */
    private fun setupRefreshAndRecycler() {
        // 下拉刷新
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadLikedFeeds() }
        }

        // 适配器
        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) }, // 点赞列表点击=取消点赞
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

    /**
     * 加载点赞列表 - 从MMKV获取，同步allFeeds最新状态（评论数/收藏状态）
     */
    private fun loadLikedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取MMKV中的点赞列表和所有动态（用于同步最新状态）
                val mmkvLikedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds()
                val savedFeeds = MMKVManager.getSavedFeeds()

                // 2. 同步状态：用allFeeds的最新数据更新点赞列表
                val syncedLikedFeeds = mmkvLikedFeeds.mapNotNull { likedFeed ->
                    // 从allFeeds中找对应动态（确保评论数/基础信息最新）
                    val latestFeed = allFeeds.firstOrNull { it.feedId == likedFeed.feedId } ?: likedFeed
                    // 补充收藏状态（从savedFeeds获取）
                    latestFeed.copy(
                        isSaved = savedFeeds.any { it.feedId == latestFeed.feedId },
                        commentCount = MMKVManager.getCommentsByFeedId(latestFeed.feedId).size
                    )
                }.sortedByDescending { it.createTime }.toMutableList() // 按创建时间倒序

                // 3. 主线程更新UI
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    likedFeedList = syncedLikedFeeds
                    adapter.submitList(likedFeedList)
                    // 空列表提示
                    binding.tvEmpty.visibility = if (syncedLikedFeeds.isEmpty()) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                Log.e("LikedFeeds", "加载点赞列表失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "加载点赞列表失败", Toast.LENGTH_SHORT).show()
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
     * 处理取消点赞 - 从MMKV的likedFeeds移除，同步allFeeds
     */
    private fun handleLikeClick(position: Int) {
        if (position >= likedFeedList.size || !isAdded) return
        val targetFeed = likedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取MMKV中的点赞列表和所有动态
                val currentLikedFeeds = MMKVManager.getLikedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()

                // 2. 从点赞列表移除当前动态
                currentLikedFeeds.removeAll { it.feedId == targetFeed.feedId }

                // 3. 更新allFeeds中对应动态的点赞状态和计数
                val feedIndexInAll = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
                if (feedIndexInAll != -1) {
                    val updatedFeed = allFeeds[feedIndexInAll].copy(
                        isLiked = false,
                        likeCount = allFeeds[feedIndexInAll].likeCount - 1
                    )
                    allFeeds[feedIndexInAll] = updatedFeed
                }

                // 4. 保存到MMKV（持久化）
                MMKVManager.saveLikedFeeds(currentLikedFeeds)
                MMKVManager.saveAllFeeds(allFeeds)

                // 5. 主线程更新UI：移除列表项 + 刷新
                withContext(Dispatchers.Main) {
                    val newLikedList = likedFeedList.toMutableList()
                    newLikedList.removeAt(position)
                    likedFeedList = newLikedList
                    adapter.submitList(likedFeedList)
                    // 空列表提示更新
                    binding.tvEmpty.visibility = if (newLikedList.isEmpty()) View.VISIBLE else View.GONE
                    Toast.makeText(requireContext(), "取消点赞成功", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("LikedFeeds", "取消点赞失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "取消点赞失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理收藏点击 - 同步MMKV的savedFeeds和allFeeds
     */
    private fun handleSaveClick(position: Int) {
        if (position >= likedFeedList.size || !isAdded) return
        val targetFeed = likedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取当前收藏状态和MMKV中的收藏列表
                val isCurrentlySaved = targetFeed.isSaved
                val currentSavedFeeds = MMKVManager.getSavedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds().toMutableList()

                // 2. 更新收藏列表
                if (!isCurrentlySaved) {
                    // 收藏：加入列表（去重）
                    if (!currentSavedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentSavedFeeds.add(targetFeed)
                    }
                } else {
                    // 取消收藏：移除列表
                    currentSavedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }

                // 3. 更新allFeeds中对应动态的收藏状态
                val feedIndexInAll = allFeeds.indexOfFirst { it.feedId == targetFeed.feedId }
                if (feedIndexInAll != -1) {
                    val updatedFeed = allFeeds[feedIndexInAll].copy(
                        isSaved = !isCurrentlySaved,
                        shareCount = if (!isCurrentlySaved) {
                            allFeeds[feedIndexInAll].shareCount + 1
                        } else {
                            allFeeds[feedIndexInAll].shareCount - 1
                        }
                    )
                    allFeeds[feedIndexInAll] = updatedFeed
                    // 同步更新内存列表
                    likedFeedList[position] = updatedFeed
                }

                // 4. 保存到MMKV
                MMKVManager.saveSavedFeeds(currentSavedFeeds)
                MMKVManager.saveAllFeeds(allFeeds)

                // 5. 主线程更新UI
                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        if (!isCurrentlySaved) "收藏成功" else "取消收藏",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("LikedFeeds", "收藏操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "收藏操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理评论点击 - 跳转评论页
     */
    private fun handleCommentClick(position: Int) {
        if (position >= likedFeedList.size || !isAdded) return
        val targetFeed = likedFeedList[position]

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
        if (position < likedFeedList.size && isAdded) {
            val username = likedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 评论返回后 - 刷新列表（评论数可能变化）
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            loadLikedFeeds()
        }
    }

    /**
     * 页面重新可见时 - 刷新数据
     */
    override fun onResume() {
        super.onResume()
        loadLikedFeeds()
    }
}