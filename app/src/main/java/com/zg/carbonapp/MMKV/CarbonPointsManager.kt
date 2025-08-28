package com.zg.carbonapp.MMKV

import com.zg.carbonapp.Dao.User

object CarbonPointsManager {
    // 给用户增加积分（在原有基础上累加）
    fun addPoints(userId: String, points: Int) {
        if (points <= 0) return // 不处理非正数积分

        val user = UserMMKV.getUser() ?: return
        // 原有积分 + 新增积分（核心：累加逻辑）
        val newPoints = user.carbonCount + points
        // 更新用户信息并保存到UserMMKV
        val updatedUser = user.copy(carbonCount = newPoints)
        UserMMKV.saveUser(updatedUser)
    }

    // 其他方法保持不变...
}