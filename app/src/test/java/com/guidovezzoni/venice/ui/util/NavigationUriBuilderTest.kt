package com.guidovezzoni.venice.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URLEncoder

private const val SIMPLE_PLACE_NAME = "Colosseum"
private const val SPECIAL_PLACE_NAME = "Sant'Angelo (Old Town)"
private const val TEST_LATITUDE = 41.9028
private const val TEST_LONGITUDE = 12.4964
private const val URL_ENCODER_CHARSET = "UTF-8"
private const val PLUS_SIGN = "+"
private const val PERCENT_ENCODED_SPACE = "%20"

class NavigationUriBuilderTest {

    @Test
    fun `GIVEN valid latitude longitude and a simple place name WHEN buildGeoUri is called THEN the result matches the geo URI format exactly`() {
        val placeName = SIMPLE_PLACE_NAME

        val result = buildGeoUri(TEST_LATITUDE, TEST_LONGITUDE, placeName)

        val expected = "geo:$TEST_LATITUDE,$TEST_LONGITUDE?q=$TEST_LATITUDE,$TEST_LONGITUDE($placeName)"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a place name containing spaces and parentheses WHEN buildGeoUri is called THEN the place name segment is percent-encoded and the overall URI remains well-formed`() {
        val placeName = SPECIAL_PLACE_NAME

        val result = buildGeoUri(TEST_LATITUDE, TEST_LONGITUDE, placeName)

        val encodedPlaceName = URLEncoder.encode(placeName, URL_ENCODER_CHARSET)
            .replace(PLUS_SIGN, PERCENT_ENCODED_SPACE)
        val expected = "geo:$TEST_LATITUDE,$TEST_LONGITUDE?q=$TEST_LATITUDE,$TEST_LONGITUDE($encodedPlaceName)"
        assertEquals(expected, result)
        assertFalse("URI must not contain unencoded spaces", result.contains(" "))
    }
}
