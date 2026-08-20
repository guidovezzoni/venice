package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class CountBandTest {

    @Test
    fun `GIVEN a negative count WHEN fromCount is called THEN returns ZERO`() {
        val expected = CountBand.ZERO
        val actual = CountBand.fromCount(-1)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a count of 0 WHEN fromCount is called THEN returns ZERO`() {
        val expected = CountBand.ZERO
        val actual = CountBand.fromCount(0)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a count of 1 WHEN fromCount is called THEN returns ONE`() {
        val expected = CountBand.ONE
        val actual = CountBand.fromCount(1)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a count of 2 WHEN fromCount is called THEN returns RANGE_2_5`() {
        val expected = CountBand.RANGE_2_5
        val actual = CountBand.fromCount(2)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a count of 5 WHEN fromCount is called THEN returns RANGE_2_5`() {
        val expected = CountBand.RANGE_2_5
        val actual = CountBand.fromCount(5)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a count of 6 WHEN fromCount is called THEN returns SIX_PLUS`() {
        val expected = CountBand.SIX_PLUS
        val actual = CountBand.fromCount(6)
        assertEquals(expected, actual)
    }
}
