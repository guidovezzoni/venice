package com.guidovezzoni.venice.core.analytics

enum class AnalyticsOperation(val value: String) {
    CREATE_TRIP("create_trip"),
    SET_STOP("set_stop"),
    EDIT_STOP("edit_stop"),
    REMOVE_STOP("remove_stop"),
    MOVE_STOP("move_stop"),
    MARK_DEPARTED("mark_departed"),
    CALCULATE_ROUTE("calculate_route"),
}
