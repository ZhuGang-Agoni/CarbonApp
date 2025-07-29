package com.zg.carbonapp.Activity



import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.databinding.ActivityCarbonResultBinding
import com.zg.carbonapp.Entity.CarbonAction
import com.zg.carbonapp.Repository.CarbonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CarbonResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonResultBinding
    private lateinit var carbon: com.zg.carbonapp.Entity.CarbonFootprint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carbon = intent.getSerializableExtra("carbon") as com.zg.carbonapp.Entity.CarbonFootprint
        showCarbonInfo()

        binding.btnAddToLedger.setOnClickListener {
            addToCarbonLedger()
        }
    }

    private fun showCarbonInfo() {
        binding.tvProductName.text = carbon.name
        binding.tvCarbonEmission.text = String.format("碳排放量: %.2f kg", carbon.carbonEmission)
        binding.tvLifecycle.text = "生命周期: ${carbon.lifecycle}"
        binding.tvSource.text = "数据来源: ${carbon.source}"
        binding.tvSuggestion.text = "减碳建议: ${carbon.suggestion}"
    }

    private fun addToCarbonLedger() {
        CoroutineScope(Dispatchers.Main).launch {
            val action = CarbonAction(
                productName = carbon.name,
                action = "记录物品碳足迹",
                reducedCarbon = 0.0
            )
            CarbonRepository().recordCarbonAction(action)
            binding.btnAddToLedger.text = "已加入碳账本"
            binding.btnAddToLedger.isEnabled = false
        }
    }
}