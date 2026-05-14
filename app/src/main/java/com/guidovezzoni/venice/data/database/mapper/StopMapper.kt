package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.StopEntity
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus

fun StopEntity.toDomain(): Stop = Stop(
    id = id,
    tripId = tripId,
    placeName = placeName,
    latitude = latitude,
    longitude = longitude,
    order = order,
    status = StopStatus.valueOf(status),
)
