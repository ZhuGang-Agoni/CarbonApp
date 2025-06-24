package com.zg.carbonapp.Tool

import androidx.lifecycle.MutableLiveData
import com.zg.carbonapp.Dao.Scene

object UpdateRecyclerHelper {
    val notify = MutableLiveData(true)
    var sceneList:MutableList<Scene> = mutableListOf()
}