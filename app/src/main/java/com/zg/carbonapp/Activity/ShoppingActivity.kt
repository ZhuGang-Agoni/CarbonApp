package com.zg.carbonapp.Activity

// CarbonMallActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.HotProductAdapter
import com.zg.carbonapp.Adapter.NewProductAdapter
import com.zg.carbonapp.Adapter.ShopExchangeRecordAdapter
import com.zg.carbonapp.Dao.ExchangeProduct
import com.zg.carbonapp.Dao.ShopRecord
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.Fragment.MainHomeFragment
import com.zg.carbonapp.MMKV.ShopRecordMMKV
import com.zg.carbonapp.MMKV.TravelRecordManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.IntentHelper
import com.zg.carbonapp.databinding.ActivityShoppingBinding


class ShoppingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShoppingBinding
    // 热门推荐适配器
    private lateinit var hotAdapter: HotProductAdapter
    // 最新上架适配器
    private lateinit var newAdapter: NewProductAdapter

    private val shopRecordAdapter=ShopExchangeRecordAdapter(this, emptyList<ShopRecord>())
    // 热门商品列表
    private val hotProductList = mutableListOf<ExchangeProduct>()
    // 最新上架商品列表
    private val newProductList = mutableListOf<ExchangeProduct>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化商品数据
        initProductData()
        // 初始化视图
        initViews()
        // 设置所有点击事件
        setupAllListeners()
    }

    // 初始化热门和最新商品数据
    private fun initProductData() {
        // 热门推荐商品
        hotProductList.apply {
            add(
                ExchangeProduct(
                    id = 1,
                    name = "环保购物袋",
                    description = "可重复使用，减少塑料污染",
                    points = 250,
                    imageRes = R.drawable.tree
                )
            )
            add(
                ExchangeProduct(
                    id = 2,
                    name = "竹制餐具套装",
                    description = "天然竹制，可降解材质",
                    points = 350,
                    imageRes = R.drawable.tree
                )
            )
            add(
                ExchangeProduct(
                    id = 3,
                    name = "可降解垃圾袋",
                    description = "全降解材料，环保无害",
                    points = 180,
                    imageRes = R.drawable.tree
                )
            )
            add(
                ExchangeProduct(
                    id = 4,
                    name = "太阳能小夜灯",
                    description = "太阳能充电，节能省电",
                    points = 420,
                    imageRes = R.drawable.tree
                )
            )
        }

        // 最新上架商品
        newProductList.apply {
            add(
                ExchangeProduct(
                    id = 101,
                    name = "太阳能充电宝",
                    description = "太阳能充电，环保便携",
                    points = 500,
                    imageRes = R.drawable.tree,
                    exchangeCount = 238
                )
            )
            add(
                ExchangeProduct(
                    id = 102,
                    name = "植树证书",
                    description = "捐赠1棵树，电子证书",
                    points = 1000,
                    imageRes = R.drawable.tree,
                    exchangeCount = 89
                )
            )
            add(
                ExchangeProduct(
                    id = 103,
                    name = "环保洗衣液",
                    description = "无磷配方，生物降解",
                    points = 320,
                    imageRes = R.drawable.tree,
                    exchangeCount = 156
                )
            )
        }
    }

    // 初始化视图（绑定适配器）
    private fun initViews() {

        val user=UserMMKV.getUser()
//         反正就是要及时的更新数据
        if (user!=null){
            binding.carbonPoint.text=user.carbonCount.toString()
        }
        // 热门推荐适配器（网格布局）
        hotAdapter = HotProductAdapter(this, hotProductList) { product ->
            // 热门商品点击事件（示例：跳转详情）
            val needPoint=product.points
            if (needPoint.toInt()<binding.carbonPoint.text.toString().toInt()) {
                AlertDialog.Builder(this)
                    .setTitle("兑换商品")
                    .setMessage("确定要兑换这件商品吗")
                    .setPositiveButton("确定"){_,_->
                         Toast.makeText(this,"兑换成功",Toast.LENGTH_SHORT).show()
                         binding.carbonPoint.text=(binding.carbonPoint.text.toString().toInt()-needPoint).toString()
                         if (user!=null){
                             val newUser= User(
                                 userId = user.userId,
                                 userName = user.userName,
                                 userEvator = user.userEvator,
                                 userPassword = user.userPassword,
                                 userQQ = user.userQQ,
                                 userTelephone=user.userTelephone,
                                 signature=user.signature,
                                 carbonCount = binding.carbonPoint.text.toString().toInt(),
                                 surePassword=user.surePassword
                             )
                             UserMMKV.saveUser(newUser)
//             这里还有一个逻辑是留给后端的 暂时先不管 这个逻辑是根据 积分更新数据
                         }
//                        处理兑换记录的逻辑 要及时的更新 兑换纪录
                    val shopRecord=ShopRecord(
                        System.currentTimeMillis(),
                        product.name.toString(),
                        product.points
                    )
                        ShopRecordMMKV.saveShopRecord(shopRecord)
//                        val list=ShopRecordMMKV.getShopRecordItem()
//                        shopRecordAdapter.updateList(list)

                    }
                    .setNegativeButton("再想想"){_,_->{

                    }}
                    .show()
                Toast.makeText(this, "点击热门商品：${product.name}", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this,"很抱歉，您的积分不足，快去做任务赚取积分吧",Toast.LENGTH_SHORT).show()
            }
        }
        binding.hotProductsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ShoppingActivity, 2) // 2列网格
            adapter = hotAdapter
        }

        // 最新上架适配器（线性布局）
        newAdapter = NewProductAdapter(this, newProductList) { product ->
            // 最新商品点击事件（示例：跳转详情）
            val needPoint=product.points
            if (needPoint.toInt()<binding.carbonPoint.text.toString().toInt()) {
                AlertDialog.Builder(this)
                    .setTitle("兑换商品")
                    .setMessage("确定要兑换这件商品吗")
                    .setPositiveButton("确定") { _, _ ->
                        Toast.makeText(this, "兑换成功", Toast.LENGTH_SHORT).show()
                        binding.carbonPoint.text =
                            (binding.carbonPoint.text.toString().toInt() - needPoint).toString()
                        if (user != null) {
                            val newUser = User(
                                userId = user.userId,
                                userName = user.userName,
                                userEvator = user.userEvator,
                                userPassword = user.userPassword,
                                userQQ = user.userQQ,
                                userTelephone = user.userTelephone,
                                signature = user.signature,
                                carbonCount = binding.carbonPoint.text.toString().toInt(),
                                surePassword = user.surePassword
                            )
                            UserMMKV.saveUser(newUser)
// 更新 Travelrecord
//             这里还有一个逻辑是留给后端的 暂时先不管 这个逻辑是根据 积分更新数据
                            }
//                        处理兑换记录的逻辑 要及时的更新 兑换纪录
                            val shopRecord = ShopRecord(
                                System.currentTimeMillis(),
                                product.name.toString(),
                                product.points
                            )
                            ShopRecordMMKV.saveShopRecord(shopRecord)
//                        val list=ShopRecordMMKV.getShopRecordItem()
//                        shopRecordAdapter.updateList(list)

                        }

                    .setNegativeButton("再想想"){_,_->{

                    }}
                    .show()
                Toast.makeText(this, "点击最新商品：${product.name}", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this,"很抱歉，您的积分不足，快去做任务赚取积分吧",Toast.LENGTH_SHORT).show()
            }
        }
        binding.newProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShoppingActivity) // 线性布局
            adapter = newAdapter
        }
    }

    // 设置所有点击事件（完全对应布局ID）
    private fun setupAllListeners() {
        binding.exchangeHistoryButton.setOnClickListener{
             IntentHelper.goIntent(this,ShopExchangeRecordActivity::class.java)
        }
//        获取更多的碳积分
        binding.earnMoreButton.setOnClickListener{
            IntentHelper.goIntent(this,MainHomeFragment::class.java)
        }
        // 顶部导航栏点击
        binding.backButton.setOnClickListener { finish() } // 返回
        binding.searchButton.setOnClickListener {
            Toast.makeText(this, "搜索功能", Toast.LENGTH_SHORT).show()
        }


        // 分类导航点击（切换选中状态）
        val categoryButtons = listOf(
            binding.categoryAll,
            binding.categoryEco,
            binding.categoryPlant,
            binding.categoryExperience,
            binding.categoryService
        )
        categoryButtons.forEach { btn ->
            btn.setOnClickListener {
                // 重置所有按钮样式
                categoryButtons.forEach {
                    it.backgroundTintList = getColorStateList(R.color.button_background)
                    it.setTextColor(getColorStateList(R.color.primary_text))
                }
                // 设置当前按钮为选中样式
                btn.backgroundTintList = getColorStateList(R.color.primary)
                btn.setTextColor(getColorStateList(R.color.white))
                // 实际项目中可添加分类筛选逻辑
            }
        }
    }
}