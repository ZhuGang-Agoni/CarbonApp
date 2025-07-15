package com.zg.carbonapp.Tool

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

object MyToast {
    //这里面就是实现了一个简单的自定义Toast
    fun sendToast(message:String, context: Context){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).apply {
             val cardView=CardView(context).apply {
                   radius=25f//弯角的一个弧度
                   cardElevation=8f
                   useCompatPadding=true//版本的一个兼容问题
                   setCardBackgroundColor(Color.GRAY)

             }

            val textView= TextView(context).apply {
                text=message
                textSize=20f
                setTextColor(Color.BLACK)
                setGravity(Gravity.CENTER)
                setPadding(80,40,80,40)
            }

            cardView.addView(textView)
            //下面这个gravity是那个啥 给Toast配置的
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,0,140)
            view=cardView
            show()
        }
    }
}