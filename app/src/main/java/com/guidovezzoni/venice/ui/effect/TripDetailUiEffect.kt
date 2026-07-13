package com.guidovezzoni.venice.ui.effect

sealed class TripDetailUiEffect {
    data class ShowError(val message: String) : TripDetailUiEffect()
    data class LaunchNavigation(
        val latitude: Double,
        val longitude: Double,
        val placeName: String,
    ) : TripDetailUiEffect()
    data class ShowNavigationError(val message: String) : TripDetailUiEffect()
}
