package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.TripEntity
import com.guidovezzoni.venice.domain.model.Trip

fun TripEntity.toDomain(stopCount: Int = 0): Trip = Trip(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    stopCount = stopCount,
)
