package com.guidovezzoni.venice.core.analytics

sealed interface AnalyticsUserProperty {

    data class TripCountBand(val band: CountBand) : AnalyticsUserProperty

    data class DistanceUnit(val unit: DistanceUnitParam) : AnalyticsUserProperty
}
