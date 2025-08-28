package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.Activity
import com.zg.carbonapp.Dao.ActivityStatus
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.TimeUtils

class ActivityAdapter(
    private val activities: MutableList<Activity>,
    private val onJoinClick: (Activity) -> Unit,
    private val onCancelClick: (Activity) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    private var filteredList: List<Activity> = activities

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: androidx.appcompat.widget.AppCompatImageView = itemView.findViewById(R.id.activityImage)
        val tvName: TextView = itemView.findViewById(R.id.activityName)
        val tvPoints: TextView = itemView.findViewById(R.id.activityPoints)
        val tvTime: TextView = itemView.findViewById(R.id.activityTime)
        val tvLocation: TextView = itemView.findViewById(R.id.activityLocation)
        val tvParticipants: TextView = itemView.findViewById(R.id.participantCount)
        val btnJoin: TextView = itemView.findViewById(R.id.joinButton)
        val btnCancel: TextView = itemView.findViewById(R.id.cancelButton)
        val tvTag: TextView = itemView.findViewById(R.id.activityTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = filteredList[position]
        // 动态计算状态（核心）
        val status = TimeUtils.getActivityStatus(activity.startTime, activity.endTime)

        holder.apply {
            ivImage.setImageResource(activity.imageRes)
            tvName.text = activity.name
            tvPoints.text = "${activity.points}积分"
            tvTime.text = "${activity.startTime} - ${activity.endTime}"
            tvLocation.text = activity.location
            tvParticipants.text = "已有${activity.participantCount}人参与"

            // 状态标签
            when (status) {
                ActivityStatus.NOT_STARTED -> {
                    tvTag.text = "未开始"
                    tvTag.setBackgroundResource(R.drawable.shape_tag_yellow)
                }
                ActivityStatus.ON_GOING -> {
                    tvTag.text = "进行中"
                    tvTag.setBackgroundResource(R.drawable.shape_tag_green)
                }
                ActivityStatus.ENDED -> {
                    tvTag.text = "已结束"
                    tvTag.setBackgroundResource(R.drawable.shape_tag_gray)
                }
            }

            // 按钮控制（已结束活动不能操作）
            if (status == ActivityStatus.ENDED) {
                btnJoin.visibility = View.GONE
                btnCancel.visibility = View.GONE
            } else {
                if (activity.joined) {
                    btnJoin.visibility = View.GONE
                    btnCancel.visibility = View.VISIBLE
                } else {
                    btnJoin.visibility = View.VISIBLE
                    btnCancel.visibility = View.GONE
                }
            }

            // 报名点击
            btnJoin.setOnClickListener {
                if (UserMMKV.getUser() == null) {
                    Toast.makeText(it.context, "请先登录", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onJoinClick(activity)
            }

            // 取消报名点击
            btnCancel.setOnClickListener {
                onCancelClick(activity)
            }
        }
    }

    fun filter(predicate: (Activity) -> Boolean) {
        filteredList = activities.filter(predicate)
        notifyDataSetChanged()
    }

    fun updateData(newActivities: List<Activity>) {
        activities.clear()
        activities.addAll(newActivities)
        filteredList = newActivities
        notifyDataSetChanged()
    }

    override fun getItemCount() = filteredList.size
}
