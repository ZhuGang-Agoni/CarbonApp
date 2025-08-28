package com.zg.carbonapp.ui.place

import android.view.animation.Transformation
import androidx.constraintlayout.widget.ConstraintSet.Transform
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.zg.carbonapp.logic.model.Place
import com.zg.carbonapp.logic.network.Repository


class PlaceViewModel:ViewModel() {

    private val searchLiveDate=MutableLiveData<String>()

    val placeList=ArrayList<Place>()
//将一个livedata转换为另一个livedata
    val placeLiveData=searchLiveDate.switchMap{ query
        ->
    Repository.searchPlaces(query)
    }

    fun searchPlaces(query :String){
        searchLiveDate.value=query
    }
//    封装一下
    fun savePlace(place:Place)=Repository.savePlace(place)

    fun getSavedPlace()=Repository.getSavedPlace()

    fun isPlaceSaved()=Repository.isPlaceSaved()
 }