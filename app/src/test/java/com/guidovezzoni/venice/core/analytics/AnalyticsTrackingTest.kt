package com.guidovezzoni.venice.core.analytics

import org.junit.Test

class AnalyticsTrackingTest {

    @Test
    fun `GIVEN a trackFailure call WHEN executed THEN logEvent with OperationFailed is called before trackException`() {
        val operation = AnalyticsOperation.CREATE_TRIP
        val errorType = AnalyticsErrorType.NETWORK
        val throwable = RuntimeException("boom")

        val loggedEvents = mutableListOf<AnalyticsEvent>()
        val trackedExceptions = mutableListOf<Pair<Throwable, AnalyticsOperation>>()

        val sut = object : AnalyticsTracking {
            override fun logEvent(event: AnalyticsEvent) { loggedEvents.add(event) }
            override fun setUserProperty(property: AnalyticsUserProperty) { /* no-op */ }
            override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
                trackedExceptions.add(throwable to operation)
            }
        }

        sut.trackFailure(operation, errorType, throwable)

        val expectedEvent = AnalyticsEvent.OperationFailed(operation, errorType)
        assert(loggedEvents == listOf(expectedEvent)) {
            "Expected logEvent called once with OperationFailed but got: $loggedEvents"
        }
        assert(trackedExceptions == listOf(throwable to operation)) {
            "Expected trackException called once with throwable and operation but got: $trackedExceptions"
        }
    }

    @Test
    fun `GIVEN a trackFailure call WHEN executed THEN logEvent is called before trackException`() {
        val callOrder = mutableListOf<String>()
        val sut = object : AnalyticsTracking {
            override fun logEvent(event: AnalyticsEvent) { callOrder.add("logEvent") }
            override fun setUserProperty(property: AnalyticsUserProperty) { /* no-op */ }
            override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
                callOrder.add("trackException")
            }
        }

        sut.trackFailure(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.TIMEOUT, RuntimeException())

        assert(callOrder == listOf("logEvent", "trackException")) {
            "Expected logEvent before trackException but got: $callOrder"
        }
    }
}
