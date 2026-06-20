package com.guidovezzoni.venice.data.network

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

private const val TRIP_ID = "trip-1"
private const val ORIGIN_LATITUDE = 41.9028
private const val ORIGIN_LONGITUDE = 12.4964
private const val DESTINATION_LATITUDE = 43.7696
private const val DESTINATION_LONGITUDE = 11.2558
private const val INTERMEDIATE_LATITUDE = 42.5
private const val INTERMEDIATE_LONGITUDE = 12.0
private const val EXPECTED_DISTANCE_METRES = 12345
private const val EXPECTED_DURATION_SECONDS = 678
private const val HTTP_OK = 200
private const val HTTP_SERVER_ERROR = 500

class DirectionsApiServiceTest {

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var sut: DirectionsApiService

    @Before
    fun setUp() {
        okHttpClient = mockk()
        sut = DirectionsApiService(okHttpClient)

        val mapsApiKeyField: Field = DirectionsApiService::class.java.getDeclaredField("mapsApiKey")
        mapsApiKeyField.isAccessible = true
        mapsApiKeyField.set(sut, "test-api-key")
    }

    @Test
    fun `GIVEN 2 stops and API returns success WHEN fetchRoute is called THEN Result with parsed legs is returned`() = runTest {
        val responseJson = """
            {
              "routes": [{
                "legs": [
                  {
                    "distanceMeters": $EXPECTED_DISTANCE_METRES,
                    "duration": "${EXPECTED_DURATION_SECONDS}s",
                    "polyline": {"encodedPolyline": "abc123"}
                  }
                ]
              }]
            }
        """.trimIndent()
        stubOkHttpResponse(HTTP_OK, responseJson)

        val stops = createTwoStops()
        val result = sut.fetchRoute(stops)

        assertTrue(result.isSuccess)
        val expectedLegCount = 1
        assertEquals(expectedLegCount, result.getOrThrow().legs.size)
        assertEquals(EXPECTED_DISTANCE_METRES, result.getOrThrow().legs[0].distanceMetres)
        assertEquals(EXPECTED_DURATION_SECONDS, result.getOrThrow().legs[0].durationSeconds)
        assertEquals("abc123", result.getOrThrow().legs[0].encodedPolyline)
    }

    @Test
    fun `GIVEN API returns error status code WHEN fetchRoute is called THEN Result failure is returned`() = runTest {
        stubOkHttpResponse(HTTP_SERVER_ERROR, "Internal Server Error")

        val stops = createTwoStops()
        val result = sut.fetchRoute(stops)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `GIVEN fewer than 2 stops WHEN fetchRoute is called THEN Result failure with requirement error is returned`() = runTest {
        val singleStop = listOf(
            Stop(
                id = "stop-1",
                tripId = TRIP_ID,
                placeName = "Rome",
                latitude = ORIGIN_LATITUDE,
                longitude = ORIGIN_LONGITUDE,
                order = 0,
                status = StopStatus.PENDING,
            ),
        )

        val result = sut.fetchRoute(singleStop)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `GIVEN 3 stops with intermediates and API returns success WHEN fetchRoute is called THEN all legs are parsed`() = runTest {
        val responseJson = """
            {
              "routes": [{
                "legs": [
                  {"distanceMeters": 100, "duration": "60s", "polyline": {"encodedPolyline": "p1"}},
                  {"distanceMeters": 200, "duration": "120s", "polyline": {"encodedPolyline": "p2"}}
                ]
              }]
            }
        """.trimIndent()
        stubOkHttpResponse(HTTP_OK, responseJson)

        val stops = listOf(
            Stop(
                id = "stop-1",
                tripId = TRIP_ID,
                placeName = "Rome",
                latitude = ORIGIN_LATITUDE,
                longitude = ORIGIN_LONGITUDE,
                order = 0,
                status = StopStatus.PENDING,
            ),
            Stop(
                id = "stop-2",
                tripId = TRIP_ID,
                placeName = "Orvieto",
                latitude = INTERMEDIATE_LATITUDE,
                longitude = INTERMEDIATE_LONGITUDE,
                order = 1,
                status = StopStatus.PENDING,
            ),
            Stop(
                id = "stop-3",
                tripId = TRIP_ID,
                placeName = "Florence",
                latitude = DESTINATION_LATITUDE,
                longitude = DESTINATION_LONGITUDE,
                order = 2,
                status = StopStatus.PENDING,
            ),
        )
        val result = sut.fetchRoute(stops)

        assertTrue(result.isSuccess)
        val expectedLegCount = 2
        assertEquals(expectedLegCount, result.getOrThrow().legs.size)
    }

    @Test
    fun `GIVEN API returns empty response body WHEN fetchRoute is called THEN Result failure is returned`() = runTest {
        val call = mockk<Call>()
        every { okHttpClient.newCall(any()) } returns call
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(HTTP_OK)
            .message("OK")
            .body(null)
            .build()
        every { call.execute() } returns response

        val stops = createTwoStops()
        val result = sut.fetchRoute(stops)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `GIVEN network throws IOException WHEN fetchRoute is called THEN Result failure is returned`() = runTest {
        val call = mockk<Call>()
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } throws java.io.IOException("Connection refused")

        val stops = createTwoStops()
        val result = sut.fetchRoute(stops)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.io.IOException)
    }

    private fun stubOkHttpResponse(code: Int, body: String) {
        val call = mockk<Call>()
        every { okHttpClient.newCall(any()) } returns call
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == HTTP_OK) "OK" else "Error")
            .body(body.toResponseBody())
            .build()
        every { call.execute() } returns response
    }

    private fun createTwoStops(): List<Stop> = listOf(
        Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = "Rome",
            latitude = ORIGIN_LATITUDE,
            longitude = ORIGIN_LONGITUDE,
            order = 0,
            status = StopStatus.PENDING,
        ),
        Stop(
            id = "stop-2",
            tripId = TRIP_ID,
            placeName = "Florence",
            latitude = DESTINATION_LATITUDE,
            longitude = DESTINATION_LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        ),
    )
}
