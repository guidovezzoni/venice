package com.guidovezzoni.venice.core.analytics

internal fun formatEventLog(event: AnalyticsEvent): String =
    "Event: ${event.name} | Properties: ${event.properties}"

internal fun formatUserPropertyLog(property: AnalyticsUserProperty): String =
    "UserProperty: $property"

internal fun formatExceptionLog(operation: AnalyticsOperation): String =
    "Exception: ${operation.value}"
