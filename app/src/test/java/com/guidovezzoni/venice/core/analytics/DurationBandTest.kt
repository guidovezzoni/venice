package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationBandTest {

    @Test
    fun `GIVEN negative seconds WHEN fromSeconds is called THEN returns UNDER_1H`() {
        val expected = DurationBand.UNDER_1H
        val actual = DurationBand.fromSeconds(-1)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 0 seconds WHEN fromSeconds is called THEN returns UNDER_1H`() {
        val expected = DurationBand.UNDER_1H
        val actual = DurationBand.fromSeconds(0)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 3599 seconds WHEN fromSeconds is called THEN returns UNDER_1H`() {
        val expected = DurationBand.UNDER_1H
        val actual = DurationBand.fromSeconds(3_599)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 3600 seconds WHEN fromSeconds is called THEN returns RANGE_1_3H`() {
        val expected = DurationBand.RANGE_1_3H
        val actual = DurationBand.fromSeconds(3_600)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 10799 seconds WHEN fromSeconds is called THEN returns RANGE_1_3H`() {
        val expected = DurationBand.RANGE_1_3H
        val actual = DurationBand.fromSeconds(10_799)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 10800 seconds WHEN fromSeconds is called THEN returns RANGE_3_6H`() {
        val expected = DurationBand.RANGE_3_6H
        val actual = DurationBand.fromSeconds(10_800)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 21599 seconds WHEN fromSeconds is called THEN returns RANGE_3_6H`() {
        val expected = DurationBand.RANGE_3_6H
        val actual = DurationBand.fromSeconds(21_599)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 21600 seconds WHEN fromSeconds is called THEN returns RANGE_6_12H`() {
        val expected = DurationBand.RANGE_6_12H
        val actual = DurationBand.fromSeconds(21_600)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 43199 seconds WHEN fromSeconds is called THEN returns RANGE_6_12H`() {
        val expected = DurationBand.RANGE_6_12H
        val actual = DurationBand.fromSeconds(43_199)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 43200 seconds WHEN fromSeconds is called THEN returns OVER_12H`() {
        val expected = DurationBand.OVER_12H
        val actual = DurationBand.fromSeconds(43_200)
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN 100000 seconds WHEN fromSeconds is called THEN returns OVER_12H`() {
        val expected = DurationBand.OVER_12H
        val actual = DurationBand.fromSeconds(100_000)
        assertEquals(expected, actual)
    }
}
