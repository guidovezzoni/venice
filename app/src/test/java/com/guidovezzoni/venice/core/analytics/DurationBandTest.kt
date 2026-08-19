package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationBandTest {

    @Test
    fun givenZeroSeconds_whenFromSeconds_thenReturnsUnder1H() {
        val expected = DurationBand.UNDER_1H
        val actual = DurationBand.fromSeconds(0)
        assertEquals(expected, actual)
    }

    @Test
    fun given3599Seconds_whenFromSeconds_thenReturnsUnder1H() {
        val expected = DurationBand.UNDER_1H
        val actual = DurationBand.fromSeconds(3_599)
        assertEquals(expected, actual)
    }

    @Test
    fun given3600Seconds_whenFromSeconds_thenReturnsRange1To3H() {
        val expected = DurationBand.RANGE_1_3H
        val actual = DurationBand.fromSeconds(3_600)
        assertEquals(expected, actual)
    }

    @Test
    fun given10799Seconds_whenFromSeconds_thenReturnsRange1To3H() {
        val expected = DurationBand.RANGE_1_3H
        val actual = DurationBand.fromSeconds(10_799)
        assertEquals(expected, actual)
    }

    @Test
    fun given10800Seconds_whenFromSeconds_thenReturnsRange3To6H() {
        val expected = DurationBand.RANGE_3_6H
        val actual = DurationBand.fromSeconds(10_800)
        assertEquals(expected, actual)
    }

    @Test
    fun given21599Seconds_whenFromSeconds_thenReturnsRange3To6H() {
        val expected = DurationBand.RANGE_3_6H
        val actual = DurationBand.fromSeconds(21_599)
        assertEquals(expected, actual)
    }

    @Test
    fun given21600Seconds_whenFromSeconds_thenReturnsRange6To12H() {
        val expected = DurationBand.RANGE_6_12H
        val actual = DurationBand.fromSeconds(21_600)
        assertEquals(expected, actual)
    }

    @Test
    fun given43199Seconds_whenFromSeconds_thenReturnsRange6To12H() {
        val expected = DurationBand.RANGE_6_12H
        val actual = DurationBand.fromSeconds(43_199)
        assertEquals(expected, actual)
    }

    @Test
    fun given43200Seconds_whenFromSeconds_thenReturnsOver12H() {
        val expected = DurationBand.OVER_12H
        val actual = DurationBand.fromSeconds(43_200)
        assertEquals(expected, actual)
    }

    @Test
    fun given100000Seconds_whenFromSeconds_thenReturnsOver12H() {
        val expected = DurationBand.OVER_12H
        val actual = DurationBand.fromSeconds(100_000)
        assertEquals(expected, actual)
    }
}
