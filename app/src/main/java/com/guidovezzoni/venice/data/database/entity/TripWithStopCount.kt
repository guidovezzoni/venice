package com.guidovezzoni.venice.data.database.entity

import androidx.room.Embedded

data class TripWithStopCount(
    @Embedded val trip: TripEntity,
    val stopCount: Int,
)
