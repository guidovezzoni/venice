package com.guidovezzoni.venice.ui.util

internal fun parseCoordinate(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()
