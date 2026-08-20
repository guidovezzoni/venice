package com.guidovezzoni.venice.core.analytics

interface AnalyticsTracking {
    fun logEvent(event: AnalyticsEvent)
    fun setUserProperty(property: AnalyticsUserProperty)
    fun trackException(throwable: Throwable, operation: AnalyticsOperation)

    fun trackFailure(operation: AnalyticsOperation, errorType: AnalyticsErrorType, throwable: Throwable) {
        logEvent(AnalyticsEvent.OperationFailed(operation, errorType))
        trackException(throwable, operation)
    }
}
