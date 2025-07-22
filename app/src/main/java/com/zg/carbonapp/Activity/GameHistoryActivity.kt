package com.zg.carbonapp.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.zg.carbonapp.Adapter.GameHistoryAdapter
import com.zg.carbonapp.MMKV.GameHistoryRecordMMKV
import com.zg.carbonapp.databinding.ActivityGameHistoryBinding

// 这个是那个啥 访问历史记录
class GameHistoryActivity : AppCompatActivity() {

    private lateinit var binding:ActivityGameHistoryBinding
    private lateinit var adapter: GameHistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityGameHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initAdapter()
    }

    private fun initAdapter(){
       val list= GameHistoryRecordMMKV.getGameRecordItem()
//         这里简单的判断一波
       if (list.isEmpty()){
           binding.recyclerHistory.visibility= View.GONE
           Toast.makeText(this,"还没有相应的历史记录哦，快去参加挑战吧",Toast.LENGTH_SHORT).show()
       }
        else {
            adapter=GameHistoryAdapter(this,list)
            binding.recyclerHistory.adapter=adapter
            binding.recyclerHistory.layoutManager=LinearLayoutManager(this)
       }

    }
    private fun initListener(){
//       返回逻辑
        binding.`return`.setOnClickListener{
            finish()
        }

//        清空逻辑
        binding.clearLog.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("确定要清除历史记录吗")
                .setPositiveButton("确定"){_,_->
                    adapter.clearLog()
//                    这个逻辑一定要加上
                    GameHistoryRecordMMKV.clearAllRecords()

                    Toast.makeText(this,"清除成功",Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消"){_,_->
                    {

                    }
                }
                .show()

        }


        }



    }