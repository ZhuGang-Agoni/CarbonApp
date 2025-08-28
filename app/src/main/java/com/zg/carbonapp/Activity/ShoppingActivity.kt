package com.zg.carbonapp.Activity

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.HotProductAdapter
import com.zg.carbonapp.Adapter.NewProductAdapter
import com.zg.carbonapp.Dao.ProductType
import com.zg.carbonapp.Dao.ShopRecord // 新增：导入ShopRecord
import com.zg.carbonapp.Dao.VirtualProduct
import com.zg.carbonapp.MMKV.ShopRecordMMKV // 新增：导入ShopRecordMMKV
import com.zg.carbonapp.MMKV.UserAssetsManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Repository.VirtualProductRepository
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.databinding.ActivityShoppingBinding

class ShoppingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingBinding
    private lateinit var hotAdapter: HotProductAdapter
    private lateinit var newAdapter: NewProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)

        initViews()
        setupAllListeners()
        loadUserPoints()
    }

    private fun loadUserPoints() {
        UserMMKV.getUser()?.let { user ->
            binding.carbonPoint.text = user.carbonCount.toString()
        }
    }

    private fun initViews() {
        hotAdapter = HotProductAdapter(this, VirtualProductRepository.hotProducts) {
            handleProductExchange(it)
        }
        binding.hotProductsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ShoppingActivity, 2)
            adapter = hotAdapter
            setHasFixedSize(true)
        }

        newAdapter = NewProductAdapter(this, VirtualProductRepository.newProducts) {
            handleProductExchange(it)
        }
        binding.newProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShoppingActivity)
            adapter = newAdapter
            setHasFixedSize(true)
        }
    }

    // 处理商品兑换逻辑（核心修复：添加记录保存）
    private fun handleProductExchange(product: VirtualProduct) {
        val user = UserMMKV.getUser()?: run {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = user.userId?: return

        if (UserAssetsManager.isItemUnlocked(userId, product.id)) {
            Toast.makeText(this, "您已拥有该物品", Toast.LENGTH_SHORT).show()
            return
        }

        if (user.carbonCount < product.points) {
            Toast.makeText(this, "积分不足，快去完成任务吧", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("兑换确认")
            .setMessage("确定要消耗 ${product.points} 积分兑换 ${product.name} 吗？")
            .setPositiveButton("确认") { _, _ ->
                // 1. 更新用户积分
                val updatedUser = user.copy(carbonCount = user.carbonCount - product.points)
                UserMMKV.saveUser(updatedUser)
                binding.carbonPoint.text = updatedUser.carbonCount.toString()

                // 2. 解锁物品
                UserAssetsManager.unlockItem(userId, product.id)
                // 3. 关键修复：立即保存兑换记录（确保记录不丢失）
                val exchangeRecord = ShopRecord(
                    time = System.currentTimeMillis(), // 记录兑换时间（毫秒级）
                    shopName =product.type.toString()+" "+product.name,
                    shopPoint = product.points
                )
                ShopRecordMMKV.saveShopRecord(exchangeRecord) // 调用保存方法

                // 4. 显示动画和提示
                showUnlockAnimation()
                Toast.makeText(this, "成功解锁 ${product.name}！", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showUnlockAnimation() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.unlock_success)
        binding.pointsCard.startAnimation(anim)
    }

    private fun setupAllListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.searchButton.setOnClickListener {
            Toast.makeText(this, "搜索功能开发中", Toast.LENGTH_SHORT).show()
        }

        binding.exchangeHistoryButton.setOnClickListener {
            IntentHelper.goIntent(this, ShopExchangeRecordActivity::class.java)
        }

        binding.earnMoreButton.setOnClickListener {
            finish()
        }

        val categories = listOf(
            binding.categoryAll,
            binding.categoryBadges,
            binding.categoryFrames,
            binding.categoryItems
        )
        categories.forEach { button ->
            button.setOnClickListener {
                categories.forEach { btn ->
                    btn.setBackgroundColor(ContextCompat.getColor(this, R.color.button_background))
                    btn.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
                }
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                button.setTextColor(ContextCompat.getColor(this, R.color.white))

                val filteredList = when (button) {
                    binding.categoryBadges -> VirtualProductRepository.getProductsByType(ProductType.BADGE)
                    binding.categoryFrames -> VirtualProductRepository.getProductsByType(ProductType.AVATAR_FRAME)
                    binding.categoryItems -> VirtualProductRepository.getProductsByType(ProductType.AVATAR_ITEM)
                    else -> VirtualProductRepository.allProducts
                }
                hotAdapter.updateList(filteredList.take(4))
                newAdapter.updateList(filteredList.takeLast(3))
            }
        }
    }
}