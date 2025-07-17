package com.zg.carbonapp.Tool

import com.zg.carbonapp.Dao.GarbageKnowledge
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.DeepSeekHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object GarbageKnowledgeBase {
    // 本地对象型知识库
    private val localKnowledge = mapOf(
        "塑料瓶" to GarbageKnowledge(
            name = "塑料瓶",
            category = "可回收物",
            explanation = "塑料瓶由PET材料制成，可以回收再利用。",
            tips = "投放前请清空内容物，压扁节省空间",
            imageName = "bg_plastic_bottle"
        ),
        "电池" to GarbageKnowledge(
            name = "电池",
            category = "有害垃圾",
            explanation = "电池属于有害垃圾，含有重金属。",
            tips = "请投放到专门的电池回收箱",
            imageName = "bg_battery"
        ),
        "果皮" to GarbageKnowledge(
            name = "果皮",
            category = "厨余垃圾",
            explanation = "果皮属于有机废弃物，可以堆肥处理。",
            tips = "可与其他厨余垃圾一同投放",
            imageName = "bg_fruit_peel"
        ),
        "废纸" to GarbageKnowledge(
            name = "废纸",
            category = "可回收物",
            explanation = "废纸可以回收再利用，制成再生纸、纸板等产品。",
            tips = "请保持干燥",
            imageName = "bg_wastepaper"
        ),
        "灯管" to GarbageKnowledge(
            name = "灯管",
            category = "有害垃圾",
            explanation = "灯管含有汞等有害物质，需要特殊处理。",
            tips = "请勿随意丢弃",
            imageName = "bg_fluorescent_lamp"
        ),
        "餐巾纸" to GarbageKnowledge(
            name = "餐巾纸",
            category = "其他垃圾",
            explanation = "餐巾纸因污染严重，纤维短，无法回收。",
            tips = "请勿投入可回收物",
            imageName = "bg_napkin"
        ),
        "药品" to GarbageKnowledge(
            name = "药品",
            category = "有害垃圾",
            explanation = "过期药品含有有害成分，需特殊处理。",
            tips = "请投放到有害垃圾专用回收箱",
            imageName = "bg_medicine"
        ),
        "易拉罐" to GarbageKnowledge(
            name = "易拉罐",
            category = "可回收物",
            explanation = "易拉罐为铝制品，可回收再利用。",
            tips = "请清空内容物，压扁投放",
            imageName = "bg_pop_can"
        )
    )
    // 智能科普缓存（字符串）
    private val cache = mutableMapOf<String, String>()

    /**
     * 智能科普查询：本地优先，无则DeepSeek API兜底，拿到后缓存
     * 返回Pair<category, explanation>
     */
    suspend fun getKnowledgeWithDeepSeek(garbageName: String): Pair<String, String> {
        // 1. 本地静态知识优先
        localKnowledge[garbageName]?.let { return it.category to it.explanation }
        // 2. 本地缓存
        cache[garbageName]?.let {
            // 缓存格式：分类|说明
            val parts = it.split("|", limit = 2)
            return if (parts.size == 2) parts[0] to parts[1] else "" to it
        }
        // 3. DeepSeek API兜底，要求返回结构化内容
        return suspendCancellableCoroutine { cont ->
            val sb = StringBuilder()
            DeepSeekHelper().sendMessageStream(
                prompt = "${garbageName}是什么垃圾？请用如下格式回答：\\n分类：xxx\\n说明：xxx",
                charDelay = 0L,
                onChar = { sb.append(it) },
                onComplete = {
                    val result = sb.toString()
                    // 解析分类和说明
                    val category = Regex("分类：(.+?)\\n").find(result)?.groupValues?.get(1)?.trim() ?: ""
                    val explanation = Regex("说明：(.+)").find(result)?.groupValues?.get(1)?.trim() ?: result.trim()
                    // 缓存格式：分类|说明
                    cache[garbageName] = "$category|$explanation"
                    if (cont.isActive) cont.resume(category to explanation)
                },
                onError = { err ->
                    if (cont.isActive) cont.resume("" to "暂无相关知识（$err）")
                }
            )
        }
    }

    fun getKnowledge(garbageName: String): GarbageKnowledge? {
        return localKnowledge[garbageName]
    }

    fun searchGarbage(query: String): List<GarbageKnowledge> {
        return localKnowledge.values.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    fun getAllCategories(): List<String> {
        return listOf("可回收物", "有害垃圾", "厨余垃圾", "其他垃圾")
    }

    fun getAllKnowledge(): List<GarbageKnowledge> {
        return localKnowledge.values.toList()
    }
} 