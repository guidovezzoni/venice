package com.guidovezzoni.venice.core.analytics

import android.util.Log
import javax.inject.Inject

/**
 * Logcat analytics provider for debug builds.
 *
 * WARNING: This class must ONLY ever be registered via a debug-source-set DI module
 * (e.g. DebugAnalyticsModule in app/src/debug/). It must NEVER be bound in a release
 * build variant — Logcat output is readable over ADB on any connected device, which
 * leaks the full analytics event stream to anyone with physical access.
 *
 * Thread-safe: all calls delegate to [Log.d]/[Log.e], which are thread-safe.
 */
class DebugAnalyticsProvider @Inject constructor() : AnalyticsProvider {

    override fun logEvent(event: AnalyticsEvent) {
        Log.d(LOG_TAG, formatEventLog(event))
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        Log.d(LOG_TAG, formatUserPropertyLog(property))
    }

    override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
        Log.e(LOG_TAG, formatExceptionLog(operation), throwable)
    }

    private companion object {
        const val LOG_TAG = "Analytics"
    }
}
