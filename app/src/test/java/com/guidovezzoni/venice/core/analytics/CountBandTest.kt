package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class CountBandTest {

    @Test
    fun givenZeroCount_whenFromCount_thenReturnsZero() {
        val expected = CountBand.ZERO
        val actual = CountBand.fromCount(0)
        assertEquals(expected, actual)
    }

    @Test
    fun givenOneCount_whenFromCount_thenReturnsOne() {
        val expected = CountBand.ONE
        val actual = CountBand.fromCount(1)
        assertEquals(expected, actual)
    }

    @Test
    fun givenTwoCount_whenFromCount_thenReturnsRange2To5() {
        val expected = CountBand.RANGE_2_5
        val actual = CountBand.fromCount(2)
        assertEquals(expected, actual)
    }

    @Test
    fun givenFiveCount_whenFromCount_thenReturnsRange2To5() {
        val expected = CountBand.RANGE_2_5
        val actual = CountBand.fromCount(5)
        assertEquals(expected, actual)
    }

    @Test
    fun givenSixCount_whenFromCount_thenReturnsSixPlus() {
        val expected = CountBand.SIX_PLUS
        val actual = CountBand.fromCount(6)
        assertEquals(expected, actual)
    }
}
