package com.codecrafters.locationtracker.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

class TrackingViewModel : ViewModel() {
    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking

    private val _routePoints = MutableLiveData<MutableList<GeoPoint>>(mutableListOf())
    val routePoints: LiveData<MutableList<GeoPoint>> get() = _routePoints

    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long> = _startTime

    private val _stopTime = MutableLiveData<Long>()
    val stopTime: LiveData<Long> = _stopTime

    private val _totalDistance = MutableLiveData<Double>(0.0)
    val totalDistance: LiveData<Double> get() = _totalDistance

    fun startTracking() {
        _isTracking.value = true
        _routePoints.value?.clear()
        _totalDistance.value = 0.0
        _startTime.value = System.currentTimeMillis()
    }

    fun stopTracking() {
        _isTracking.value = false
        _stopTime.value = System.currentTimeMillis()

    }

    fun pauseTracking() {
        _isTracking.value = false
    }

    fun resumeTracking() {
        _isTracking.value = true
    }

    fun addRoutePoint(point: GeoPoint) {
        _routePoints.value?.add(point)
        _routePoints.postValue(_routePoints.value)
    }

    fun updateTotalDistance(distance: Double) {
        _totalDistance.value = distance
    }
}
