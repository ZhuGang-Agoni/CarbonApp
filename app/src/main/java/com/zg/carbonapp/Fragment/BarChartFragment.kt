package com.zg.carbonapp.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.zg.carbonapp.Dao.TravelOption
import com.zg.carbonapp.R

class BarChartFragment : Fragment() {

    private lateinit var options: List<TravelOption>
    private var distance: Float = 0f

    companion object {
        fun newInstance(
            options: List<TravelOption>,
            distance: Float
        ): BarChartFragment {
            return BarChartFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("options", ArrayList(options))
                    putFloat("distance", distance)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            options = bundle.getParcelableArrayList<TravelOption>("options") ?: emptyList()
            distance = bundle.getFloat("distance", 0f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bar_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val carbonInfo = view.findViewById<TextView>(R.id.carbonInfo)

        if (options.isNotEmpty()) {
            setupBarChart(barChart)
            val totalCarbon = options.sumOf { it.carbonFootprint.toDouble() }
            carbonInfo.text = "出行方式碳排放对比（单位：g CO2/km）\n" +
                    "步行和骑行是最环保的选择，自驾碳排放最高"
        }
    }

    private fun setupBarChart(chart: BarChart) {
        val entries = options.mapIndexed { index, option ->
            BarEntry(index.toFloat(), option.carbonFootprint)
        }

        val dataSet = BarDataSet(entries, "碳排放").apply {
            colors = options.map { it.color }
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val data = BarData(dataSet)

        with(chart) {
            this.data = data
            description.text = "碳排放对比"
            setFitBars(true)
            animateY(1000)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return options.getOrNull(value.toInt())?.type ?: ""
                    }
                }
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 50f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }
    }
}