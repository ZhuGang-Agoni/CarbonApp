package com.zg.carbonapp.Dao

//这个是进行排名
data class RankingItem(
                         val id:String,
                         val userName: String,
                         val userEvator:String,
                         val carbonCount: Double,
                         val rank: Int,
                         val isCurrentUser: Boolean = true)
//                         val badge: BadgeType? = null

//enum class BadgeType {
//    TRANSPORT_HERO, ENERGY_SAVER, RECYCLING_CHAMP, WEEKLY_STAR
//}
