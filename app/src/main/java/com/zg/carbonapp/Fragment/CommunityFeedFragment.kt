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
import com.zg.carbonapp.Activity.PostFeedActivity
import com.zg.carbonapp.Activity.UserCommentActivity
import com.zg.carbonapp.Adapter.CommunityFeedAdapter
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.databinding.FragmentCommunityFeedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommunityFeedFragment : Fragment() {
    private lateinit var binding: FragmentCommunityFeedBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var allFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002
    private val PUBLISH_REQUEST_CODE = 1001

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
        initSwipeRefresh()
        initPublishButton()
        initRecyclerView()
        loadAllFeeds() // 初始化加载所有动态
    }

    /**
     * 初始化下拉刷新 - 刷新时重新加载所有动态（含最新状态）
     */
    private fun initSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadAllFeeds() }
        }
    }

    /**
     * 初始化发布按钮 - 发布后刷新列表
     */
    private fun initPublishButton() {
        binding.fabPost.setOnClickListener {
            if (!TokenManager.isLoggedIn()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivityForResult(
                Intent(requireContext(), PostFeedActivity::class.java),
                PUBLISH_REQUEST_CODE
            )
        }
    }

    /**
     * 初始化RecyclerView - 绑定适配器和点击事件
     */
    private fun initRecyclerView() {
        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) },
            onShareClick = { handleSaveClick(it) },
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommunityFeedFragment.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * 核心：加载所有动态 + 合并最新状态（点赞/收藏/评论数）
     * 从MMKV的4个列表中同步数据：allFeeds + likedFeeds + savedFeeds + comments
     */
    private fun loadAllFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取基础数据：所有动态、点赞列表、收藏列表
                val baseAllFeeds = MMKVManager.getAllFeeds().toMutableList()
                val likedFeeds = MMKVManager.getLikedFeeds()
                val savedFeeds = MMKVManager.getSavedFeeds()

                // 2. 合并状态：给每个动态设置最新的点赞/收藏/评论数
                val mergedFeeds = baseAllFeeds.map { feed ->
                    // 点赞状态：判断当前动态是否在点赞列表中
                    val isLiked = likedFeeds.any { it.feedId == feed.feedId }
                    // 收藏状态：判断当前动态是否在收藏列表中
                    val isSaved = savedFeeds.any { it.feedId == feed.feedId }
                    // 评论数：从MMKV获取该动态的评论列表大小
                    val commentCount = MMKVManager.getCommentsByFeedId(feed.feedId).size

                    // 构建合并后的动态（保留原数据，更新状态字段）
                    feed.copy(
                        isLiked = isLiked,
                        isSaved = isSaved,
                        commentCount = commentCount
                    )
                }.sortedByDescending { it.createTime }.toMutableList() // 按创建时间倒序（最新在前）

                // 3. 主线程更新UI
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    allFeedList = mergedFeeds
                    adapter.submitList(allFeedList)
                }

            } catch (e: Exception) {
                Log.e("CommunityFeed", "加载动态失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "加载动态失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }

            } finally {
                // 无论成功失败，关闭刷新动画
                withContext(Dispatchers.Main) {
                    if (isAdded && binding.swipeRefresh.isRefreshing) {
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    /**
     * 处理点赞点击 - 同步更新MMKV的likedFeeds和allFeeds
     */
    private fun handleLikeClick(position: Int) {
        if (position >= allFeedList.size || !isAdded) return
        val targetFeed = allFeedList[position]

        // 未登录拦截
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

                // 2. 更新点赞列表：点赞则添加，取消则移除
                if (!isCurrentlyLiked) {
                    // 点赞：将当前动态加入点赞列表（去重）
                    if (!currentLikedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentLikedFeeds.add(targetFeed)
                    }
                } else {
                    // 取消点赞：从点赞列表移除当前动态
                    currentLikedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }

                // 3. 更新allFeeds中对应动态的点赞状态和计数
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
                    // 同步更新内存列表（用于UI刷新）
                    allFeedList[position] = updatedFeed
                }

                // 4. 保存到MMKV（持久化）
                MMKVManager.saveLikedFeeds(currentLikedFeeds)
                MMKVManager.saveAllFeeds(allFeeds)

                // 5. 主线程更新UI
                withContext(Dispatchers.Main) {
                    adapter.notifyItemChanged(position) // 局部刷新当前item
                    Toast.makeText(
                        requireContext(),
                        if (!isCurrentlyLiked) "点赞成功" else "取消点赞",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("CommunityFeed", "点赞操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理收藏点击 - 同步更新MMKV的savedFeeds和allFeeds
     */
    private fun handleSaveClick(position: Int) {
        if (position >= allFeedList.size || !isAdded) return
        val targetFeed = allFeedList[position]

        // 未登录拦截
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

                // 2. 更新收藏列表：收藏则添加，取消则移除
                if (!isCurrentlySaved) {
                    // 收藏：加入收藏列表（去重）
                    if (!currentSavedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentSavedFeeds.add(targetFeed)
                    }
                } else {
                    // 取消收藏：从收藏列表移除
                    currentSavedFeeds.removeAll { it.feedId == targetFeed.feedId }
                }

                // 3. 更新allFeeds中对应动态的收藏状态和计数
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
                    allFeedList[position] = updatedFeed
                }

                // 4. 保存到MMKV（持久化）
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
                Log.e("CommunityFeed", "收藏操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "收藏操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理评论点击 - 跳转评论页，返回后刷新列表（评论数可能变化）
     */
    private fun handleCommentClick(position: Int) {
        if (position >= allFeedList.size || !isAdded) return
        val targetFeed = allFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 携带动态ID和当前评论数跳转
        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        intent.putExtra("currentCommentCount", targetFeed.commentCount)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    /**
     * 处理头像点击 - 示例：查看用户资料（可后续扩展）
     */
    private fun handleAvatarClick(position: Int) {
        if (position < allFeedList.size && isAdded) {
            val username = allFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 发布/评论返回后 - 重新加载数据（确保状态同步）
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == PUBLISH_REQUEST_CODE || requestCode == COMMENT_REQUEST_CODE)
            && resultCode == RESULT_OK
        ) {
            loadAllFeeds()
        }
    }

    /**
     * 页面重新可见时 - 刷新数据（防止其他页面修改后状态不同步）
     */
    override fun onResume() {
        super.onResume()
        loadAllFeeds()
    }
}