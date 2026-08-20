package com.guidovezzoni.venice.core.analytics

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CompositeAnalyticsClientTest {

    private val event = AnalyticsEvent.TripCreated(isFirstTrip = true)
    private val operation = AnalyticsOperation.CREATE_TRIP
    private val throwable = RuntimeException("test error")
    private val userProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.ZERO)

    private lateinit var providerOne: AnalyticsProvider
    private lateinit var providerTwo: AnalyticsProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        providerOne = mockk<AnalyticsProvider>()
        providerTwo = mockk<AnalyticsProvider>()
    }

    // Section 2 — event fan-out

    @Test
    fun `GIVEN two registered providers WHEN logEvent is called THEN both providers receive logEvent with the same event instance`() {
        every { providerOne.logEvent(any()) } just Runs
        every { providerTwo.logEvent(any()) } just Runs
        val client = CompositeAnalyticsClient(setOf(providerOne, providerTwo))

        client.logEvent(event)

        verify(exactly = 1) { providerOne.logEvent(event) }
        verify(exactly = 1) { providerTwo.logEvent(event) }
    }

    // Section 3 — user property fan-out

    @Test
    fun `GIVEN two registered providers WHEN setUserProperty is called THEN both providers receive setUserProperty with the same property instance`() {
        every { providerOne.setUserProperty(any()) } just Runs
        every { providerTwo.setUserProperty(any()) } just Runs
        val client = CompositeAnalyticsClient(setOf(providerOne, providerTwo))

        client.setUserProperty(userProperty)

        verify(exactly = 1) { providerOne.setUserProperty(userProperty) }
        verify(exactly = 1) { providerTwo.setUserProperty(userProperty) }
    }

    // Section 4 — exception fan-out

    @Test
    fun `GIVEN two registered providers WHEN trackException is called THEN both providers receive trackException with the same throwable and operation`() {
        every { providerOne.trackException(any(), any()) } just Runs
        every { providerTwo.trackException(any(), any()) } just Runs
        val client = CompositeAnalyticsClient(setOf(providerOne, providerTwo))

        client.trackException(throwable, operation)

        verify(exactly = 1) { providerOne.trackException(throwable, operation) }
        verify(exactly = 1) { providerTwo.trackException(throwable, operation) }
    }

    // Section 5 — zero-provider no-op

    @Test
    fun `GIVEN zero registered providers WHEN logEvent is called THEN no exception is thrown and no provider interaction occurs`() {
        val client = CompositeAnalyticsClient(emptySet())

        client.logEvent(event)
    }

    @Test
    fun `GIVEN zero registered providers WHEN setUserProperty is called THEN no exception is thrown and no provider interaction occurs`() {
        val client = CompositeAnalyticsClient(emptySet())

        client.setUserProperty(userProperty)
    }

    @Test
    fun `GIVEN zero registered providers WHEN trackException is called THEN no exception is thrown and no provider interaction occurs`() {
        val client = CompositeAnalyticsClient(emptySet())

        client.trackException(throwable, operation)
    }
}
