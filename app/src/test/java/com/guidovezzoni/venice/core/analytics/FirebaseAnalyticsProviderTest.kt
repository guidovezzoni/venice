package com.guidovezzoni.venice.core.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class FirebaseAnalyticsProviderTest {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var provider: FirebaseAnalyticsProvider

    @Before
    fun setUp() {
        mockkConstructor(Bundle::class)
        every { anyConstructed<Bundle>().putString(any(), any()) } just Runs
        every { anyConstructed<Bundle>().putLong(any(), any()) } just Runs
        every { anyConstructed<Bundle>().putDouble(any(), any()) } just Runs

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        firebaseAnalytics = mockk(relaxed = true)
        provider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = false)
    }

    @After
    fun tearDown() {
        unmockkConstructor(Bundle::class)
        unmockkStatic(Log::class)
    }

    // ─── Section 3: Construction ──────────────────────────────────────────────

    @Test
    fun `GIVEN a mocked FirebaseAnalytics WHEN FirebaseAnalyticsProvider is constructed THEN it implements AnalyticsProvider and performs no static SDK lookup`() {
        // Type assignment confirms FirebaseAnalyticsProvider implements AnalyticsProvider at compile time.
        // confirmVerified ensures no Firebase SDK methods were called during construction.
        @Suppress("UNUSED_VARIABLE")
        val constructed: AnalyticsProvider = FirebaseAnalyticsProvider(firebaseAnalytics)

        confirmVerified(firebaseAnalytics)
    }

    // ─── Section 4: Generic event property mapping ────────────────────────────

    @Test
    fun `GIVEN an AnalyticsEvent with a String property WHEN logEvent is called THEN Bundle putString is invoked with the same key and value`() {
        val event = AnalyticsEvent.StopEdited(stopType = StopTypeParam.DESTINATION)

        provider.logEvent(event)

        verify { anyConstructed<Bundle>().putString("stop_type", "destination") }
    }

    @Test
    fun `GIVEN an AnalyticsEvent with an Int property WHEN logEvent is called THEN Bundle putLong is invoked with the same key and the value as Long`() {
        val event = AnalyticsEvent.StopDeparted(stopPosition = 1, stopCount = 3)

        provider.logEvent(event)

        verify { anyConstructed<Bundle>().putLong("stop_position", 1L) }
        verify { anyConstructed<Bundle>().putLong("stop_count", 3L) }
    }

    @Test
    fun `GIVEN TripCreated with isFirstTrip true WHEN logEvent is called THEN Bundle putString is invoked with is_first_trip and the String true not a Boolean`() {
        val event = AnalyticsEvent.TripCreated(isFirstTrip = true)

        provider.logEvent(event)

        verify { anyConstructed<Bundle>().putString("is_first_trip", "true") }
    }

    @Test
    fun `GIVEN all non-ScreenViewed events WHEN logEvent is called THEN Firebase logEvent is invoked with the event name as first argument`() {
        val nonScreenEvents = listOf(
            AnalyticsEvent.TripCreated(isFirstTrip = true),
            AnalyticsEvent.TripOpened(stopCount = 2, routeState = "none"),
            AnalyticsEvent.StopAdded(stopType = StopTypeParam.DESTINATION, stopCount = 2),
            AnalyticsEvent.RouteCalculated(
                stopCount = 2,
                legCount = 1,
                distanceBand = DistanceBand.UNDER_50KM,
                durationBand = DurationBand.UNDER_1H,
            ),
            AnalyticsEvent.NavigationLaunched(stopType = StopTypeParam.DESTINATION, stopPosition = 0),
            AnalyticsEvent.StopEdited(stopType = StopTypeParam.INTERMEDIATE),
            AnalyticsEvent.StopRemoved(stopType = StopTypeParam.INTERMEDIATE, stopCount = 1),
            AnalyticsEvent.StopReordered(direction = "up", stopCount = 3),
            AnalyticsEvent.StopDeparted(stopPosition = 0, stopCount = 2),
            AnalyticsEvent.StopDepartureUndone(stopPosition = 0),
            AnalyticsEvent.PlaceSearchPerformed(suggestionCount = 5),
            AnalyticsEvent.PlaceSuggestionSelected(suggestionPosition = 0),
            AnalyticsEvent.OperationFailed(
                operation = AnalyticsOperation.CREATE_TRIP,
                errorType = AnalyticsErrorType.NETWORK,
            ),
        )

        for (event in nonScreenEvents) {
            provider.logEvent(event)
            verify { firebaseAnalytics.logEvent(event.name, any()) }
        }
    }

    // ─── Section 5: ScreenViewed special-case mapping ─────────────────────────

    @Test
    fun `GIVEN ScreenViewed with TRIP_LIST WHEN logEvent is called THEN Firebase logEvent is invoked with SCREEN_VIEW not screen_viewed`() {
        val event = AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_LIST)

        provider.logEvent(event)

        verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
        verify(exactly = 0) { firebaseAnalytics.logEvent("screen_viewed", any()) }
    }

    @Test
    fun `GIVEN ScreenViewed with TRIP_DETAIL WHEN logEvent is called THEN Bundle contains trip_detail keyed by FirebaseAnalytics Param SCREEN_NAME`() {
        val event = AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_DETAIL)

        provider.logEvent(event)

        verify {
            anyConstructed<Bundle>().putString(
                FirebaseAnalytics.Param.SCREEN_NAME,
                AnalyticsScreen.TRIP_DETAIL.value,
            )
        }
    }

    // ─── Section 6: User property forwarding ─────────────────────────────────

    @Test
    fun `GIVEN TripCountBand with RANGE_2_5 WHEN setUserProperty is called THEN Firebase setUserProperty is invoked with trip_count_band and 2_5`() {
        val property = AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5)

        provider.setUserProperty(property)

        verify { firebaseAnalytics.setUserProperty("trip_count_band", "2_5") }
    }

    @Test
    fun `GIVEN DistanceUnit with IMPERIAL WHEN setUserProperty is called THEN Firebase setUserProperty is invoked with distance_unit and imperial`() {
        val property = AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL)

        provider.setUserProperty(property)

        verify { firebaseAnalytics.setUserProperty("distance_unit", "imperial") }
    }

    // ─── Section 7: trackException no-op ─────────────────────────────────────

    @Test
    fun `GIVEN a provider WHEN trackException is called THEN no method is invoked on FirebaseAnalytics and no exception propagates`() {
        val throwable = RuntimeException("boom")
        val operation = AnalyticsOperation.CREATE_TRIP

        provider.trackException(throwable, operation)

        verify { firebaseAnalytics wasNot io.mockk.Called }
    }

    // ─── Section 8: Platform-limit enforcement ────────────────────────────────

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with event name exceeding 40 characters THEN ISE is thrown and Firebase logEvent is not invoked`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val longNameEvent = mockk<AnalyticsEvent>()
        every { longNameEvent.name } returns "a".repeat(41)
        every { longNameEvent.properties } returns emptyMap()

        assertThrows(IllegalStateException::class.java) {
            debugProvider.logEvent(longNameEvent)
        }

        verify(exactly = 0) { firebaseAnalytics.logEvent(any(), any()) }
    }

    @Test
    fun `GIVEN debug mode false WHEN logEvent is called with event name exceeding 40 characters THEN no exception is thrown Firebase logEvent is not invoked and Log e is called`() {
        val releaseProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = false)
        val longNameEvent = mockk<AnalyticsEvent>()
        every { longNameEvent.name } returns "a".repeat(41)
        every { longNameEvent.properties } returns emptyMap()

        releaseProvider.logEvent(longNameEvent)

        verify(exactly = 0) { firebaseAnalytics.logEvent(any(), any()) }
        verify { Log.e(any(), any()) }
    }

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with event properties containing a reserved prefix key THEN ISE is thrown and Firebase logEvent is not invoked`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val reservedPrefixEvent = mockk<AnalyticsEvent>()
        every { reservedPrefixEvent.name } returns "valid_event"
        every { reservedPrefixEvent.properties } returns mapOf("firebase_key" to "value")

        assertThrows(IllegalStateException::class.java) {
            debugProvider.logEvent(reservedPrefixEvent)
        }

        verify(exactly = 0) { firebaseAnalytics.logEvent(any(), any()) }
    }

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with a String parameter value of exactly 100 characters THEN no exception is thrown and Firebase logEvent is invoked`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val event100Chars = mockk<AnalyticsEvent>()
        every { event100Chars.name } returns "valid_event"
        every { event100Chars.properties } returns mapOf("valid_param" to "a".repeat(100))

        debugProvider.logEvent(event100Chars)

        verify { firebaseAnalytics.logEvent("valid_event", any()) }
    }

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with a String parameter value of 101 characters THEN ISE is thrown`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val event101Chars = mockk<AnalyticsEvent>()
        every { event101Chars.name } returns "valid_event"
        every { event101Chars.properties } returns mapOf("valid_param" to "a".repeat(101))

        assertThrows(IllegalStateException::class.java) {
            debugProvider.logEvent(event101Chars)
        }
    }

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with exactly 25 parameters THEN no exception is thrown`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val event25Params = mockk<AnalyticsEvent>()
        every { event25Params.name } returns "valid_event"
        every { event25Params.properties } returns (1..25).associate { index -> "param$index" to "value" }

        debugProvider.logEvent(event25Params)

        verify { firebaseAnalytics.logEvent("valid_event", any()) }
    }

    @Test
    fun `GIVEN debug mode true WHEN logEvent is called with 26 parameters THEN ISE is thrown`() {
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val event26Params = mockk<AnalyticsEvent>()
        every { event26Params.name } returns "valid_event"
        every { event26Params.properties } returns (1..26).associate { index -> "param$index" to "value" }

        assertThrows(IllegalStateException::class.java) {
            debugProvider.logEvent(event26Params)
        }
    }

    @Test
    fun `GIVEN debug mode true WHEN setUserProperty is called with a mapped property name exceeding 24 characters THEN ISE is thrown and Firebase setUserProperty is not invoked`() {
        // Tests via the internal forwarding method since current taxonomy mapped names are < 24 chars.
        // validateAndSetUserProperty is the function called internally by setUserProperty after mapping.
        val debugProvider = FirebaseAnalyticsProvider(firebaseAnalytics, isDebugBuild = true)
        val longName = "a".repeat(25)

        assertThrows(IllegalStateException::class.java) {
            debugProvider.validateAndSetUserProperty(longName, "value")
        }

        verify(exactly = 0) { firebaseAnalytics.setUserProperty(any(), any()) }
    }

    // ─── Section 9: Thread safety ─────────────────────────────────────────────

    @Test
    fun `GIVEN a provider WHEN logEvent is invoked concurrently from multiple threads THEN every call reaches Firebase exactly once with no exceptions`() = runBlocking {
        val eventCount = 20
        val jobs = (0 until eventCount).map { index ->
            launch(Dispatchers.Default) {
                provider.logEvent(AnalyticsEvent.TripCreated(isFirstTrip = index % 2 == 0))
            }
        }
        jobs.forEach { it.join() }

        verify(exactly = eventCount) { firebaseAnalytics.logEvent(any(), any()) }
    }
}
