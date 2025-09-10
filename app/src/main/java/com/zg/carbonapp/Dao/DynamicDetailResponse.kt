package com.zg.carbonapp.Service

import com.zg.carbonapp.Dao.Dynamic
import com.zg.carbonapp.Dao.Comment

data class DynamicDetailResponse(
    val dynamic: Dynamic,
    val comments: List<Comment>
)