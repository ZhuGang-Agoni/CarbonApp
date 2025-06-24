package com.zg.carbonapp.Fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.zg.carbonapp.Dao.Scene
import com.zg.carbonapp.MMKV.SceneMmkv
import com.zg.carbonapp.R
import com.zg.carbonapp.Tool.MyToast
import java.util.*
import kotlin.collections.ArrayList

class CompareSceneFragment : Fragment() {

    // 用于保存对比历史（可持久化到 MMKV/数据库）
    private val compareHistory = mutableListOf<ComparisonResult>()
    private lateinit var et_scene1:EditText
    private lateinit var et_scene2:EditText
    private lateinit var   tv_smart_analysis:TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_compare_scene, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btn_compare=view.findViewById<Button>(R.id.btn_compare)
         et_scene1=view.findViewById<EditText>(R.id.et_scene1)
         et_scene2=view.findViewById<EditText>(R.id.et_scene2)
        tv_smart_analysis=view.findViewById<TextView>(R.id.tv_smart_analysis)
        val btn_save_history=view.findViewById<Button>(R.id.btn_save_history)
        // 绑定按钮事件
        btn_compare.setOnClickListener { startCompare() }
        btn_save_history.setOnClickListener { saveCompareHistory() }

    }

    // 核心对比逻辑：校验场景 + 可视化 + 智能分析
    private fun startCompare() {
        val name1 = et_scene1.text.toString().trim()
        val name2 = et_scene2.text.toString().trim()

        // 1. 从 MMKV 获取场景数据（需确保 SceneMmkv 正确实现）
        val allScenes = SceneMmkv.getScene() ?: mutableListOf()
        val scene1 = allScenes.find { it.name == name1 }
        val scene2 = allScenes.find { it.name == name2 }

        // 2. 校验场景存在性
        when {
            scene1 == null && scene2 == null -> {
                context?.let {
                    MyToast.sendToast("\"$name1\" 和 \"$name2\" 都未添加！",
                        it.applicationContext)
                }
            }
            scene1 == null -> {
                context?.let {
                    MyToast.sendToast("\"$name1\" 未添加，请先创建！",
                        it.applicationContext)
                }
            }
            scene2 == null -> {
                context?.let {
                    MyToast.sendToast("\"$name2\" 未添加，请先创建！",
                        it.applicationContext)
                }
            }
            else -> {
                // 3. 执行可视化对比（示例：柱状图）
                renderComparisonChart(scene1, scene2)
                // 4. 执行智能分析
                val analysis = generateSmartAnalysis(scene1, scene2)
                tv_smart_analysis.text = analysis
                // 5. 保存对比结果到历史（内存，可扩展持久化）
                compareHistory.add(
                    ComparisonResult(
                        scene1.name,
                        scene2.name,
                        Date().time,
                        analysis
                    )
                )
            }
        }
    }

    // 可视化：用柱状图对比「碳排放、空气质量、绿化率」（可扩展更多维度）
    private fun renderComparisonChart(sceneA: Scene, sceneB: Scene) {
        val chart = view?.findViewById<BarChart>(R.id.chart_comparison) ?: return


    }

    // 智能分析：结合权重算法生成环保评分 + 改善建议
    private fun generateSmartAnalysis(sceneA: Scene, sceneB: Scene): String {
        // 简易算法：碳排放*0.4 + 空气质量*0.3 + 绿化率*0.3
        fun calculateScore(scene: Scene): Double {
            return 100.0
//            return scene.carbonEmission.toDouble() * 0.4 +
//                    scene.airQuality.toDouble() * 0.3 +
//                    scene.greenRate.toDouble() * 0.3
        }

        val scoreA = calculateScore(sceneA)
        val scoreB = calculateScore(sceneB)
        val winner = if (scoreA > scoreB) sceneA.name else sceneB.name

        // 生成建议（可接入 AI 接口优化，这里示例固定文案）
        val suggestion = if (winner == sceneA.name) {
            "${sceneB.name} 可参考 ${sceneA.name} 的绿化方案，增加植被覆盖率"
        } else {
            "${sceneA.name} 可学习 ${sceneB.name} 的碳排放控制策略"
        }

        return """
            智能分析结果：
            - ${sceneA.name} 综合得分：${String.format("%.2f", scoreA)}
            - ${sceneB.name} 综合得分：${String.format("%.2f", scoreB)}
            - 更优场景：$winner
            - 改善建议：$suggestion
        """.trimIndent()
    }

    // 保存对比历史（示例：持久化到 MMKV）
    private fun saveCompareHistory() {
        if (compareHistory.isEmpty()) {
            context?.let { MyToast.sendToast("暂无对比记录可保存", it.applicationContext) }
            return
        }
        // 转换为 JSON 存储
        val historyJson = Gson().toJson(compareHistory)
        SceneMmkv.setCompareHistory(historyJson)
        context?.let {
            com.zg.carbonapp.Tool.MyToast.sendToast("对比记录已保存！可在「历史」页面查看",
                it.applicationContext)
        }
    }



    // 对比结果数据类（用于历史记录）
    data class ComparisonResult(
        val sceneA: String,
        val sceneB: String,
        val compareTime: Long,
        val analysisResult: String
    )
}