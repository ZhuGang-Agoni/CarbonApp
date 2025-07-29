package com.zg.carbonapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.databinding.ActivityManualInputBinding
import com.zg.carbonapp.Repository.CarbonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManualInputBinding
    private val repository by lazy { CarbonRepository() }
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSearch.setOnClickListener {
            val productName = binding.etProductName.text.toString().trim()
            if (productName.isEmpty()) {
                Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 隐藏键盘
            hideKeyboard()
            searchProductCarbon(productName)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etProductName.windowToken, 0)
    }

    private fun searchProductCarbon(name: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
//            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.btnSearch.isEnabled = false

            try {
                val carbon = withContext(Dispatchers.IO) {
                    repository.searchCarbonFootprint(name)
                }

                if (carbon != null) {
                    navigateToResult(carbon)
                } else {
                    Toast.makeText(
                        this@ManualInputActivity,
                        "未找到该商品的碳足迹数据",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ManualInputActivity,
                    "查询失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
//                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSearch.isEnabled = true
            }
        }
    }

    private fun navigateToResult(carbon: com.zg.carbonapp.Entity.CarbonFootprint) {
        val intent = Intent(this, CarbonResultActivity::class.java)
        intent.putExtra("carbon", carbon)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}