package com.guidovezzoni.venice.data.analytics

import com.guidovezzoni.venice.domain.analytics.AnalyticsEvent
import com.guidovezzoni.venice.domain.analytics.AnalyticsProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CompositeAnalyticsTrackerTest {

    private val event = AnalyticsEvent.TripCreated(tripId = "trip-1")

    @Test
    fun `GIVEN a provider that accepts all events WHEN track is called THEN provider track is called`() {
        val provider = mockk<AnalyticsProvider>(relaxed = true)
        every { provider.shouldTrack(any()) } returns true
        val tracker = CompositeAnalyticsTracker(setOf(provider))

        tracker.track(event)

        verify(exactly = 1) { provider.track(event) }
    }

    @Test
    fun `GIVEN a provider that rejects all events WHEN track is called THEN provider track is not called`() {
        val provider = mockk<AnalyticsProvider>(relaxed = true)
        every { provider.shouldTrack(any()) } returns false
        val tracker = CompositeAnalyticsTracker(setOf(provider))

        tracker.track(event)

        verify(exactly = 0) { provider.track(any()) }
    }

    @Test
    fun `GIVEN two providers one accepting one rejecting WHEN track is called THEN only accepting provider is called`() {
        val acceptingProvider = mockk<AnalyticsProvider>(relaxed = true)
        every { acceptingProvider.shouldTrack(any()) } returns true
        val rejectingProvider = mockk<AnalyticsProvider>(relaxed = true)
        every { rejectingProvider.shouldTrack(any()) } returns false
        val tracker = CompositeAnalyticsTracker(setOf(acceptingProvider, rejectingProvider))

        tracker.track(event)

        verify(exactly = 1) { acceptingProvider.track(event) }
        verify(exactly = 0) { rejectingProvider.track(any()) }
    }

    @Test
    fun `GIVEN an empty provider set WHEN track is called THEN no exception is thrown`() {
        val tracker = CompositeAnalyticsTracker(emptySet())

        tracker.track(event)
    }

    @Test
    fun `GIVEN multiple accepting providers WHEN track is called THEN all providers are called`() {
        val providerOne = mockk<AnalyticsProvider>(relaxed = true)
        every { providerOne.shouldTrack(any()) } returns true
        val providerTwo = mockk<AnalyticsProvider>(relaxed = true)
        every { providerTwo.shouldTrack(any()) } returns true
        val tracker = CompositeAnalyticsTracker(setOf(providerOne, providerTwo))

        tracker.track(event)

        verify(exactly = 1) { providerOne.track(event) }
        verify(exactly = 1) { providerTwo.track(event) }
    }
}
