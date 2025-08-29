package com.zg.carbonapp.Fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.Dao.TravelOption
import com.zg.carbonapp.R

class RecommendationFragment : Fragment() {

    private lateinit var options: List<TravelOption>
    private var distance: Float = 0f

    companion object {
        fun newInstance(
            options: List<TravelOption>,
            distance: Float
        ): RecommendationFragment {
            return RecommendationFragment().apply {
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
        return inflater.inflate(R.layout.fragment_recommendation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RecommendationAdapter(options)

        view.findViewById<TextView>(R.id.title).text = "详细出行建议"
    }
}

class RecommendationAdapter(private val options: List<TravelOption>) :
    RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val recommendation: TextView = view.findViewById(R.id.recommendation)
        val scoreBar: ProgressBar = view.findViewById(R.id.scoreBar)
        val scoreText: TextView = view.findViewById(R.id.scoreText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]

        holder.icon.setImageResource(option.iconRes)
        holder.title.text = option.type
        holder.recommendation.text = option.recommendation

        val score = (option.overallScore() * 20).toInt()
        holder.scoreBar.progress = score
        holder.scoreText.text = "${score}%"

        when {
            score >= 80 -> holder.scoreBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
            score >= 60 -> holder.scoreBar.progressTintList = ColorStateList.valueOf(Color.YELLOW)
            else -> holder.scoreBar.progressTintList = ColorStateList.valueOf(Color.RED)
        }
    }

    override fun getItemCount() = options.size
}