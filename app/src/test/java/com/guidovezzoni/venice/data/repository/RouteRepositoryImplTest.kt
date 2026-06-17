package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.LegDao
import com.guidovezzoni.venice.data.database.entity.LegEntity
import com.guidovezzoni.venice.data.network.DirectionsApiService
import com.guidovezzoni.venice.data.network.dto.DirectionsLeg
import com.guidovezzoni.venice.data.network.dto.DirectionsResponse
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"
private const val STOP_ID_1 = "stop-1"
private const val STOP_ID_2 = "stop-2"
private const val STOP_ID_3 = "stop-3"
private const val LEG_ID_1 = "leg-1"
private const val LEG_ID_2 = "leg-2"
private const val DISTANCE_METRES_1 = 10000
private const val DURATION_SECONDS_1 = 600
private const val ENCODED_POLYLINE_1 = "polyline1"
private const val DISTANCE_METRES_2 = 20000
private const val DURATION_SECONDS_2 = 1200
private const val ENCODED_POLYLINE_2 = "polyline2"

class RouteRepositoryImplTest {

    private lateinit var directionsApiService: DirectionsApiService
    private lateinit var legDao: LegDao
    private lateinit var sut: RouteRepositoryImpl

    @Before
    fun setUp() {
        directionsApiService = mockk()
        legDao = mockk()
        sut = RouteRepositoryImpl(directionsApiService, legDao)
    }

    @Test
    fun `GIVEN 3 stops and API returns 2 legs WHEN calculateRoute is called THEN existing legs are deleted and 2 new legs are persisted`() = runTest {
        val stops = listOf(
            Stop(
                id = STOP_ID_1,
                tripId = TRIP_ID,
                placeName = "Rome",
                latitude = 41.9028,
                longitude = 12.4964,
                order = 0,
                status = StopStatus.PENDING,
            ),
            Stop(
                id = STOP_ID_2,
                tripId = TRIP_ID,
                placeName = "Florence",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 1,
                status = StopStatus.PENDING,
            ),
            Stop(
                id = STOP_ID_3,
                tripId = TRIP_ID,
                placeName = "Milan",
                latitude = 45.4642,
                longitude = 9.1900,
                order = 2,
                status = StopStatus.PENDING,
            ),
        )
        val directionsLeg1 = DirectionsLeg(
            distanceMetres = DISTANCE_METRES_1,
            durationSeconds = DURATION_SECONDS_1,
            encodedPolyline = ENCODED_POLYLINE_1,
        )
        val directionsLeg2 = DirectionsLeg(
            distanceMetres = DISTANCE_METRES_2,
            durationSeconds = DURATION_SECONDS_2,
            encodedPolyline = ENCODED_POLYLINE_2,
        )
        val directionsResponse = DirectionsResponse(legs = listOf(directionsLeg1, directionsLeg2))
        coEvery { directionsApiService.fetchRoute(stops) } returns Result.success(directionsResponse)
        coJustRun { legDao.deleteByTripId(TRIP_ID) }
        coJustRun { legDao.insertAll(any()) }

        val result = sut.calculateRoute(TRIP_ID, stops)

        assertTrue(result.isSuccess)
        coVerifyOrder {
            legDao.deleteByTripId(TRIP_ID)
            legDao.insertAll(any())
        }
        coVerify(exactly = 1) { legDao.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `GIVEN API failure WHEN calculateRoute is called THEN Result failure is returned`() = runTest {
        val stops = listOf(
            Stop(
                id = STOP_ID_1,
                tripId = TRIP_ID,
                placeName = "Rome",
                latitude = 41.9028,
                longitude = 12.4964,
                order = 0,
                status = StopStatus.PENDING,
            ),
            Stop(
                id = STOP_ID_2,
                tripId = TRIP_ID,
                placeName = "Florence",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 1,
                status = StopStatus.PENDING,
            ),
        )
        val exception = RuntimeException("Network error")
        coEvery { directionsApiService.fetchRoute(stops) } returns Result.failure(exception)

        val result = sut.calculateRoute(TRIP_ID, stops)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 0) { legDao.deleteByTripId(any()) }
        coVerify(exactly = 0) { legDao.insertAll(any()) }
    }

    @Test
    fun `GIVEN legs exist WHEN deleteLegsForTrip is called THEN legs are deleted`() = runTest {
        coJustRun { legDao.deleteByTripId(TRIP_ID) }

        val result = sut.deleteLegsForTrip(TRIP_ID)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { legDao.deleteByTripId(TRIP_ID) }
    }

    @Test
    fun `GIVEN legs exist WHEN observeLegsForTrip is called THEN Flow emits mapped domain legs`() = runTest {
        val entity1 = LegEntity(
            id = LEG_ID_1,
            tripId = TRIP_ID,
            fromStopId = STOP_ID_1,
            toStopId = STOP_ID_2,
            distanceMetres = DISTANCE_METRES_1,
            durationSeconds = DURATION_SECONDS_1,
            encodedPolyline = ENCODED_POLYLINE_1,
        )
        val entity2 = LegEntity(
            id = LEG_ID_2,
            tripId = TRIP_ID,
            fromStopId = STOP_ID_2,
            toStopId = STOP_ID_3,
            distanceMetres = DISTANCE_METRES_2,
            durationSeconds = DURATION_SECONDS_2,
            encodedPolyline = ENCODED_POLYLINE_2,
        )
        every { legDao.observeByTripId(TRIP_ID) } returns flowOf(listOf(entity1, entity2))

        val emissions = sut.observeLegsForTrip(TRIP_ID).toList()

        val expectedLeg1 = Leg(
            id = LEG_ID_1,
            tripId = TRIP_ID,
            fromStopId = STOP_ID_1,
            toStopId = STOP_ID_2,
            distanceMetres = DISTANCE_METRES_1,
            durationSeconds = DURATION_SECONDS_1,
            encodedPolyline = ENCODED_POLYLINE_1,
        )
        val expectedLeg2 = Leg(
            id = LEG_ID_2,
            tripId = TRIP_ID,
            fromStopId = STOP_ID_2,
            toStopId = STOP_ID_3,
            distanceMetres = DISTANCE_METRES_2,
            durationSeconds = DURATION_SECONDS_2,
            encodedPolyline = ENCODED_POLYLINE_2,
        )
        val expectedEmissions = listOf(listOf(expectedLeg1, expectedLeg2))
        assertEquals(expectedEmissions, emissions)
    }
}
