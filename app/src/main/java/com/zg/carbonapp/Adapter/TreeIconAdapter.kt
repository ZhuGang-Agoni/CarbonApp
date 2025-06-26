package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.R

class TreeIconAdapter(private val treeList: List<Int>) : RecyclerView.Adapter<TreeIconAdapter.TreeViewHolder>() {
    class TreeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivTree: ImageView = view.findViewById(R.id.ivTree)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tree_icon, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        holder.ivTree.setImageResource(treeList[position])
    }

    override fun getItemCount(): Int = treeList.size
} 