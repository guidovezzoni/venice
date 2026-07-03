package com.guidovezzoni.venice.domain.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
