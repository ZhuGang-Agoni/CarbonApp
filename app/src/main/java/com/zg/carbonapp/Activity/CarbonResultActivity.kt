package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.AlternativeAdapter
import com.zg.carbonapp.DB.ProductCarbonDB
import com.zg.carbonapp.Dao.Product
import com.zg.carbonapp.databinding.ActivityCarbonResultBinding

class CarbonResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarbonResultBinding
    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取商品数据（修复空数据判断）
        product = intent.getParcelableExtra("product") ?: run {
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 显示商品信息
        binding.tvProductName.text = product.name
        binding.tvProductCategory.text = "类别：${getCategoryName(product.category)}"
        binding.tvCarbonFootprint.text = "碳足迹：${product.carbonFootprint} kgCO₂e/${product.unit}"

        // 加载低碳替代品
        loadAlternatives()
    }

    // 类别转换（修复潜在空值）
    private fun getCategoryName(category: String): String {
        return when(category) {
            "dairy" -> "乳制品"
            "tissue" -> "纸巾"
            "beverage" -> "饮料"
            else -> "其他（$category）" // 显示未知类别，方便调试
        }
    }

    // 加载替代品（修复适配器绑定）
    private fun loadAlternatives() {
        val alternatives = ProductCarbonDB.getLowCarbonAlternatives(product.category)
        binding.rvAlternatives.apply {
            layoutManager = LinearLayoutManager(this@CarbonResultActivity)
            adapter = AlternativeAdapter(alternatives, product.carbonFootprint)
        }
    }
}