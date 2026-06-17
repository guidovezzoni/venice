package com.guidovezzoni.venice.data.mapper

import com.guidovezzoni.venice.data.network.dto.DirectionsLeg
import com.guidovezzoni.venice.domain.model.Leg
import java.util.UUID

private const val DEFAULT_DISTANCE_METRES = 0
private const val DEFAULT_DURATION_SECONDS = 0
private const val DEFAULT_ENCODED_POLYLINE = ""

fun DirectionsLeg.toDomain(
    tripId: String,
    fromStopId: String,
    toStopId: String,
): Leg = Leg(
    id = UUID.randomUUID().toString(),
    tripId = tripId,
    fromStopId = fromStopId,
    toStopId = toStopId,
    distanceMetres = distanceMetres ?: DEFAULT_DISTANCE_METRES,
    durationSeconds = durationSeconds ?: DEFAULT_DURATION_SECONDS,
    encodedPolyline = encodedPolyline ?: DEFAULT_ENCODED_POLYLINE,
)
