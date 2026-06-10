package com.guidovezzoni.venice.data.mapper

import com.google.android.libraries.places.api.model.Place
import com.guidovezzoni.venice.domain.model.PlaceDetail
import javax.inject.Inject

class PlaceDetailMapper @Inject constructor() {

    fun map(place: Place): PlaceDetail = PlaceDetail(
        // TODO !! should be replaced with some explicit exception throwing like place.displayName?: throw NullPointerException()
        name = place.displayName!!,
        latitude = place.location!!.latitude,
        longitude = place.location!!.longitude,
    )
}
