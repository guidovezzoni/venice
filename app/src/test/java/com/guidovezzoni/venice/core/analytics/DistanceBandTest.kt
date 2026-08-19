package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceBandTest {

    @Test
    fun givenZeroMetres_whenFromMetres_thenReturnsUnder50Km() {
        val expected = DistanceBand.UNDER_50KM
        val actual = DistanceBand.fromMetres(0)
        assertEquals(expected, actual)
    }

    @Test
    fun given49999Metres_whenFromMetres_thenReturnsUnder50Km() {
        val expected = DistanceBand.UNDER_50KM
        val actual = DistanceBand.fromMetres(49_999)
        assertEquals(expected, actual)
    }

    @Test
    fun given50000Metres_whenFromMetres_thenReturnsRange50To200Km() {
        val expected = DistanceBand.RANGE_50_200KM
        val actual = DistanceBand.fromMetres(50_000)
        assertEquals(expected, actual)
    }

    @Test
    fun given199999Metres_whenFromMetres_thenReturnsRange50To200Km() {
        val expected = DistanceBand.RANGE_50_200KM
        val actual = DistanceBand.fromMetres(199_999)
        assertEquals(expected, actual)
    }

    @Test
    fun given200000Metres_whenFromMetres_thenReturnsRange200To500Km() {
        val expected = DistanceBand.RANGE_200_500KM
        val actual = DistanceBand.fromMetres(200_000)
        assertEquals(expected, actual)
    }

    @Test
    fun given999999Metres_whenFromMetres_thenReturnsRange500To1000Km() {
        val expected = DistanceBand.RANGE_500_1000KM
        val actual = DistanceBand.fromMetres(999_999)
        assertEquals(expected, actual)
    }

    @Test
    fun given1000000Metres_whenFromMetres_thenReturnsOver1000Km() {
        val expected = DistanceBand.OVER_1000KM
        val actual = DistanceBand.fromMetres(1_000_000)
        assertEquals(expected, actual)
    }

    @Test
    fun given2000000Metres_whenFromMetres_thenReturnsOver1000Km() {
        val expected = DistanceBand.OVER_1000KM
        val actual = DistanceBand.fromMetres(2_000_000)
        assertEquals(expected, actual)
    }
}
