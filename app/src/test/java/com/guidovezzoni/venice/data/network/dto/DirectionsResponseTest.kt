package com.guidovezzoni.venice.data.network.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DirectionsResponseTest {

    @Test
    fun `GIVEN valid Directions API JSON WHEN fromJson is called THEN DirectionsResponse with correct legs is returned`() {
        val json = """
            {
              "status": "OK",
              "routes": [{
                "legs": [
                  {
                    "distance": {"value": 12345},
                    "duration": {"value": 678}
                  },
                  {
                    "distance": {"value": 5000},
                    "duration": {"value": 300}
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

        val expectedSecondDistanceMetres = 5000
        val expectedSecondDurationSeconds = 300
        assertEquals(expectedSecondDistanceMetres, result.legs[1].distanceMetres)
        assertEquals(expectedSecondDurationSeconds, result.legs[1].durationSeconds)
    }

    @Test
    fun `GIVEN JSON with empty routes WHEN fromJson is called THEN DirectionsResponse with empty legs is returned`() {
        val json = """{"status": "OK", "routes": []}"""

        val result = DirectionsResponse.fromJson(json)

        assertTrue(result.legs.isEmpty())
    }

    @Test
    fun `GIVEN JSON with REQUEST_DENIED status WHEN fromJson is called THEN IllegalStateException is thrown`() {
        val json = """
            {
              "status": "REQUEST_DENIED",
              "error_message": "API not enabled",
              "routes": []
            }
        """.trimIndent()

        try {
            DirectionsResponse.fromJson(json)
            fail("Expected IllegalStateException")
        } catch (exception: IllegalStateException) {
            assertTrue(exception.message!!.contains("REQUEST_DENIED"))
        }
    }
}
