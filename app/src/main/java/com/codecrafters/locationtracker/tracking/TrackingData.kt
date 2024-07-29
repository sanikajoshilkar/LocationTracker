package com.codecrafters.locationtracker.tracking

data class TrackingData(
    val imageUrl: String = "",
    val startTime: String = "",
    val stopTime: String = "",
    val stepsTracked: Int = 0,
    val distanceTraveled: Double = 0.0
)
