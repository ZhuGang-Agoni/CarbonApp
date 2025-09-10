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
import com.zg.carbonapp.Tool.toUserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.Service.RetrofitClient
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
        //loadCommentedFeeds()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val feedId = data?.getStringExtra("feedId")

            lifecycleScope.launch(Dispatchers.IO) {
                val allFeeds = MMKVManager.getAllFeeds()
                val updatedFeed = allFeeds.firstOrNull { it.feedId == feedId }

                withContext(Dispatchers.Main) {
                    updatedFeed?.let { updateLocalFeed(it) }
                    loadCommentedFeeds()
                }
            }
        }
    }

    private fun updateLocalFeed(updatedFeed: UserFeed) {
        val targetIndex = commentedFeedList.indexOfFirst { it.feedId == updatedFeed.feedId }
        if (targetIndex != -1) {
            commentedFeedList[targetIndex] = updatedFeed
            adapter.notifyItemChanged(targetIndex)
        }
    }

    private fun setupRefreshAndRecycler() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light
            )
            setOnRefreshListener { loadCommentedFeeds() }
        }

        adapter = CommunityFeedAdapter(
            onLikeClick = { handleLikeClick(it) },
            onCommentClick = { handleCommentClick(it) },
            onShareClick = { handleSaveClick(it) },
            onAvatarClick = { handleAvatarClick(it) }
        )

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CommentedFeedsFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadCommentedFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")

                // 获取用户评论列表
                val commentResponse = RetrofitClient.instance.getCommentListByUser("Bearer $token")
                if (commentResponse.isSuccessful && commentResponse.body()?.code == 200) {
                    val comments = commentResponse.body()?.data ?: emptyList()

                    // 提取不重复的动态ID
                    val feedIds = comments.map { it.feedId }.distinct()

                    // 获取这些动态的详情
                    val feeds = mutableListOf<UserFeed>()
                    for (feedId in feedIds) {
                        val dynamicResponse = RetrofitClient.instance.getDynamicDetail(feedId)
                        if (dynamicResponse.isSuccessful && dynamicResponse.body()?.code == 200) {
                            val dynamic = dynamicResponse.body()?.data?.dynamic
                            dynamic?.let {
                                feeds.add(it.toUserFeed().copy(isCommented = true))
                            }
                        }
                    }

                    // 保存到本地缓存
                    MMKVManager.saveCommentedFeeds(feeds)

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        commentedFeedList = feeds.toMutableList()
                        adapter.submitList(commentedFeedList)

                        // 空列表提示
                        if (feeds.isEmpty()) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            binding.tvEmpty.visibility = View.GONE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "获取评论列表失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CommentedFeeds", "加载失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "加载评论动态失败", Toast.LENGTH_SHORT).show()
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

    private fun handleLikeClick(position: Int) {
        if (position >= commentedFeedList.size) return
        val targetFeed = commentedFeedList[position]
        val newLikeState = !targetFeed.isLiked

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")

                // 调用点赞接口
                val response = RetrofitClient.instance.likeDynamic(
                    "Bearer $token",
                    targetFeed.feedId
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        adapter.notifyItemChanged(position)
                        Toast.makeText(
                            requireContext(),
                            if (newLikeState) "点赞成功" else "取消点赞",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "操作失败: ${response.body()?.message ?: "未知错误"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "操作失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("CommentedFeeds", "点赞失败", e)
                }
            }
        }
    }

    private fun handleSaveClick(position: Int) {
        if (position >= commentedFeedList.size) return
        val targetFeed = commentedFeedList[position]
        val newSaveState = !targetFeed.isSaved

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken() ?: throw Exception("用户未登录")

                // 调用收藏接口
                val response = RetrofitClient.instance.collectDynamic(
                    "Bearer $token",
                    targetFeed.feedId
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        adapter.notifyItemChanged(position)
                        Toast.makeText(
                            requireContext(),
                            if (newSaveState) "收藏成功" else "取消收藏",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "操作失败: ${response.body()?.message ?: "未知错误"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "操作失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("CommentedFeeds", "收藏失败", e)
                }
            }
        }
    }

    private fun handleCommentClick(position: Int) {
        if (position >= commentedFeedList.size) return
        val targetFeed = commentedFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    private fun handleAvatarClick(position: Int) {
        if (position < commentedFeedList.size && isAdded) {
            val username = commentedFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }
}