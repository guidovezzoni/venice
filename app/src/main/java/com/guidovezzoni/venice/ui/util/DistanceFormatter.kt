package com.guidovezzoni.venice.ui.util

import android.content.res.Resources
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import com.guidovezzoni.venice.R
import java.util.Locale

private const val METRES_PER_KILOMETRE = 1000
private const val METRES_PER_MILE = 1609.344
private val IMPERIAL_UNIT_COUNTRY_CODES = setOf("US", "GB", "LR", "MM")

fun isImperialLocale(locale: Locale): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val measurementSystem = LocaleData.getMeasurementSystem(ULocale.forLocale(locale))
        if (measurementSystem == LocaleData.MeasurementSystem.US) {
            return true
        }
    }
    return locale.country in IMPERIAL_UNIT_COUNTRY_CODES
}

fun formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String =
    if (isImperialLocale(locale)) {
        resources.getString(
            R.string.trip_detail_leg_distance_miles,
            distanceMetres / METRES_PER_MILE.toFloat(),
        )
    } else if (distanceMetres >= METRES_PER_KILOMETRE) {
        resources.getString(
            R.string.trip_detail_leg_distance_kilometres,
            distanceMetres / METRES_PER_KILOMETRE.toFloat(),
        )
    } else {
        resources.getString(R.string.trip_detail_leg_distance_metres, distanceMetres)
    }
