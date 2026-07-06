package com.guidovezzoni.venice.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CoordinateFormatterTest {

    @Test
    fun `GIVEN a positive double WHEN formatCoordinate THEN formats to 4 decimal places using default locale`() {
        val expected = String.format(Locale.getDefault(), "%.4f", 41.8902)
        val result = formatCoordinate(41.8902)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a negative double WHEN formatCoordinate THEN formats correctly using default locale`() {
        val expected = String.format(Locale.getDefault(), "%.4f", -41.8902)
        val result = formatCoordinate(-41.8902)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a whole number WHEN formatCoordinate THEN formats with 4 trailing zeros`() {
        val expected = String.format(Locale.getDefault(), "%.4f", 0.0)
        val result = formatCoordinate(0.0)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a value with more than 4 decimal places WHEN formatCoordinate THEN truncates to 4 places`() {
        val expected = String.format(Locale.getDefault(), "%.4f", 12.49220)
        val result = formatCoordinate(12.49220)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN latitude and longitude WHEN formatCoordinates THEN returns comma-space-separated pair`() {
        val expected = "${String.format(Locale.getDefault(), "%.4f", 41.9028)}, ${String.format(Locale.getDefault(), "%.4f", 12.4964)}"
        val result = formatCoordinates(41.9028, 12.4964)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN negative coordinates WHEN formatCoordinates THEN formats both values correctly`() {
        val expected = "${String.format(Locale.getDefault(), "%.4f", -33.8688)}, ${String.format(Locale.getDefault(), "%.4f", 151.2093)}"
        val result = formatCoordinates(-33.8688, 151.2093)
        assertEquals(expected, result)
    }
}
