package com.guidovezzoni.venice.ui.util

import java.util.Locale

private const val COORDINATE_DECIMAL_PLACES = 4
private val COORDINATE_FORMAT = "%.${COORDINATE_DECIMAL_PLACES}f"

internal fun formatCoordinate(value: Double): String =
    String.format(Locale.US, COORDINATE_FORMAT, value)

internal fun formatCoordinates(latitude: Double, longitude: Double): String =
    "${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}"
