package com.guidovezzoni.venice.ui.screens.tripdetail

import android.content.res.Resources
import com.guidovezzoni.venice.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

private const val METRES_PATTERN = "%1\$d m"
private const val KILOMETRES_PATTERN = "%.1f km"
private const val MILES_PATTERN = "%.1f mi"

class LegSummaryFormatDistanceTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = mockk()
        every { resources.getString(R.string.trip_detail_leg_distance_metres, any()) } answers {
            val formatArgument = secondArg<Array<Any?>>().first()
            String.format(Locale.US, METRES_PATTERN, formatArgument)
        }
        every { resources.getString(R.string.trip_detail_leg_distance_kilometres, any()) } answers {
            val formatArgument = secondArg<Array<Any?>>().first()
            String.format(Locale.US, KILOMETRES_PATTERN, formatArgument)
        }
        every { resources.getString(R.string.trip_detail_leg_distance_miles, any()) } answers {
            val formatArgument = secondArg<Array<Any?>>().first()
            String.format(Locale.US, MILES_PATTERN, formatArgument)
        }
    }

    @Test
    fun `GIVEN distanceMetres below 1000 and a metric locale WHEN formatDistance is called THEN it returns the distance in metres`() {
        val distanceMetres = 450
        val locale = Locale.ITALY

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "450 m"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres above 1000 and a metric locale WHEN formatDistance is called THEN it returns the distance in kilometres with standard rounding`() {
        val distanceMetres = 12345
        val locale = Locale.ITALY

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "12.3 km"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres just below the kilometre boundary and a metric locale WHEN formatDistance is called THEN it returns the distance in metres`() {
        val distanceMetres = 999
        val locale = Locale.ITALY

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "999 m"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres exactly at the kilometre boundary and a metric locale WHEN formatDistance is called THEN it returns the distance in kilometres`() {
        val distanceMetres = 1000
        val locale = Locale.ITALY

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "1.0 km"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres and an imperial locale WHEN formatDistance is called THEN it returns the distance in miles`() {
        val distanceMetres = 12545
        val locale = Locale.US

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "7.8 mi"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a sub-mile distanceMetres and an imperial locale WHEN formatDistance is called THEN it returns the distance in miles without feet-based formatting`() {
        val distanceMetres = 483
        val locale = Locale.UK

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "0.3 mi"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres and locale country Liberia WHEN formatDistance is called THEN it returns the distance in miles`() {
        val distanceMetres = 12545
        val locale = Locale.Builder().setRegion("LR").build()

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "7.8 mi"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN distanceMetres and locale country Myanmar WHEN formatDistance is called THEN it returns the distance in miles`() {
        val distanceMetres = 12545
        val locale = Locale.Builder().setRegion("MM").build()

        val result = formatDistance(distanceMetres, locale, resources)

        val expected = "7.8 mi"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a metric locale WHEN isImperialLocale is called THEN it returns false`() {
        val locale = Locale.FRANCE

        val result = isImperialLocale(locale)

        assertFalse(result)
    }

    @Test
    fun `GIVEN an imperial locale WHEN isImperialLocale is called THEN it returns true`() {
        val locale = Locale.US

        val result = isImperialLocale(locale)

        assertTrue(result)
    }
}
