package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.LegEntity
import com.guidovezzoni.venice.domain.model.Leg

fun LegEntity.toDomain(): Leg = Leg(
    id = id,
    tripId = tripId,
    fromStopId = fromStopId,
    toStopId = toStopId,
    distanceMetres = distanceMetres,
    durationSeconds = durationSeconds,
    encodedPolyline = encodedPolyline,
)

fun Leg.toEntity(): LegEntity = LegEntity(
    id = id,
    tripId = tripId,
    fromStopId = fromStopId,
    toStopId = toStopId,
    distanceMetres = distanceMetres,
    durationSeconds = durationSeconds,
    encodedPolyline = encodedPolyline,
)
