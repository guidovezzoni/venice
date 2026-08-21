package com.guidovezzoni.venice.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

/**
 * Crashlytics analytics provider. Forwards exceptions to Firebase Crashlytics via
 * [trackException], setting an `operation` custom key before calling [FirebaseCrashlytics.recordException]
 * so the operation context is guaranteed to appear alongside the throwable in the crash report.
 *
 * Thread-safe: no mutable instance state is held. All calls delegate to [FirebaseCrashlytics],
 * which is thread-safe by contract. Safe to call from any thread or coroutine context.
 */
class CrashlyticsAnalyticsProvider @Inject constructor(
    private val firebaseCrashlytics: FirebaseCrashlytics,
) : AnalyticsProvider {

    // setCustomKey persists for the entire Crashlytics session: the error_type value set here
    // will also appear on any subsequent fatal crash or ANR, not just the non-fatal recorded by
    // trackException. This is intentional — it serves as a "last known error type" breadcrumb.
    override fun logEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.OperationFailed -> firebaseCrashlytics.setCustomKey(KEY_ERROR_TYPE, event.errorType.value)
            else -> Unit
        }
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        when (property) {
            is AnalyticsUserProperty.TripCountBand ->
                firebaseCrashlytics.setCustomKey(KEY_TRIP_COUNT_BAND, property.band.value)
            is AnalyticsUserProperty.DistanceUnit ->
                firebaseCrashlytics.setCustomKey(KEY_DISTANCE_UNIT, property.unit.value)
        }
    }

    override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
        firebaseCrashlytics.setCustomKey(KEY_OPERATION, operation.value)
        firebaseCrashlytics.recordException(throwable)
    }

    private companion object {
        const val KEY_OPERATION = "operation"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_TRIP_COUNT_BAND = "trip_count_band"
        const val KEY_DISTANCE_UNIT = "distance_unit"
    }
}
