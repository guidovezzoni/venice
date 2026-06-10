package com.guidovezzoni.venice.data.mapper

import com.google.android.libraries.places.api.model.Place
import com.guidovezzoni.venice.domain.model.PlaceDetail
import javax.inject.Inject

class PlaceDetailMapper @Inject constructor() {

    fun map(place: Place): PlaceDetail {
        val displayName = place.displayName
            ?: throw IllegalStateException("Place displayName is null")
        val location = place.location
            ?: throw IllegalStateException("Place location is null")

        return PlaceDetail(
            name = displayName,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }
}
