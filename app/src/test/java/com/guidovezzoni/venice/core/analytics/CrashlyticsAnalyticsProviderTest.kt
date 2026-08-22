package com.guidovezzoni.venice.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CrashlyticsAnalyticsProviderTest {

    private lateinit var firebaseCrashlytics: FirebaseCrashlytics
    private lateinit var provider: CrashlyticsAnalyticsProvider

    @Before
    fun setUp() {
        firebaseCrashlytics = mockk(relaxed = true)
        provider = CrashlyticsAnalyticsProvider(firebaseCrashlytics)
    }

    // ─── Section 2: trackException ────────────────────────────────────────────

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN trackException is called THEN setCustomKey operation create_trip is invoked before recordException is invoked`() {
        val throwable = RuntimeException("boom")
        val operation = AnalyticsOperation.CREATE_TRIP

        provider.trackException(throwable, operation)

        verifyOrder {
            firebaseCrashlytics.setCustomKey("operation", "create_trip")
            firebaseCrashlytics.recordException(throwable)
        }
    }

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN trackException is called with a specific Throwable instance THEN FirebaseCrashlytics recordException is invoked with that exact instance`() {
        val throwable = RuntimeException("specific-exception")
        val operation = AnalyticsOperation.CREATE_TRIP

        provider.trackException(throwable, operation)

        val expectedThrowable = throwable
        verify { firebaseCrashlytics.recordException(expectedThrowable) }
    }

    // ─── Section 3: logEvent ─────────────────────────────────────────────────

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent OperationFailed is called THEN setCustomKey error_type network is invoked`() {
        val event = AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK)

        provider.logEvent(event)

        val expectedKey = "error_type"
        val expectedValue = "network"
        verify { firebaseCrashlytics.setCustomKey(expectedKey, expectedValue) }
    }

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent OperationFailed is called THEN recordException is never invoked on the mocked FirebaseCrashlytics`() {
        val event = AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK)

        provider.logEvent(event)

        verify(exactly = 0) { firebaseCrashlytics.recordException(any()) }
    }

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent is called with a non-OperationFailed event THEN no method is invoked on the mocked FirebaseCrashlytics`() {
        val event = AnalyticsEvent.TripCreated(isFirstTrip = true)

        provider.logEvent(event)

        verify(exactly = 0) { firebaseCrashlytics.setCustomKey(any(), any<String>()) }
        verify(exactly = 0) { firebaseCrashlytics.recordException(any()) }
    }

    // ─── Section 4: setUserProperty ──────────────────────────────────────────

    @Test
    fun `GIVEN AnalyticsUserProperty TripCountBand RANGE_2_5 WHEN setUserProperty is called THEN FirebaseCrashlytics setCustomKey is invoked with trip_count_band and 2_5`() {
        val property = AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5)

        provider.setUserProperty(property)

        val expectedKey = "trip_count_band"
        val expectedValue = "2_5"
        verify { firebaseCrashlytics.setCustomKey(expectedKey, expectedValue) }
    }

    @Test
    fun `GIVEN AnalyticsUserProperty DistanceUnit IMPERIAL WHEN setUserProperty is called THEN FirebaseCrashlytics setCustomKey is invoked with distance_unit and imperial`() {
        val property = AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL)

        provider.setUserProperty(property)

        val expectedKey = "distance_unit"
        val expectedValue = "imperial"
        verify { firebaseCrashlytics.setCustomKey(expectedKey, expectedValue) }
    }

    // ─── Section 5: Thread safety ─────────────────────────────────────────────

    @Test
    fun `GIVEN a CrashlyticsAnalyticsProvider WHEN trackException is called concurrently from multiple threads THEN every call reaches FirebaseCrashlytics exactly once with no exceptions`() =
        runBlocking {
            val callCount = 20
            val throwables = (0 until callCount).map { index -> RuntimeException("exception-$index") }
            val operations = AnalyticsOperation.entries.toList()
            val jobs = (0 until callCount).map { index ->
                launch(Dispatchers.Default) {
                    provider.trackException(throwables[index], operations[index % operations.size])
                }
            }
            jobs.forEach { it.join() }

            val expectedCallCount = callCount
            verify(exactly = expectedCallCount) { firebaseCrashlytics.recordException(any()) }
        }
}
