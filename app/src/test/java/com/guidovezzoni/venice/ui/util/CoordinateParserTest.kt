package com.guidovezzoni.venice.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun `GIVEN coordinate with dot decimal separator WHEN parseCoordinate is called THEN returns correct double`() {
        val result = parseCoordinate("41.9028")

        val expected = 41.9028
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN coordinate with comma decimal separator WHEN parseCoordinate is called THEN returns correct double`() {
        val result = parseCoordinate("41,9028")

        val expected = 41.9028
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN negative coordinate with dot WHEN parseCoordinate is called THEN returns correct negative double`() {
        val result = parseCoordinate("-90.0")

        val expected = -90.0
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN negative coordinate with comma WHEN parseCoordinate is called THEN returns correct negative double`() {
        val result = parseCoordinate("-180,0000")

        val expected = -180.0
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN integer coordinate WHEN parseCoordinate is called THEN returns correct double`() {
        val result = parseCoordinate("0")

        val expected = 0.0
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN coordinate with surrounding whitespace WHEN parseCoordinate is called THEN returns correct double`() {
        val result = parseCoordinate("  51.5074  ")

        val expected = 51.5074
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN empty string WHEN parseCoordinate is called THEN returns null`() {
        val result = parseCoordinate("")

        assertNull(result)
    }

    @Test
    fun `GIVEN blank string WHEN parseCoordinate is called THEN returns null`() {
        val result = parseCoordinate("   ")

        assertNull(result)
    }

    @Test
    fun `GIVEN non-numeric string WHEN parseCoordinate is called THEN returns null`() {
        val result = parseCoordinate("abc")

        assertNull(result)
    }

    @Test
    fun `GIVEN string with letters mixed in WHEN parseCoordinate is called THEN returns null`() {
        val result = parseCoordinate("41.90abc")

        assertNull(result)
    }
}
