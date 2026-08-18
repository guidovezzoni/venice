package com.guidovezzoni.venice.core.analytics

import javax.inject.Inject

/**
 * Fans out every analytics operation to all registered [AnalyticsProvider]s.
 *
 * Thread-safe: all three operations delegate to a fixed, immutable [Set] obtained at
 * construction time, and each provider is required to be thread-safe itself.
 *
 * When [providers] is empty every operation is a silent no-op — not an error, not a crash.
 * This is the expected state in a release build until story 9.2.1 wires a real backend provider.
 */
class CompositeAnalyticsClient @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards AnalyticsProvider>,
) : AnalyticsClient {

    override fun logEvent(event: AnalyticsEvent) {
        providers.forEach { provider ->
            provider.logEvent(event)
        }
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        providers.forEach { provider ->
            provider.setUserProperty(property)
        }
    }

    override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
        providers.forEach { provider ->
            provider.trackException(throwable, operation)
        }
    }
}
