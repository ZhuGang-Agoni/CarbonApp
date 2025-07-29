package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.ShopExchangeRecordAdapter
import com.zg.carbonapp.Dao.ShopRecord
import com.zg.carbonapp.Dao.User
import com.zg.carbonapp.MMKV.ShopRecordMMKV
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.databinding.ActivityShopExchangeRecordBinding

class ShopExchangeRecordActivity : AppCompatActivity() {
    private lateinit var binding:ActivityShopExchangeRecordBinding
    private lateinit var adapter: ShopExchangeRecordAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityShopExchangeRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val list=ShopRecordMMKV.getShopRecordItem()
        adapter=ShopExchangeRecordAdapter(this,list)
        binding.rvRecords.adapter=adapter
        binding.rvRecords.layoutManager=LinearLayoutManager(this)

        initLisener()

    }


    private fun initLisener(){
        binding.btnBack.setOnClickListener{
             finish()
        }

        binding.btnClear.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定要删除你的兑换吗")
                .setPositiveButton("确定"){_,_->
                    ShopRecordMMKV.clearAllRecords()
                    adapter.clearLog()
//                     原则上这里还有后端 不过不重要
                    Toast.makeText(this,"删除成功", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("再想想"){_,_->{

                }}
                .show()
        }

    }
}