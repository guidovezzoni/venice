package com.guidovezzoni.venice.data.mapper

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.guidovezzoni.venice.domain.model.PlaceDetail
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val PLACE_NAME = "Colosseum"
private const val LATITUDE = 41.8902
private const val LONGITUDE = 12.4922

class PlaceDetailMapperTest {

    private lateinit var mapper: PlaceDetailMapper

    @Before
    fun setUp() {
        mapper = PlaceDetailMapper()
    }

    @Test
    fun `GIVEN a Place with displayName and location WHEN mapped THEN returns PlaceDetail with correct fields`() {
        val location = LatLng(LATITUDE, LONGITUDE)
        val place = mockk<Place>(relaxed = true)
        every { place.displayName } returns PLACE_NAME
        every { place.location } returns location

        val result = mapper.map(place)

        val expected = PlaceDetail(
            name = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
        )
        assertEquals(expected, result)
    }

    @Test(expected = IllegalStateException::class)
    fun `GIVEN a Place with null displayName WHEN mapped THEN throws IllegalStateException`() {
        val place = mockk<Place>(relaxed = true)
        every { place.displayName } returns null
        every { place.location } returns LatLng(LATITUDE, LONGITUDE)

        mapper.map(place)
    }

    @Test(expected = IllegalStateException::class)
    fun `GIVEN a Place with null location WHEN mapped THEN throws IllegalStateException`() {
        val place = mockk<Place>(relaxed = true)
        every { place.displayName } returns PLACE_NAME
        every { place.location } returns null

        mapper.map(place)
    }
}
