package com.zg.carbonapp.Fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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

class CompareSceneFragment : Fragment() {

    // 用于保存对比历史（可持久化到 MMKV/数据库）
    private val compareHistory = mutableListOf<ComparisonResult>()
    private lateinit var et_scene1: EditText
    private lateinit var et_scene2: EditText
    private lateinit var tv_smart_analysis: TextView
    private lateinit var btn_compare: Button
    private lateinit var btn_save_history: Button
    private lateinit var chart_comparison: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scenarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        et_scene1 = view.findViewById(R.id.et_scene1)
        et_scene2 = view.findViewById(R.id.et_scene2)
        tv_smart_analysis = view.findViewById(R.id.tv_smart_analysis)
        btn_compare = view.findViewById(R.id.btn_compare)
        btn_save_history = view.findViewById(R.id.btn_save_history)
        chart_comparison = view.findViewById(R.id.chart_comparison)

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
        // 示例：仅用detail长度做对比（实际应用可扩展为碳排放等）
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, sceneA.detail.length.toFloat()))
        entries.add(BarEntry(1f, sceneB.detail.length.toFloat()))
        val dataSet = BarDataSet(entries, "场景对比")
        dataSet.colors = listOf(
            resources.getColor(R.color.green_light, null),
            resources.getColor(R.color.blue_light, null)
        )
        val barData = BarData(dataSet)
        chart_comparison.data = barData
        chart_comparison.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(arrayOf(sceneA.name, sceneB.name))
        chart_comparison.invalidate()
    }

    // 智能分析：结合权重算法生成环保评分 + 改善建议
    private fun generateSmartAnalysis(sceneA: Scene, sceneB: Scene): String {
        // 示例算法：detail长度越长分数越高
        val scoreA = sceneA.detail.length
        val scoreB = sceneB.detail.length
        val winner = if (scoreA > scoreB) sceneA.name else sceneB.name
        val suggestion = if (winner == sceneA.name) {
            "${sceneB.name} 可参考 ${sceneA.name} 的方案，丰富场景细节"
        } else {
            "${sceneA.name} 可学习 ${sceneB.name} 的方案，丰富场景细节"
        }
        return """
            智能分析结果：
            - ${sceneA.name} 评分：$scoreA
            - ${sceneB.name} 评分：$scoreB
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