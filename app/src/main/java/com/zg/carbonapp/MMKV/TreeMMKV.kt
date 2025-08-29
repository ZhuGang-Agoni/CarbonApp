package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.TreeModel

object TreeMMKV {
    private val mmkv = MMKV.defaultMMKV()
    private const val KEY_TREES = "user_trees"
    private val gson = Gson()
    private val treeListType = object : TypeToken<List<TreeModel>>() {}.type

    // 保存树木列表
    fun saveTrees(trees: List<TreeModel>) {
        val jsonStr = gson.toJson(trees, treeListType)
        mmkv.encode(KEY_TREES, jsonStr)
    }

    // 获取所有树木
    fun getTrees(): List<TreeModel> {
        val jsonStr = mmkv.decodeString(KEY_TREES, "") ?: ""
        return if (jsonStr.isEmpty()) emptyList() else gson.fromJson(jsonStr, treeListType)
    }

    // 添加新树
    fun addTree(tree: TreeModel) {
        val current = getTrees().toMutableList()
        current.add(tree)
        saveTrees(current)
    }
}