package com.guidovezzoni.venice.ui.intent

sealed class TripDetailUiIntent {
    data object OnSetStartingPointClicked : TripDetailUiIntent()
    data class OnStartingPointConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent()
    data object OnDismissStartingPointDialog : TripDetailUiIntent()
    data object OnSetDestinationClicked : TripDetailUiIntent()
    data class OnDestinationConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent()
    data object OnDismissDestinationDialog : TripDetailUiIntent()
    data object OnAddStopClicked : TripDetailUiIntent()
    data class OnAddStopConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent()
    data object OnDismissAddStopDialog : TripDetailUiIntent()
}
