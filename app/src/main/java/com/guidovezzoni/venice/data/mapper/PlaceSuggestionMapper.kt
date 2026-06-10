package com.guidovezzoni.venice.data.mapper

import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import javax.inject.Inject

class PlaceSuggestionMapper @Inject constructor() {

    fun map(prediction: AutocompletePrediction): PlaceSuggestion = PlaceSuggestion(
        placeId = prediction.placeId,
        primaryText = prediction.getPrimaryText(null).toString(),
        secondaryText = prediction.getSecondaryText(null).toString(),
    )
}
