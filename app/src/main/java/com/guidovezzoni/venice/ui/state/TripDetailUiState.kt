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
    /**
     * null means "route not complete / unavailable".
     * Transitions to/from null in lockstep with [formattedTotalDistance], since both share the
     * same completeness check (`legs.size == stops.size - 1 && stops.size >= 2`).
     */
    val formattedTotalDuration: String? = null,
    val formattedStopCoordinates: Map<String, String> = emptyMap(),
    val routeCalculationState: RouteCalculationState = RouteCalculationState.Idle,
    /**
     * The name of the trip being viewed.
     * null means "trip not yet loaded / not yet known".
     * The composable falls back to the string resource `trip_detail_title` ("Trip Detail")
     * while this is null. Using null (not "") distinguishes "not loaded yet" from a
     * hypothetically blank name.
     */
    val tripName: String? = null,
    /**
     * Whether to display the route recalculation prompt.
     * Derived in the ViewModel's `combine(stops, legs)` collector.
     * True when the route is incomplete AND `stops.size >= 2`.
     * False otherwise (route is complete, or there are fewer than 2 stops).
     * Transitions in lockstep with [formattedTotalDistance] and [formattedTotalDuration]
     * becoming null, but has an additional `stops.size >= 2` guard that the total fields
     * do not have.
     */
    val isRouteRecalculationPromptVisible: Boolean = false,
)

