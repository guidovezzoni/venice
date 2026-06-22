package com.guidovezzoni.venice.data.analytics

import com.guidovezzoni.venice.domain.analytics.AnalyticsEvent
import com.guidovezzoni.venice.domain.analytics.AnalyticsProvider
import com.guidovezzoni.venice.domain.analytics.AnalyticsTracker
import javax.inject.Inject

class CompositeAnalyticsTracker @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards AnalyticsProvider>,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        providers.forEach { provider ->
            if (provider.shouldTrack(event)) {
                provider.track(event)
            }
        }
    }
}
