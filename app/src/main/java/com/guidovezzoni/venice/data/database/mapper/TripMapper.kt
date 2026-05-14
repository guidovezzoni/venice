package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.TripWithStopCount
import com.guidovezzoni.venice.domain.model.Trip

fun TripWithStopCount.toDomain(): Trip = Trip(
    id = trip.id,
    name = trip.name,
    createdAt = trip.createdAt,
    updatedAt = trip.updatedAt,
    stopCount = stopCount,
)
