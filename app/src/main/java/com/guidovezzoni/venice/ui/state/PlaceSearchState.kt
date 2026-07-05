package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion

data class PlaceSearchState(
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedPlaceDetail: PlaceDetail? = null,
    val isResolvingPlace: Boolean = false,
    val placeDetailError: String? = null,
)
