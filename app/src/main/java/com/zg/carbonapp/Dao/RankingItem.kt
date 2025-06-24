package com.zg.carbonapp.Dao

data class RankingItem(
                         val userName: String,
                         val userEvator:String,
                         val carbonCount: Double,
                         val rank: Int,
                         val isCurrentUser: Boolean = false)
//                         val badge: BadgeType? = null

//enum class BadgeType {
//    TRANSPORT_HERO, ENERGY_SAVER, RECYCLING_CHAMP, WEEKLY_STAR
//}
