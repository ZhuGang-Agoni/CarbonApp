package com.zg.carbonapp.Tool
import com.zg.carbonapp.Dao.Dynamic
import com.zg.carbonapp.Dao.UserFeed

// 将 Dynamic 转换为 UserFeed
fun Dynamic.toUserFeed(): UserFeed {
    return UserFeed(
        feedId = this.feedId,
        userId = this.userId,
        username = this.userName,
        avatar = this.avatar,
        content = this.content,
        images = this.pics,
        likeCount = this.likeCount,
        commentCount = this.commentCount,
        shareCount = this.collectCount,
        createTime = this.createTime,
        isLiked = this.isLiked,
        isSaved = this.isSaved,
        isCommented = false
    )
}

// 将 UserFeed 转换为 Dynamic
fun UserFeed.toDynamic(): Dynamic {
    return Dynamic(
        userId = this.userId,
        content = this.content,
        feedId = this.feedId,
        createTime = this.createTime,
        avatar = this.avatar,
        userName = this.username,
        pics = this.images,
        likeCount = this.likeCount,
        commentCount = this.commentCount,
        collectCount = this.shareCount,
        isLiked = this.isLiked,
        isSaved = this.isSaved
    )
}