package com.guidovezzoni.venice.ui.intent

sealed class TripListUiIntent {
    data object OnCreateTripClicked : TripListUiIntent()
    data object OnDismissCreateDialog : TripListUiIntent()
    data class OnTripNameChanged(val name: String) : TripListUiIntent()
    data object ConfirmCreateTrip : TripListUiIntent()
}
