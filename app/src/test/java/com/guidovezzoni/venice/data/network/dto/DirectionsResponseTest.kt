package com.guidovezzoni.venice.data.network.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DirectionsResponseTest {

    @Test
    fun `GIVEN valid Routes API JSON WHEN fromJson is called THEN DirectionsResponse with correct legs is returned`() {
        val json = """
            {
              "routes": [{
                "legs": [
                  {
                    "distanceMeters": 12345,
                    "duration": "678s",
                    "polyline": {"encodedPolyline": "abc123"}
                  },
                  {
                    "distanceMeters": 5000,
                    "duration": "300s",
                    "polyline": {"encodedPolyline": "def456"}
                  }
                ]
              }]
            }
        """.trimIndent()

        val result = DirectionsResponse.fromJson(json)

        val expectedLegCount = 2
        assertEquals(expectedLegCount, result.legs.size)

        val expectedFirstDistanceMetres = 12345
        val expectedFirstDurationSeconds = 678
        assertEquals(expectedFirstDistanceMetres, result.legs[0].distanceMetres)
        assertEquals(expectedFirstDurationSeconds, result.legs[0].durationSeconds)
        assertEquals("abc123", result.legs[0].encodedPolyline)

        val expectedSecondDistanceMetres = 5000
        val expectedSecondDurationSeconds = 300
        assertEquals(expectedSecondDistanceMetres, result.legs[1].distanceMetres)
        assertEquals(expectedSecondDurationSeconds, result.legs[1].durationSeconds)
        assertEquals("def456", result.legs[1].encodedPolyline)
    }

    @Test
    fun `GIVEN JSON with empty routes WHEN fromJson is called THEN DirectionsResponse with empty legs is returned`() {
        val json = """{"routes": []}"""

        val result = DirectionsResponse.fromJson(json)

        assertTrue(result.legs.isEmpty())
    }

    @Test
    fun `GIVEN JSON with no routes key WHEN fromJson is called THEN DirectionsResponse with empty legs is returned`() {
        val json = """{}"""

        val result = DirectionsResponse.fromJson(json)

        assertTrue(result.legs.isEmpty())
    }

    @Test
    fun `GIVEN JSON with null legs array WHEN fromJson is called THEN DirectionsResponse with empty legs is returned`() {
        val json = """{"routes": [{}]}"""

        val result = DirectionsResponse.fromJson(json)

        assertTrue(result.legs.isEmpty())
    }

    @Test
    fun `GIVEN leg with blank duration WHEN fromJson is called THEN durationSeconds is null`() {
        val json = """
            {
              "routes": [{
                "legs": [
                  {
                    "distanceMeters": 100,
                    "duration": "",
                    "polyline": {"encodedPolyline": "xyz"}
                  }
                ]
              }]
            }
        """.trimIndent()

        val result = DirectionsResponse.fromJson(json)

        val expectedLegCount = 1
        assertEquals(expectedLegCount, result.legs.size)
        assertNull(result.legs[0].durationSeconds)
    }

    @Test
    fun `GIVEN leg with non-numeric duration WHEN fromJson is called THEN durationSeconds is null`() {
        val json = """
            {
              "routes": [{
                "legs": [
                  {
                    "distanceMeters": 100,
                    "duration": "notanumber",
                    "polyline": {"encodedPolyline": "xyz"}
                  }
                ]
              }]
            }
        """.trimIndent()

        val result = DirectionsResponse.fromJson(json)

        assertNull(result.legs[0].durationSeconds)
    }

    @Test
    fun `GIVEN leg without polyline object WHEN fromJson is called THEN encodedPolyline is null`() {
        val json = """
            {
              "routes": [{
                "legs": [
                  {
                    "distanceMeters": 100,
                    "duration": "60s"
                  }
                ]
              }]
            }
        """.trimIndent()

        val result = DirectionsResponse.fromJson(json)

        val expectedLegCount = 1
        assertEquals(expectedLegCount, result.legs.size)
        assertNull(result.legs[0].encodedPolyline)
    }

    @Test
    fun `GIVEN JSON with API error WHEN fromJson is called THEN IllegalStateException is thrown`() {
        val json = """
            {
              "error": {
                "code": 403,
                "message": "Routes API is not enabled",
                "status": "PERMISSION_DENIED"
              }
            }
        """.trimIndent()

        try {
            DirectionsResponse.fromJson(json)
            fail("Expected IllegalStateException")
        } catch (exception: IllegalStateException) {
            assertTrue(exception.message!!.contains("PERMISSION_DENIED"))
            assertTrue(exception.message!!.contains("Routes API is not enabled"))
        }
    }
}
