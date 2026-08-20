package com.guidovezzoni.venice.ui.intent

import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.model.Stop

sealed interface TripDetailUiIntent {
    data object OnSetStartingPointClicked : TripDetailUiIntent
    data class OnStartingPointConfirmed(
        // TODO: should PlaceDetail be used here? And in the following cases
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent
    data object OnDismissStartingPointDialog : TripDetailUiIntent
    data object OnSetDestinationClicked : TripDetailUiIntent
    data class OnDestinationConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent
    data object OnDismissDestinationDialog : TripDetailUiIntent
    data object OnAddStopClicked : TripDetailUiIntent
    data class OnAddStopConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent
    data object OnDismissAddStopDialog : TripDetailUiIntent
    data class OnMoveStopUp(val stopId: String, val currentOrder: Int) : TripDetailUiIntent
    data class OnMoveStopDown(val stopId: String, val currentOrder: Int) : TripDetailUiIntent
    data class OnEditStopClicked(val stop: Stop) : TripDetailUiIntent
    data class OnEditStopConfirmed(
        val stopId: String,
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent
    data object OnDismissEditStopDialog : TripDetailUiIntent
    data class OnRemoveStopClicked(val stop: Stop) : TripDetailUiIntent
    data object OnRemoveStopConfirmed : TripDetailUiIntent
    data object OnDismissRemoveStopDialog : TripDetailUiIntent
    data class OnMarkStopDepartedClicked(val stopId: String) : TripDetailUiIntent
    data class OnUndoMarkStopDepartedClicked(val stopId: String) : TripDetailUiIntent
    data class OnNavigateToStopClicked(val stopId: String) : TripDetailUiIntent
    data class OnSearchQueryChanged(val query: String) : TripDetailUiIntent
    data class OnSuggestionSelected(val suggestion: PlaceSuggestion) : TripDetailUiIntent
    data object OnCalculateRouteClicked : TripDetailUiIntent
}
