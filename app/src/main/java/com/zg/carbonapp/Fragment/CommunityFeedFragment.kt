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
import com.zg.carbonapp.Activity.PostFeedActivity
import com.zg.carbonapp.Activity.UserCommentActivity
import com.zg.carbonapp.Adapter.CommunityFeedAdapter
import com.zg.carbonapp.Dao.UserFeed
import com.zg.carbonapp.MMKV.MMKVManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.databinding.FragmentCommunityFeedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommunityFeedFragment : Fragment() {
    private lateinit var binding: FragmentCommunityFeedBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var allFeedList: MutableList<UserFeed> = mutableListOf()
    private val COMMENT_REQUEST_CODE = 1002  // 评论页请求码
    private val PUBLISH_REQUEST_CODE = 1001   // 发布页请求码

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
        loadAllFeeds()
    }

    // 初始化下拉刷新
    private fun initSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_green_light)
            setOnRefreshListener { loadAllFeeds() }
        }
    }

    // 初始化发布按钮（登录校验）
    private fun initPublishButton() {
        binding.fabPost.setOnClickListener {
            val currentUser = UserMMKV.getUser()
            if (currentUser == null) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivityForResult(Intent(requireContext(), PostFeedActivity::class.java), PUBLISH_REQUEST_CODE)
        }
    }

    // 关键：接收评论页回传的最新评论数，实时同步
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PUBLISH_REQUEST_CODE && resultCode == RESULT_OK) {
            loadAllFeeds()  // 发布动态后全量刷新
        } else if (requestCode == COMMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val feedId = data?.getStringExtra("feedId")
            val newCommentCount = data?.getIntExtra("newCommentCount", 0) ?: 0
            // 1. 实时更新当前列表评论数（局部刷新，体验更好）
            if (!feedId.isNullOrEmpty() && newCommentCount >= 0) {
                updateLocalFeedCommentCount(feedId, newCommentCount)
            }
            // 2. 全量刷新兜底（避免极端数据不一致）
            loadAllFeeds()
        }
    }

    // 实时更新本地列表的评论数（局部刷新）
    private fun updateLocalFeedCommentCount(feedId: String, newCommentCount: Int) {
        val targetIndex = allFeedList.indexOfFirst { it.feedId == feedId }
        if (targetIndex != -1) {
            // 复制对象更新（避免直接修改原数据）
            allFeedList[targetIndex] = allFeedList[targetIndex].copy(
                commentCount = newCommentCount
            )
            adapter.notifyItemChanged(targetIndex)  // 只刷新当前项，性能更优
        }
    }

    // 初始化RecyclerView
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

    // 加载全量最新动态（数据来源基准）
    private fun loadAllFeeds() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val savedFeeds = MMKVManager.getAllFeeds().toMutableList()
                savedFeeds.sortByDescending { it.createTime }  // 按发布时间倒序

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    allFeedList = savedFeeds
                    adapter.submitList(allFeedList)
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

    // 点赞逻辑（用feedId精准匹配，避免content+time误判）
    private fun handleLikeClick(position: Int) {
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]
        val newLikeState = !targetFeed.isLiked
        val currentUser = UserMMKV.getUser()

        // 登录校验
        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. 更新当前列表状态
            allFeedList[position] = targetFeed.copy(
                isLiked = newLikeState,
                likeCount = targetFeed.likeCount + if (newLikeState) 1 else -1
            )
            // 2. 同步到全量数据MMKV
            MMKVManager.saveAllFeeds(allFeedList)

            // 3. 更新点赞列表（用feedId匹配）
            val likedFeeds = MMKVManager.getLikedFeeds().toMutableList()
            if (newLikeState) {
                if (!likedFeeds.any { it.feedId == targetFeed.feedId }) {
                    likedFeeds.add(allFeedList[position])
                }
            } else {
                likedFeeds.removeAll { it.feedId == targetFeed.feedId }
            }
            MMKVManager.saveLikedFeeds(likedFeeds)

            // 4. 刷新UI
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

    // 收藏逻辑（feedId精准匹配）
    private fun handleSaveClick(position: Int) {
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]
        val newSaveState = !targetFeed.isSaved
        val currentUser = UserMMKV.getUser()

        // 登录校验
        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. 更新当前列表状态
            allFeedList[position] = targetFeed.copy(
                isSaved = newSaveState,
                shareCount = targetFeed.shareCount + if (newSaveState) 1 else -1
            )
            // 2. 同步到全量数据MMKV
            MMKVManager.saveAllFeeds(allFeedList)

            // 3. 更新收藏列表（feedId匹配）
            val savedFeeds = MMKVManager.getSavedFeeds().toMutableList()
            if (newSaveState) {
                if (!savedFeeds.any { it.feedId == targetFeed.feedId }) {
                    savedFeeds.add(allFeedList[position])
                }
            } else {
                savedFeeds.removeAll { it.feedId == targetFeed.feedId }
            }
            MMKVManager.saveSavedFeeds(savedFeeds)

            // 4. 刷新UI
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

    // 评论点击（跳转评论页，传递feedId）
    private fun handleCommentClick(position: Int) {
        if (position >= allFeedList.size) return
        val targetFeed = allFeedList[position]
        val currentUser = UserMMKV.getUser()

        // 登录校验
        if (currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 跳转评论页，携带动态唯一ID
        val intent = Intent(requireContext(), UserCommentActivity::class.java)
        intent.putExtra("feedId", targetFeed.feedId)
        startActivityForResult(intent, COMMENT_REQUEST_CODE)
    }

    // 头像点击（示例：查看用户资料，可后续扩展）
    private fun handleAvatarClick(position: Int) {
        if (position < allFeedList.size && isAdded) {
            val username = allFeedList[position].username
            Toast.makeText(requireContext(), "查看${username}的资料", Toast.LENGTH_SHORT).show()
        }
    }
}