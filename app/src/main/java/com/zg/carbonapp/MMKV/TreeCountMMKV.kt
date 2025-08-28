package com.zg.carbonapp.MMKV

import com.tencent.mmkv.MMKV

object TreeCountMMKV {
    private val mmkv = MMKV.defaultMMKV()

    // 存储总种树数量
    fun saveTotalTreeCount(count: Int) {
        mmkv.encode("total_tree_count", count)
    }

    // 获取总种树数量（默认0）
    fun getTotalTreeCount(): Int {
        return mmkv.decodeInt("total_tree_count", 0)
    }

    // 增加一棵树（原子操作，避免并发问题）
    fun incrementTreeCount() {
        val current = getTotalTreeCount()
        saveTotalTreeCount(current + 1)
    }
}