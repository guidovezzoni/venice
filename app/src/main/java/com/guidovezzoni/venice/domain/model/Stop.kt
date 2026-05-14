package com.guidovezzoni.venice.domain.model

data class Stop(
    val id: String,
    val tripId: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val status: StopStatus,
)
