package com.guidovezzoni.venice.core.analytics

internal fun formatEventLog(event: AnalyticsEvent): String =
    "Event: ${event.name} | Properties: ${event.properties}"

internal fun formatUserPropertyLog(property: AnalyticsUserProperty): String =
    "UserProperty: $property"

internal fun formatExceptionLog(throwable: Throwable, operation: AnalyticsOperation): String =
    "Exception: ${operation.value} | ${throwable.message}"
