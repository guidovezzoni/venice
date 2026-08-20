package com.guidovezzoni.venice.ui.state

sealed interface RouteCalculationState {
    data object Idle : RouteCalculationState
    data object Calculating : RouteCalculationState
    data class Error(val message: String) : RouteCalculationState
}
