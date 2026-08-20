package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceBandTest {

    @Test
    fun `GIVEN negative metres WHEN fromMetres is called THEN returns UNDER_50KM`() {
        val expected = DistanceBand.UNDER_50KM
        val actual = DistanceBand.fromMetres(-1)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 0 metres WHEN fromMetres is called THEN returns UNDER_50KM`() {
        val expected = DistanceBand.UNDER_50KM
        val actual = DistanceBand.fromMetres(0)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 49999 metres WHEN fromMetres is called THEN returns UNDER_50KM`() {
        val expected = DistanceBand.UNDER_50KM
        val actual = DistanceBand.fromMetres(49_999)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 50000 metres WHEN fromMetres is called THEN returns RANGE_50_200KM`() {
        val expected = DistanceBand.RANGE_50_200KM
        val actual = DistanceBand.fromMetres(50_000)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 199999 metres WHEN fromMetres is called THEN returns RANGE_50_200KM`() {
        val expected = DistanceBand.RANGE_50_200KM
        val actual = DistanceBand.fromMetres(199_999)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 200000 metres WHEN fromMetres is called THEN returns RANGE_200_500KM`() {
        val expected = DistanceBand.RANGE_200_500KM
        val actual = DistanceBand.fromMetres(200_000)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 999999 metres WHEN fromMetres is called THEN returns RANGE_500_1000KM`() {
        val expected = DistanceBand.RANGE_500_1000KM
        val actual = DistanceBand.fromMetres(999_999)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 1000000 metres WHEN fromMetres is called THEN returns OVER_1000KM`() {
        val expected = DistanceBand.OVER_1000KM
        val actual = DistanceBand.fromMetres(1_000_000)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 2000000 metres WHEN fromMetres is called THEN returns OVER_1000KM`() {
        val expected = DistanceBand.OVER_1000KM
        val actual = DistanceBand.fromMetres(2_000_000)
        assertEquals(expected, actual)
    }
}
