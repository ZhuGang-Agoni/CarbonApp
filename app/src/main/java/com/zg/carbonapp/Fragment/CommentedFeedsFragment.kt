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
import com.zg.carbonapp.databinding.FragmentCommentedFeedsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentedFeedsFragment : Fragment() {
    private lateinit var binding: FragmentCommentedFeedsBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var commentedFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentedFeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRefreshAndRecycler()
        loadCommentedFeeds() // 初始化加载评论过的列表
    }

    /**
     * 初始化下拉刷新和RecyclerView
     */
    private fun setupRefreshAndRecycler() {
        // 下拉刷新
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadCommentedFeeds() }
        }

        // 适配器
        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) }, // 跳转评论页（带已有评论）
            onShareClick = { handleSaveClick(it) },
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommentedFeedsFragment.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * 加载评论过的列表 - 从MMKV获取（MMKVManager自动维护commentedFeeds）
     */
    private fun loadCommentedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取MMKV中的评论过列表、所有动态、点赞/收藏列表（同步状态）
                val mmkvCommentedFeeds = MMKVManager.getCommentedFeeds().toMutableList()
                val allFeeds = MMKVManager.getAllFeeds()
                val likedFeeds = MMKVManager.getLikedFeeds()
                val savedFeeds = MMKVManager.getSavedFeeds()

                // 2. 同步状态：用allFeeds的最新数据更新评论过列表
                val syncedCommentedFeeds = mmkvCommentedFeeds.mapNotNull { commentedFeed ->
                    // 从allFeeds中找对应动态（确保评论数/基础信息最新）
                    val latestFeed = allFeeds.firstOrNull { it.feedId == commentedFeed.feedId } ?: commentedFeed
                    // 补充点赞/收藏状态
                    latestFeed.copy(
                        isLiked = likedFeeds.any { it.feedId == latestFeed.feedId },
                        isSaved = savedFeeds.any { it.feedId == latestFeed.feedId },
                        commentCount = MMKVManager.getCommentsByFeedId(latestFeed.feedId).size
                    )
                }.sortedByDescending { it.commentCount }.toMutableList() // 按评论数倒序（评论多的在前）

                // 3. 主线程更新UI
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    commentedFeedList = syncedCommentedFeeds
                    adapter.submitList(commentedFeedList)
                    // 空列表提示
                    binding.tvEmpty.visibility = if (syncedCommentedFeeds.isEmpty()) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                Log.e("CommentedFeeds", "加载评论列表失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "加载评论列表失败", Toast.LENGTH_SHORT).show()
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
        if (position >= commentedFeedList.size || !isAdded) return
        val targetFeed = commentedFeedList[position]

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
                    if (!currentLikedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentLikedFeeds.add(targetFeed)
                    }
                } else {
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
                    commentedFeedList[position] = updatedFeed
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
                Log.e("CommentedFeeds", "点赞操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理收藏点击 - 同步MMKV的savedFeeds和allFeeds
     */
    private fun handleSaveClick(position: Int) {
        if (position >= commentedFeedList.size || !isAdded) return
        val targetFeed = commentedFeedList[position]

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
                    if (!currentSavedFeeds.any { it.feedId == targetFeed.feedId }) {
                        currentSavedFeeds.add(targetFeed)
                    }
                } else {
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
                    commentedFeedList[position] = updatedFeed
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
                Log.e("CommentedFeeds", "收藏操作失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "收藏操作失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 处理评论点击 - 跳转评论页（携带已有评论）
     */
    private fun handleCommentClick(position: Int) {
        if (position >= commentedFeedList.size || !isAdded) return
        val targetFeed = commentedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 从MMKV获取该动态的已有评论（传给评论页）
        val existingComments = MMKVManager.getCommentsByFeedId(targetFeed.feedId)
        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        intent.putExtra("existingCommentCount", existingComments.size)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    /**
     * 处理头像点击
     */
    private fun handleAvatarClick(position: Int) {
        if (position < commentedFeedList.size && isAdded) {
            val username = commentedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 评论返回后 - 刷新列表（评论可能新增/删除）
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            loadCommentedFeeds()
        }
    }

    /**
     * 页面重新可见时 - 刷新数据
     */
    override fun onResume() {
        super.onResume()
        loadCommentedFeeds()
    }
}