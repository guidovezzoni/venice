package com.guidovezzoni.venice.core.analytics

sealed class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
) {

    data class TripCreated(val isFirstTrip: Boolean) : AnalyticsEvent(
        name = "trip_created",
        properties = mapOf("is_first_trip" to isFirstTrip),
    )

    data class TripOpened(val stopCount: Int, val routeState: String) : AnalyticsEvent(
        name = "trip_opened",
        properties = mapOf("stop_count" to stopCount, "route_state" to routeState),
    )

    data class StopAdded(val stopType: StopTypeParam, val stopCount: Int) : AnalyticsEvent(
        name = "stop_added",
        properties = mapOf("stop_type" to stopType.value, "stop_count" to stopCount),
    )

    data class RouteCalculated(
        val stopCount: Int,
        val legCount: Int,
        val distanceBand: DistanceBand,
        val durationBand: DurationBand,
    ) : AnalyticsEvent(
        name = "route_calculated",
        properties = mapOf(
            "stop_count" to stopCount,
            "leg_count" to legCount,
            "distance_band" to distanceBand.value,
            "duration_band" to durationBand.value,
        ),
    )

    data class NavigationLaunched(val stopType: StopTypeParam, val stopPosition: Int) : AnalyticsEvent(
        name = "navigation_launched",
        properties = mapOf("stop_type" to stopType.value, "stop_position" to stopPosition),
    )

    data class StopEdited(val stopType: StopTypeParam) : AnalyticsEvent(
        name = "stop_edited",
        properties = mapOf("stop_type" to stopType.value),
    )

    data class StopRemoved(val stopType: StopTypeParam, val stopCount: Int) : AnalyticsEvent(
        name = "stop_removed",
        properties = mapOf("stop_type" to stopType.value, "stop_count" to stopCount),
    )

    data class StopReordered(val direction: String, val stopCount: Int) : AnalyticsEvent(
        name = "stop_reordered",
        properties = mapOf("direction" to direction, "stop_count" to stopCount),
    )

    data class StopDeparted(val stopPosition: Int, val stopCount: Int) : AnalyticsEvent(
        name = "stop_departed",
        properties = mapOf("stop_position" to stopPosition, "stop_count" to stopCount),
    )

    data class StopDepartureUndone(val stopPosition: Int) : AnalyticsEvent(
        name = "stop_departure_undone",
        properties = mapOf("stop_position" to stopPosition),
    )

    data class PlaceSearchPerformed(val suggestionCount: Int) : AnalyticsEvent(
        name = "place_search_performed",
        properties = mapOf("suggestion_count" to suggestionCount),
    )

    data class PlaceSuggestionSelected(val suggestionPosition: Int) : AnalyticsEvent(
        name = "place_suggestion_selected",
        properties = mapOf("suggestion_position" to suggestionPosition),
    )

    data class OperationFailed(
        val operation: AnalyticsOperation,
        val errorType: AnalyticsErrorType,
    ) : AnalyticsEvent(
        name = "operation_failed",
        properties = mapOf("operation" to operation.value, "error_type" to errorType.value),
    )

    data class ScreenViewed(val screen: AnalyticsScreen) : AnalyticsEvent(
        name = "screen_viewed",
        properties = mapOf("screen_name" to screen.value),
    )
}
