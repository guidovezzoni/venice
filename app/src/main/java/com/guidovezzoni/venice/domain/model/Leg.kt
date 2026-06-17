package com.guidovezzoni.venice.domain.model

data class Leg(
    val id: String,
    val tripId: String,
    val fromStopId: String,
    val toStopId: String,
    val distanceMetres: Int,
    val durationSeconds: Int,
    val encodedPolyline: String,
)
