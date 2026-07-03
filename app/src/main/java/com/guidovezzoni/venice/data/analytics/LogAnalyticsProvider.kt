package com.guidovezzoni.venice.data.analytics

import android.util.Log
import com.guidovezzoni.venice.domain.analytics.AnalyticsEvent
import com.guidovezzoni.venice.domain.analytics.AnalyticsProvider
import javax.inject.Inject

class LogAnalyticsProvider @Inject constructor() : AnalyticsProvider {

    override fun shouldTrack(event: AnalyticsEvent): Boolean = true

    override fun track(event: AnalyticsEvent) {
        Log.d(LOG_TAG, "Event: ${event.name} | Properties: ${event.properties}")
    }

    private companion object {
        const val LOG_TAG = "Analytics"
    }
}
