package com.guidovezzoni.venice.core.analytics

sealed class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
) {

    data class TripCreated(val tripId: String) : AnalyticsEvent(
        name = EVENT_TRIP_CREATED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class TripOpened(val tripId: String) : AnalyticsEvent(
        name = EVENT_TRIP_OPENED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class StopSet(val tripId: String, val stopType: String) : AnalyticsEvent(
        name = EVENT_STOP_SET,
        properties = mapOf(PROPERTY_TRIP_ID to tripId, PROPERTY_STOP_TYPE to stopType),
    )

    data class StopEdited(val tripId: String) : AnalyticsEvent(
        name = EVENT_STOP_EDITED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class StopRemoved(val tripId: String) : AnalyticsEvent(
        name = EVENT_STOP_REMOVED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class StopReordered(val tripId: String) : AnalyticsEvent(
        name = EVENT_STOP_REORDERED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class StopDeparted(val tripId: String, val stopId: String) : AnalyticsEvent(
        name = EVENT_STOP_DEPARTED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId, PROPERTY_STOP_ID to stopId),
    )

    data class RouteCalculated(val tripId: String) : AnalyticsEvent(
        name = EVENT_ROUTE_CALCULATED,
        properties = mapOf(PROPERTY_TRIP_ID to tripId),
    )

    data class ScreenViewed(val screenName: String) : AnalyticsEvent(
        name = EVENT_SCREEN_VIEWED,
        properties = mapOf(PROPERTY_SCREEN_NAME to screenName),
    )

    data class OperationFailed(val operation: String, val errorMessage: String) : AnalyticsEvent(
        name = EVENT_OPERATION_FAILED,
        properties = mapOf(PROPERTY_OPERATION to operation, PROPERTY_ERROR_MESSAGE to errorMessage),
    )

    companion object {
        private const val EVENT_TRIP_CREATED = "trip_created"
        private const val EVENT_TRIP_OPENED = "trip_opened"
        private const val EVENT_STOP_SET = "stop_set"
        private const val EVENT_STOP_EDITED = "stop_edited"
        private const val EVENT_STOP_REMOVED = "stop_removed"
        private const val EVENT_STOP_REORDERED = "stop_reordered"
        private const val EVENT_STOP_DEPARTED = "stop_departed"
        private const val EVENT_ROUTE_CALCULATED = "route_calculated"
        private const val EVENT_SCREEN_VIEWED = "screen_viewed"
        private const val EVENT_OPERATION_FAILED = "operation_failed"

        const val PROPERTY_TRIP_ID = "trip_id"
        const val PROPERTY_STOP_ID = "stop_id"
        const val PROPERTY_STOP_TYPE = "stop_type"
        const val PROPERTY_SCREEN_NAME = "screen_name"
        const val PROPERTY_OPERATION = "operation"
        const val PROPERTY_ERROR_MESSAGE = "error_message"

        const val SCREEN_TRIP_LIST = "trip_list"
        const val SCREEN_TRIP_DETAIL = "trip_detail"

        const val OPERATION_CREATE_TRIP = "create_trip"
        const val OPERATION_SET_STOP = "set_stop"
        const val OPERATION_EDIT_STOP = "edit_stop"
        const val OPERATION_REMOVE_STOP = "remove_stop"
        const val OPERATION_MOVE_STOP = "move_stop"
        const val OPERATION_MARK_DEPARTED = "mark_departed"
        const val OPERATION_CALCULATE_ROUTE = "calculate_route"
    }
}
