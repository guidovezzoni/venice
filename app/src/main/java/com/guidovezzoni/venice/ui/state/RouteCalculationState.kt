package com.guidovezzoni.venice.ui.state

sealed class RouteCalculationState {
    data object Idle : RouteCalculationState()
    data object Calculating : RouteCalculationState()
    data class Error(val message: String) : RouteCalculationState()
}
