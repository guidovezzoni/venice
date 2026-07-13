package com.guidovezzoni.venice.ui.util

import java.net.URLEncoder

private const val URL_ENCODER_CHARSET = "UTF-8"
private const val PLUS_SIGN = "+"
private const val PERCENT_ENCODED_SPACE = "%20"

internal fun buildGeoUri(latitude: Double, longitude: Double, placeName: String): String {
    val encodedPlaceName = URLEncoder.encode(placeName, URL_ENCODER_CHARSET)
        .replace(PLUS_SIGN, PERCENT_ENCODED_SPACE)
    return "geo:$latitude,$longitude?q=$latitude,$longitude($encodedPlaceName)"
}
