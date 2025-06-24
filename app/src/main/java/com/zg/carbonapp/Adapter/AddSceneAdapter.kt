package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zg.carbonapp.Dao.Scene
import com.zg.carbonapp.Fragment.SearchFragment
import com.zg.carbonapp.R

class AddSceneAdapter(val sceneList:List<Scene>, val activity: AppCompatActivity) :RecyclerView.Adapter<AddSceneAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val btn = view.findViewById<FloatingActionButton>(R.id.add_scene)
        val img = view.findViewById<ImageView>(R.id.img_scene)
        val scene_Name = view.findViewById<TextView>(R.id.tv_scene_name)
        val simple_Des = view.findViewById<TextView>(R.id.tv_scene_desc)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scene = sceneList[position]

        holder.itemView.setOnClickListener {//打开卡片
            showSceneDetailDialog(scene)
        }

        holder.scene_Name.text = scene.name
        holder.simple_Des.text = scene.description
        //这边用Glide加载
        Glide.with(holder.itemView.context).load(R.drawable.forest).into(holder.img)
//        holder.btn.setOnClickListener{
//              showAddSceneDialog()
//        }

    }

    override fun getItemCount(): Int {
        return sceneList.size
    }

    private fun showSceneDetailDialog(scene:Scene) {
        val dialog = AlertDialog.Builder(activity)//自己引入一个context不就行了吗
            .setTitle(scene.name)
            .setMessage("描述：${scene.description}\n\n詳情：${scene.detail}\n\n}")
            .setPositiveButton("關閉", null)
            .create()
        dialog.show()
    }

}