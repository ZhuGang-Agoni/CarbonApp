package com.zg.carbonapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.GameHistoryRecord
import com.zg.carbonapp.R
import org.w3c.dom.Text

class GameHistoryAdapter(val context:Context,var list:List<GameHistoryRecord>):
    RecyclerView.Adapter<GameHistoryAdapter.ViewHolder>() {


    private val itemList: MutableList<GameHistoryRecord> = list.toMutableList()
    inner  class ViewHolder(view : View):RecyclerView.ViewHolder(view){

        val score=view.findViewById<TextView>(R.id.tv_item_score)
        val carbon=view.findViewById<TextView>(R.id.tv_item_carbon)
        val time=view.findViewById<TextView>(R.id.tv_item_date)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
         val view=LayoutInflater.from(parent.context).
         inflate(R.layout.item_history,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
         val item=itemList[position]

         holder.time.text = item.date
         holder.carbon.text="减少碳排放： "+item.carbonReduction+"kg"
         holder.score.text="分数： "+item.score

    }
    override fun getItemCount(): Int {
        return list.size
    }

    fun clearLog(){
        itemList.clear() // 现在不会报错了
        itemList.addAll(emptyList())
        notifyDataSetChanged()
    }
}