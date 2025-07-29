package com.zg.carbonapp.Activity



import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.CarbonActionAdapter
import com.zg.carbonapp.databinding.ActivityCarbonLedgerBinding
import com.zg.carbonapp.Repository.CarbonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarbonLedgerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarbonLedgerBinding
    private val adapter by lazy { CarbonActionAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarbonLedgerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvActions.layoutManager = LinearLayoutManager(this)
        binding.rvActions.adapter = adapter

        loadCarbonActions()
    }

    private fun loadCarbonActions() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                CarbonRepository().getAllCarbonActions().collect { actions ->
                    withContext(Dispatchers.Main) {
                        if (actions.isEmpty()) {
                            Toast.makeText(this@CarbonLedgerActivity, "碳账本为空", Toast.LENGTH_SHORT).show()
                        }
                        adapter.submitList(actions)
                    }
                }
            }
        }
    }
}