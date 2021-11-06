package com.application.myapplication.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.myapplication.DirectionsApiService
import com.mechanizo.android.customer.data.model.response.directions.RouteDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(): ViewModel() {

    private val directionsMLD = MutableLiveData<RouteDirections>()
    val directionsLD: LiveData<RouteDirections> = directionsMLD
    val repo = DirectionsApiService.buildApi()

    fun getDirectionsForCoords(start: String, end: String){

        Timber.e("getdir called!")
    viewModelScope.launch(Dispatchers.IO) {
        val res = repo.getRouteDirections(start = start, end = end)


        if(res.isSuccessful){
            directionsMLD.postValue(res.body())
        }else{
            Timber.e(res.errorBody()?.string().toString())
        }

    }

    }

}