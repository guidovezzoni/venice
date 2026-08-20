package com.guidovezzoni.venice.data.mapper

import com.google.android.libraries.places.api.model.Place
import com.guidovezzoni.venice.domain.model.PlaceDetail
import javax.inject.Inject

class PlaceDetailMapper @Inject constructor() {

    fun map(place: Place): PlaceDetail {
        val displayName = checkNotNull(place.displayName) { "Place displayName is null" }
        val location = checkNotNull(place.location) { "Place location is null" }

        return PlaceDetail(
            name = displayName,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }
}
