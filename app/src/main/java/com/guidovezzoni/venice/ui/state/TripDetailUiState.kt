package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.Stop

data class TripDetailUiState(
    val tripId: String = "",
    val startingPoint: Stop? = null,
    val destination: Stop? = null,
    val intermediateStops: List<Stop> = emptyList(),
    val isLoading: Boolean = false,
    val dialogState: DialogState = DialogState.None,
    val canAddMoreStops: Boolean = false,
    val placeSearchState: PlaceSearchState = PlaceSearchState(),
    val legs: List<Leg> = emptyList(),
    val formattedLegDistances: Map<String, String> = emptyMap(),
    val formattedLegDurations: Map<String, String> = emptyMap(),
    /** null means "route not complete / unavailable" */
    val formattedTotalDistance: String? = null,
    val formattedStopCoordinates: Map<String, String> = emptyMap(),
    val routeCalculationState: RouteCalculationState = RouteCalculationState.Idle,
)
