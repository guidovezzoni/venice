package com.guidovezzoni.venice.ui.effect

sealed class TripDetailUiEffect {
    data class ShowError(val message: String) : TripDetailUiEffect()
}
