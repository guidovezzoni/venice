package com.guidovezzoni.venice.domain.analytics

interface AnalyticsProvider {
    fun shouldTrack(event: AnalyticsEvent): Boolean
    fun track(event: AnalyticsEvent)
}
