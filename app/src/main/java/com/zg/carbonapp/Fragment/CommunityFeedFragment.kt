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
import com.zg.carbonapp.Tool.toUserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.TokenManager
import com.zg.carbonapp.Service.RetrofitClient
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
        //loadAllFeeds()
    }

    private fun initSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadAllFeeds() }
        }
    }

    private fun initPublishButton() {
        binding.fabPost.setOnClickListener {
            if (!TokenManager.isLoggedIn()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivityForResult(Intent(requireContext(), PostFeedActivity::class.java), PUBLISH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PUBLISH_REQUEST_CODE && resultCode == RESULT_OK) {
            loadAllFeeds()
        }
    }

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

    private fun loadAllFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getAllDynamics()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200) {
                        val dynamics = apiResponse.data
                        // 转换为UserFeed列表
                        val feeds = dynamics.map { it.toUserFeed() }

                        // 保存到本地缓存
                        MMKVManager.saveAllFeeds(feeds)

                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            allFeedList = feeds.toMutableList()
                            adapter.submitList(allFeedList)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "获取动态失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "网络请求失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CommunityFeed", "加载失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) Toast.makeText(requireContext(), "加载动态失败", Toast.LENGTH_SHORT).show()
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
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]
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
                        // 更新本地状态
                        allFeedList[position] = targetFeed.copy(
                            isLiked = newLikeState,
                            likeCount = targetFeed.likeCount + if (newLikeState) 1 else -1
                        )
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
                    Log.e("CommunityFeed", "点赞失败", e)
                }
            }
        }
    }

    private fun handleSaveClick(position: Int) {
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]
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
                        // 更新本地状态
                        allFeedList[position] = targetFeed.copy(
                            isSaved = newSaveState,
                            shareCount = targetFeed.shareCount + if (newSaveState) 1 else -1
                        )
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
                    Log.e("CommunityFeed", "收藏失败", e)
                }
            }
        }
    }

    private fun handleCommentClick(position: Int) {
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]

        if (!TokenManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 跳转评论页
        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    private fun handleAvatarClick(position: Int) {
        if (position < allFeedList.size && isAdded) {
            val username = allFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }
}