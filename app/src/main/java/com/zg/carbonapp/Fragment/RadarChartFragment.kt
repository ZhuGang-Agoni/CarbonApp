package com.zg.carbonapp.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.zg.carbonapp.Dao.TravelOption
import com.zg.carbonapp.R

class RadarChartFragment : Fragment() {

    private lateinit var options: List<TravelOption>
    private lateinit var bestOption: TravelOption
    private var distance: Float = 0f

    companion object {
        fun newInstance(
            options: List<TravelOption>,
            bestOption: TravelOption,
            distance: Float
        ): RadarChartFragment {
            return RadarChartFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("options", ArrayList(options))
                    putParcelable("bestOption", bestOption)
                    putFloat("distance", distance)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            options = bundle.getParcelableArrayList<TravelOption>("options") ?: emptyList()
            bestOption = bundle.getParcelable<TravelOption>("bestOption") ?: TravelOption.empty()
            distance = bundle.getFloat("distance", 0f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_radar_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radarChart = view.findViewById<RadarChart>(R.id.radarChart)
        val recommendationText = view.findViewById<TextView>(R.id.recommendationText)

        recommendationText.text = "推荐出行方式: ${bestOption.type}\n${bestOption.recommendation}"
        setupRadarChart(radarChart)
    }

    private fun setupRadarChart(chart: RadarChart) {
        if (options.isEmpty()) return

        val entries = options.mapIndexed { index, option ->
            RadarEntry(option.overallScore())
        }

        val dataSet = RadarDataSet(entries, "出行方式评分").apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
            fillColor = ColorTemplate.MATERIAL_COLORS[0]
            setDrawFilled(true)
            fillAlpha = 120
            lineWidth = 2f
            isDrawHighlightCircleEnabled = true
            setDrawValues(true)
        }

        val data = RadarData(dataSet)

        with(chart) {
            this.data = data
            description.isEnabled = false
            webLineWidth = 1f
            webColor = Color.LTGRAY
            webAlpha = 100
            rotationAngle = 0f

            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return options.getOrNull(value.toInt())?.type ?: ""
                    }
                }
                textSize = 12f
                textColor = Color.DKGRAY
            }

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 5f
                setLabelCount(5, true)
                textSize = 10f
            }

            legend.isEnabled = false
            animateY(1400, Easing.EaseInOutQuad)
            invalidate()
        }
    }
}