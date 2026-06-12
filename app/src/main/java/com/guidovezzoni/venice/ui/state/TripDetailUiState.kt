package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.model.Stop

// TODO Ideally this should be broken down in a sealed class, to make the info more consistent
data class TripDetailUiState(
    val tripId: String = "",
    val startingPoint: Stop? = null,
    val destination: Stop? = null,
    val intermediateStops: List<Stop> = emptyList(),
    val isLoading: Boolean = false,
    val isSetStartingPointDialogVisible: Boolean = false,
    val isSetDestinationDialogVisible: Boolean = false,
    val isAddStopDialogVisible: Boolean = false,
    val isEditStopDialogVisible: Boolean = false,
    val editingStop: Stop? = null,
    val canAddMoreStops: Boolean = false,
    val isRemoveStopDialogVisible: Boolean = false,
    val stopToRemove: Stop? = null,
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val isSearchingPlaces: Boolean = false,
    val searchError: String? = null,
    val selectedPlaceDetail: PlaceDetail? = null,
    val isResolvingPlace: Boolean = false,
    val placeDetailError: String? = null,
)

