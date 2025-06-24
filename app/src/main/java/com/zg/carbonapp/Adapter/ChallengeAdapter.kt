package com.zg.carbonapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.Challenge
import com.zg.carbonapp.R

/**
 * 挑战列表的 RecyclerView 适配器
 * @param challenges 挑战数据列表
 * @param onJoinCLick 参与挑战回调
 * @param onCheckInClick 打卡回调
 * @param onRestartClick 重新挑战回调
 * @param onQuitClick 退出挑战回调
 */
class ChallengeAdapter(
    private val challenges: List<Challenge>,
    private val onJoinCLick: (Int) -> Unit,
    private val onCheckInClick: (Int) -> Unit,
    private val onRestartClick: (Int) -> Unit,
    private val onQuitClick: (Int) -> Unit
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>(){

    /**
     * ViewHolder 持有每个挑战卡片的所有控件
     */
    class ChallengeViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvTitle: TextView = view.findViewById(R.id.tvTitle) // 挑战标题
        val tvDescription: TextView = view.findViewById(R.id.tvDescription) // 挑战描述
        val tvTarget: TextView = view.findViewById(R.id.tvTarget) // 挑战目标
        val btnJoin: Button = view.findViewById(R.id.btnJoin) // 参与按钮
        val btnCheckIn: Button = view.findViewById(R.id.btnCheckIn) // 打卡按钮
        val btnRestart: Button = view.findViewById(R.id.btnRestart) // 重新挑战按钮
        val btnQuit: Button = view.findViewById(R.id.btnQuit) // 退出挑战按钮
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar) // 进度条
        val tvProgress: TextView = view.findViewById(R.id.tvProgress) // 进度文本
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge,parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        // 设置基本信息
        holder.tvTitle.text = challenge.title
        holder.tvDescription.text = challenge.description
        holder.tvTarget.text = "目标" + challenge.target
        holder.progressBar.max = challenge.total
        holder.progressBar.progress = challenge.progress
        holder.tvProgress.text = "进度: ${challenge.progress}/${challenge.total}"

        // 参与按钮：仅在未参与时显示
        holder.btnJoin.visibility = if (!challenge.isJoined) View.VISIBLE else View.GONE
        holder.btnJoin.setOnClickListener { onJoinCLick(position) }

        // 打卡按钮：仅在已参与且未完成时显示，且一天只能打卡一次
        val today = getTodayString()
        val canCheckIn = challenge.isJoined && !challenge.isCompleted && challenge.lastCheckInDate != today
        holder.btnCheckIn.visibility = if (challenge.isJoined && !challenge.isCompleted) View.VISIBLE else View.GONE
        holder.btnCheckIn.isEnabled = canCheckIn
        holder.btnCheckIn.text = if (canCheckIn) "打卡" else "已打卡"
        holder.btnCheckIn.setOnClickListener { onCheckInClick(position) }

        // 重新挑战按钮：仅在已完成时显示
        holder.btnRestart.visibility = if (challenge.isCompleted) View.VISIBLE else View.GONE
        holder.btnRestart.setOnClickListener { onRestartClick(position) }

        // 退出挑战按钮：仅在已参与且未完成时显示
        holder.btnQuit.visibility = if (challenge.isJoined && !challenge.isCompleted) View.VISIBLE else View.GONE
        holder.btnQuit.setOnClickListener { onQuitClick(position) }
    }

    override fun getItemCount(): Int = challenges.size

    /**
     * 获取今天的日期字符串（yyyy-MM-dd），用于判断是否已打卡
     */
    private fun getTodayString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}