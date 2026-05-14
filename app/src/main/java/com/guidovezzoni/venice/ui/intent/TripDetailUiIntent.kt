package com.guidovezzoni.venice.ui.intent

sealed class TripDetailUiIntent {
    data object OnSetStartingPointClicked : TripDetailUiIntent()
    data class OnStartingPointConfirmed(
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
    ) : TripDetailUiIntent()
    data object OnDismissStartingPointDialog : TripDetailUiIntent()
}
