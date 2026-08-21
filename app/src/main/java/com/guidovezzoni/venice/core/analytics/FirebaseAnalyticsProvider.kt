package com.guidovezzoni.venice.core.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.guidovezzoni.venice.BuildConfig
import javax.inject.Inject

/**
 * Firebase Analytics provider. Forwards all analytics events and user properties to Google
 * Firebase Analytics (GA4 for Firebase).
 *
 * Thread-safe: all calls delegate to [FirebaseAnalytics], which is thread-safe by contract.
 * No mutable instance state is held.
 */
class FirebaseAnalyticsProvider @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val isDebugBuild: Boolean = BuildConfig.DEBUG,
) : AnalyticsProvider {

    override fun logEvent(event: AnalyticsEvent) {
        if (event is AnalyticsEvent.ScreenViewed) {
            firebaseAnalytics.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, event.screen.value) }
            )
            return
        }
        if (validateEventName(event.name) && validateProperties(event.properties)) {
            firebaseAnalytics.logEvent(event.name, buildBundle(event.properties))
        }
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        val (name, value) = when (property) {
            is AnalyticsUserProperty.TripCountBand -> PROP_TRIP_COUNT_BAND to property.band.value
            is AnalyticsUserProperty.DistanceUnit -> PROP_DISTANCE_UNIT to property.unit.value
        }
        validateAndSetUserProperty(name, value)
    }

    /**
     * Visible for testing — validates the mapped Firebase property name, then forwards to
     * [FirebaseAnalytics.setUserProperty]. Exposed because the current taxonomy's mapped names are
     * all well under the 24-character limit, making the length validation unreachable via the public
     * [setUserProperty] API without a user property whose Firebase name exceeds 24 characters.
     */
    internal fun validateAndSetUserProperty(name: String, value: String) {
        if (validateUserPropertyName(name)) {
            firebaseAnalytics.setUserProperty(name, value)
        }
    }

    override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
        // Intentional no-op: Firebase Analytics is a product-analytics destination, not a crash
        // reporter. Crash reporting is delivered by a future Crashlytics provider (story 9.2.2).
    }

    private fun validateEventName(name: String): Boolean {
        val message = when {
            name.length > MAX_EVENT_NAME_LENGTH ->
                "Event name '$name' exceeds $MAX_EVENT_NAME_LENGTH characters (has ${name.length})"
            !IDENTIFIER_PATTERN.matches(name) ->
                "Event name '$name' must start with a letter and contain only [A-Za-z0-9_]"
            else -> null
        }
        if (message != null) reportPlatformLimitViolation(message)
        return message == null
    }

    private fun validateProperties(properties: Map<String, Any>): Boolean {
        if (properties.size > MAX_PROPERTIES_COUNT) {
            reportPlatformLimitViolation(
                "Event has ${properties.size} parameters, exceeding the $MAX_PROPERTIES_COUNT limit"
            )
            return false
        }
        return properties.all { (key, value) ->
            val keyValid = validateParameterName(key)
            val valueMessage = if (value is String && value.length > MAX_STRING_VALUE_LENGTH) {
                "Parameter '$key' value length ${value.length} exceeds $MAX_STRING_VALUE_LENGTH characters"
            } else {
                null
            }
            if (valueMessage != null) reportPlatformLimitViolation(valueMessage)
            keyValid && valueMessage == null
        }
    }

    private fun validateParameterName(name: String): Boolean {
        val message = when {
            name.length > MAX_PARAM_NAME_LENGTH ->
                "Parameter name '$name' exceeds $MAX_PARAM_NAME_LENGTH characters (has ${name.length})"
            !IDENTIFIER_PATTERN.matches(name) ->
                "Parameter name '$name' must start with a letter and contain only [A-Za-z0-9_]"
            RESERVED_PREFIXES.any { name.startsWith(it) } ->
                "Parameter name '$name' uses a reserved prefix (firebase_, google_, ga_)"
            else -> null
        }
        if (message != null) reportPlatformLimitViolation(message)
        return message == null
    }

    private fun validateUserPropertyName(name: String): Boolean {
        val message = when {
            name.length > MAX_USER_PROPERTY_NAME_LENGTH ->
                "User property name '$name' exceeds $MAX_USER_PROPERTY_NAME_LENGTH characters (has ${name.length})"
            !IDENTIFIER_PATTERN.matches(name) ->
                "User property name '$name' must start with a letter and contain only [A-Za-z0-9_]"
            RESERVED_PREFIXES.any { name.startsWith(it) } ->
                "User property name '$name' uses a reserved prefix (firebase_, google_, ga_)"
            else -> null
        }
        if (message != null) reportPlatformLimitViolation(message)
        return message == null
    }

    private fun buildBundle(properties: Map<String, Any>): Bundle {
        return Bundle().apply {
            for ((key, value) in properties) {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putString(key, value.toString())
                    else -> reportPlatformLimitViolation(
                        "Unsupported parameter type for key '$key': ${value::class.simpleName}"
                    )
                }
            }
        }
    }

    private fun reportPlatformLimitViolation(message: String) {
        if (isDebugBuild) {
            error(message)
        } else {
            Log.e(LOG_TAG, message)
        }
    }

    private companion object {
        const val LOG_TAG = "Analytics"
        const val MAX_EVENT_NAME_LENGTH = 40
        const val MAX_PARAM_NAME_LENGTH = 40
        const val MAX_USER_PROPERTY_NAME_LENGTH = 24
        const val MAX_STRING_VALUE_LENGTH = 100
        const val MAX_PROPERTIES_COUNT = 25
        const val PROP_TRIP_COUNT_BAND = "trip_count_band"
        const val PROP_DISTANCE_UNIT = "distance_unit"
        val IDENTIFIER_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*")
        val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
    }
}
