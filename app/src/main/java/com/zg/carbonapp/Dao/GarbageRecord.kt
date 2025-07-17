package com.zg.carbonapp.Dao

data class GarbageRecord(
    val garbageName: String,
    val categoryName: String,
    val time: String,
    val categoryIcon: Int
)

data class GarbageChallenge(
    val id: String,
    val imageUrl: String,
    val correctCategory: String,
    val options: List<String>, // 四个选项
    val explanation: String, // 分类原由
    val source: String = "developer" // "developer" 或 "user"
)

data class ChallengeRecord(
    val id: String,
    val totalScore: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val timestamp: Long,
    val isFinished: Boolean // 只保存完整挑战
)

data class RecognitionRecord(
    val id: String,
    val garbageName: String,
    val category: String,
    val explanation: String,
    val imageUrl: String?,
    val recognitionMethod: String, // "search" 或 "camera"
    val timestamp: Long
)

data class GarbageKnowledge(
    val name: String,
    val category: String,
    val explanation: String,
    val tips: String,
    val imageName: String // 图片名（不带后缀，推荐png）
) 