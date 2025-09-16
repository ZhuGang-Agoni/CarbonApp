package com.zg.carbonapp.Activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zg.carbonapp.Dao.ChallengeRecord
import com.zg.carbonapp.Dao.GarbageChallenge
import com.zg.carbonapp.MMKV.GarbageRecordMMKV
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.GarbageKnowledgeBase
import com.zg.carbonapp.Tool.MyToast
import java.util.*
import android.widget.LinearLayout
import com.zg.carbonapp.MMKV.UserChallengePhotoMMKV
import android.speech.tts.TextToSpeech
import android.content.Intent
import android.widget.Toast

class GarbageChallengeActivity : AppCompatActivity() {
    
    private lateinit var ivGarbageImage: ImageView
    private lateinit var btnRecyclable: LinearLayout
    private lateinit var btnHazardous: LinearLayout
    private lateinit var btnKitchen: LinearLayout
    private lateinit var btnOther: LinearLayout
    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var progressBar: com.google.android.material.progressindicator.LinearProgressIndicator
    
    private val challenges = mutableListOf<GarbageChallenge>()
    private var currentChallengeIndex = 0
    private var currentScore = 0
    private var totalQuestions = 10
    private val scorePerQuestion = 10
    
//    private lateinit var textToSpeech: TextToSpeech
    private var ttsReady = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garbage_challenge)
        
        initViews()
        initChallenges()
        startChallenge()

    }
    // 辅助方法：显示Toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun initViews() {
        ivGarbageImage = findViewById(R.id.iv_garbage_image)
        btnRecyclable = findViewById(R.id.btn_recyclable)
        btnHazardous = findViewById(R.id.btn_hazardous)
        btnKitchen = findViewById(R.id.btn_kitchen)
        btnOther = findViewById(R.id.btn_other)
        tvProgress = findViewById(R.id.tv_progress)
        tvScore = findViewById(R.id.tv_score)
        progressBar = findViewById(R.id.progress_bar)
        // 设置按钮点击事件
        btnRecyclable.setOnClickListener { checkAnswer("可回收物") }
        btnHazardous.setOnClickListener { checkAnswer("有害垃圾") }
        btnKitchen.setOnClickListener { checkAnswer("厨余垃圾") }
        btnOther.setOnClickListener { checkAnswer("其他垃圾") }
    }
    
    private fun initChallenges() {
        // 用合并后的题库初始化
        challenges.clear()
        challenges.addAll(getAllChallenges().shuffled().take(totalQuestions))
        totalQuestions = challenges.size
        progressBar.max = totalQuestions
    }
    
    private fun startChallenge() {
        if (GarbageRecordMMKV.getTodayChallengeCount() >= 3) {
            MyToast.sendToast("今日挑战次数已达上限，明天再来吧！", this)
            finish()
            return
        }
        currentChallengeIndex = 0
        currentScore = 0
        showCurrentChallenge()
    }
    
    private fun showCurrentChallenge() {
        if (currentChallengeIndex >= totalQuestions) {
            finishChallenge()
            return
        }
        val challenge = challenges[currentChallengeIndex]
        val imageUrl = challenge.imageUrl
        val context = this
        //判断照片来源 （本地文件/drawable资源)
        if (imageUrl.startsWith("file://") || imageUrl.startsWith("/")) {
            val uri = if (imageUrl.startsWith("file://")) {
                android.net.Uri.parse(imageUrl)
            } else {
                android.net.Uri.fromFile(java.io.File(imageUrl))
            }
            com.bumptech.glide.Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.ic_other)
                .into(ivGarbageImage)
        } else {
            val resId = context.resources.getIdentifier(
                imageUrl, // 直接用 imageName
                "drawable",
                context.packageName
            )
            if (resId != 0) {
                com.bumptech.glide.Glide.with(context)
                    .load(resId)
                    .placeholder(R.drawable.ic_other)
                    .into(ivGarbageImage)
            } else {
                ivGarbageImage.setImageResource(R.drawable.ic_other)
            }
        }
        tvProgress.text = "${currentChallengeIndex + 1}/$totalQuestions"
        tvScore.text = "得分: $currentScore"
        progressBar.progress = currentChallengeIndex + 1
    }
    
    private fun checkAnswer(userAnswer: String) {
        val challenge = challenges[currentChallengeIndex]
        val isCorrect = mapCategory(userAnswer) == mapCategory(challenge.correctCategory)
        
        if (isCorrect) {
            currentScore += scorePerQuestion
            MyToast.sendToast("回答正确！+${scorePerQuestion}分", this)
        } else {
            MyToast.sendToast("回答错误！", this)
            showKnowledgeDialog(challenge)
        }
        
        currentChallengeIndex++
        showCurrentChallenge()
    }
    
    private fun showKnowledgeDialog(challenge: GarbageChallenge) {
        val speakText = "正确答案：${challenge.correctCategory}。${challenge.explanation}"
//        if (ttsReady) {
//            val speakResult = textToSpeech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
//            if (speakResult == TextToSpeech.ERROR) {
//                MyToast.sendToast("语音播报失败", this)
//            }
//        }
        AlertDialog.Builder(this)
            .setTitle("知识科普")
            .setMessage("正确答案：${challenge.correctCategory}\n\n${challenge.explanation}")
            .setPositiveButton("知道了") { _, _ -> }
            .show()
    }
    
    private fun finishChallenge() {
        val accuracy = (currentScore / (totalQuestions * scorePerQuestion.toDouble()) * 100).toInt()
        // 只保存一条完整挑战记录
        val record = ChallengeRecord(
            id = UUID.randomUUID().toString(),
            totalScore = currentScore,
            correctCount = currentScore / scorePerQuestion,
            totalQuestions = totalQuestions,
            timestamp = System.currentTimeMillis(),
            isFinished = true
        )
        GarbageRecordMMKV.saveChallengeRecord(record)

        AlertDialog.Builder(this)
            .setTitle("挑战完成！")
            .setMessage("你的得分：$currentScore\n正确率：$accuracy%\n\n你真棒，再接再厉！根据你的表现，获得${calculateCarbonPoints(currentScore)}碳积分！")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun calculateCarbonPoints(score: Int): Int {
        // 按照总分动态计算奖励
        val maxScore = totalQuestions * scorePerQuestion
        return when {
            score >= maxScore * 0.8 -> 10
            score >= maxScore * 0.6 -> 6
            score >= maxScore * 0.4 -> 3
            else -> 1
        }
    }

    // 分类标准化映射
    private fun mapCategory(raw: String): String {
        return when (raw.trim()) {
            "可回收垃圾", "可回收物" -> "可回收物"
            "有害垃圾" -> "有害垃圾"
            "厨余垃圾", "湿垃圾" -> "厨余垃圾"
            "其他垃圾", "干垃圾" -> "其他垃圾"
            else -> raw
        }
    }

    // 获取全部挑战题库（开发者题库+用户拍照题库）
    private fun getAllChallenges(): List<GarbageChallenge> {
        val devChallenges = getDeveloperChallenges() // 原有开发者题库
        val userChallenges = UserChallengePhotoMMKV.getAll().map {
            GarbageChallenge(
                id = it.timestamp.toString(),
                imageUrl = it.imagePath,
                correctCategory = mapCategory(it.correctCategory),
                options = listOf("可回收物", "有害垃圾", "厨余垃圾", "其他垃圾"),
                explanation = it.explanation,
                source = "user"
            )
        }
        return devChallenges + userChallenges
    }

    // 开发者题库：本地静态知识库转挑战题库，图片用照片名
    private fun getDeveloperChallenges(): List<GarbageChallenge> {
        return GarbageKnowledgeBase.getAllKnowledge().mapIndexed { idx, knowledge ->
            GarbageChallenge(
                id = "dev_$idx",
                imageUrl = knowledge.imageName, // 用照片名
                correctCategory = mapCategory(knowledge.category),
                options = listOf("可回收物", "有害垃圾", "厨余垃圾", "其他垃圾"),
                explanation = knowledge.explanation,
                source = "developer"
            )
        }
    }

    override fun onDestroy() {
//        if (::textToSpeech.isInitialized) {
//            textToSpeech.stop()
//            textToSpeech.shutdown()
//        }
        super.onDestroy()
    }
} 