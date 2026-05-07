package com.guidovezzoni.venice.ui.effect

sealed class TripListUiEffect {
    data class NavigateToTripDetail(val tripId: String) : TripListUiEffect()
    data class ShowError(val message: String) : TripListUiEffect()
}
