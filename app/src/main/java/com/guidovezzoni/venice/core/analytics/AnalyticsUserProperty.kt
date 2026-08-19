package com.guidovezzoni.venice.core.analytics

sealed class AnalyticsUserProperty {

    data class TripCountBand(val band: CountBand) : AnalyticsUserProperty()

    data class DistanceUnit(val unit: String) : AnalyticsUserProperty()
}
