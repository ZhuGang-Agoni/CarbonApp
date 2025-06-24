package com.zg.carbonapp.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zg.carbonapp.databinding.FragmentDataAnalysisBinding

class DataAnalyseFragment : Fragment(){

    private lateinit var binding:FragmentDataAnalysisBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
           binding=FragmentDataAnalysisBinding.inflate(layoutInflater)
           return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}