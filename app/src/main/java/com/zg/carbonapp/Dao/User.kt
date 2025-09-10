package com.zg.carbonapp.Dao

data class User(
    // 接口16/17返回的完整字段
    val userId: String = "", // 对应user_id
    var userName: String = "碳助手", // 对应user_name
    var userPassword: String = "",
    var userAvatar: String = "", // 对应user_avatar（头像URL）
    val userTelephone: String = "", // 对应user_telephone（手机号）
    val email: String = "", // 接口返回的邮箱
    val carbonScore: Int = 0, // 对应carbon_score（碳积分）
    var signature: String = "", // 个性签名
    val userQQ: String = "", // 对应user_QQ（QQ号）

    var carbonCount: Int = 0 // 兼容旧代码，实际用carbonScore（可后续统一）
) {
    // 辅助方法：从接口返回数据创建User对象
    companion object {
        fun fromUserInfoResponse(
            userId: String,
            userName: String,
            userTelephone: String,
            email: String,
            carbonScore: Int,
            userAvatar: String,
            signature: String,
            userQQ: String
        ): User {
            return User(
                userId = userId,
                userName = userName,
                userTelephone = userTelephone,
                email = email,
                carbonScore = carbonScore,
                userAvatar = userAvatar,
                signature = signature,
                userQQ = userQQ,
                carbonCount = carbonScore // 兼容旧碳积分字段
            )
        }
    }
}