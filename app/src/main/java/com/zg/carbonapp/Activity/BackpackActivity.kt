package com.zg.carbonapp.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.zg.carbonapp.DB.TreeDatabase
import com.zg.carbonapp.DB.TreeEntity
import com.zg.carbonapp.MMKV.TreeCountMMKV
import com.zg.carbonapp.R
import kotlinx.coroutines.launch
import java.util.UUID

class BackpackActivity : AppCompatActivity() {
    // 可种植的树种列表
    private val treeTypes = listOf(
        TreeType("松树", "生长快，耐寒", R.drawable.stage_3),
        TreeType("杨树", "高大挺拔，防风", R.drawable.stage_3),
        TreeType("樟树", "常绿乔木，驱蚊", R.drawable.stage_3),
        TreeType("枫树", "秋季变红，美观", R.drawable.stage_3)
    )

    data class TreeType(
        val name: String,
        val desc: String,
        val iconRes: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backpack)

        // 返回按钮
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }

        // 显示总种树数量（从MMKV获取）
        val totalCount = TreeCountMMKV.getTotalTreeCount()
        findViewById<TextView>(R.id.tv_total_count).text = "$totalCount}棵"

        // 初始化树种列表
        findViewById<RecyclerView>(R.id.rv_tree_types).apply {
            layoutManager = GridLayoutManager(this@BackpackActivity, 2)
            adapter = TreeTypeAdapter(treeTypes) { treeType ->
                // 种植选中的树
                plantTree(treeType.name)
            }
        }
    }

    // 种植新树（保存到数据库+更新MMKV计数）
    private fun plantTree(treeType: String) {
        lifecycleScope.launch {
            // 1. 保存到数据库
            val newTree = TreeEntity(
                id = UUID.randomUUID().toString(),
                treeType = treeType,
                plantTime = System.currentTimeMillis(),
                lastWaterTime = 0,
                growthSpeed = 1.0f
            )
            TreeDatabase.getInstance(this@BackpackActivity).treeDao().addTree(newTree)

            // 2. 更新MMKV计数
            TreeCountMMKV.incrementTreeCount()

            // 3. 提示并返回主界面
            android.widget.Toast.makeText(
                this@BackpackActivity,
                "成功种植一棵$treeType！",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    // 树种选择适配器
    inner class TreeTypeAdapter(
        private val types: List<TreeType>,
        private val onPlantClick: (TreeType) -> Unit
    ) : RecyclerView.Adapter<TreeTypeAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: ImageView = itemView.findViewById(R.id.iv_tree_icon)
            val tvName: TextView = itemView.findViewById(R.id.tv_tree_name)
            val tvDesc: TextView = itemView.findViewById(R.id.tv_tree_desc)
            val btnPlant: Button = itemView.findViewById(R.id.btn_plant)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tree_type, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val type = types[position]
            holder.ivIcon.setImageResource(type.iconRes)
            holder.tvName.text = type.name
            holder.tvDesc.text = type.desc
            holder.btnPlant.setOnClickListener { onPlantClick(type) }
        }

        override fun getItemCount() = types.size
    }
}