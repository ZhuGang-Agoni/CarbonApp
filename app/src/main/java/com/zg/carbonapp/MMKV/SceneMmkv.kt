package com.zg.carbonapp.MMKV

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.zg.carbonapp.Dao.Scene
import com.zg.carbonapp.Fragment.CompareSceneFragment

//对场景进行解析
object SceneMmkv {
    private val mmkv by lazy { MMKV.mmkvWithID("sceneCache") }
    private val gson = Gson()
    private const val SCENE_LIST_KEY = "scene_list"
    private const val COMPARE_HISTORY_KEY = "compare_history"
    private const val OPTIMIZE_SUGGESTION_KEY = "optimize_suggestion"

    fun setScene(scene: MutableList<Scene>){
         mmkv.encode("scene",gson.toJson(scene))//转换为JSON格式的字符串存起来
    }

    fun getScene():MutableList<Scene>?{//解码
         val json=mmkv.decodeString("scene") ?:return null
        val type = object : TypeToken<MutableList<Scene>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
    //下面这些就是弄着玩的 不要当真
    fun setCompareHistory(historyJson: String) {
        mmkv.encode(COMPARE_HISTORY_KEY, historyJson)
    }

    // 新增：获取对比历史
    fun getCompareHistory(): List<CompareSceneFragment.ComparisonResult>? {
        val json = mmkv.decodeString(COMPARE_HISTORY_KEY)
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<CompareSceneFragment.ComparisonResult>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 新增：提交优化建议（可扩展审核流程）
    fun submitOptimizeSuggestion(suggestion: String) {
        val suggestions = getOptimizeSuggestions().toMutableList()
        suggestions.add(suggestion)
        mmkv.encode(OPTIMIZE_SUGGESTION_KEY, gson.toJson(suggestions))
    }

    // 新增：获取优化建议
    fun getOptimizeSuggestions(): List<String> {
        val json = mmkv.decodeString(OPTIMIZE_SUGGESTION_KEY)
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}