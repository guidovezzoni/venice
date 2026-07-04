package com.guidovezzoni.venice.ui.util

import android.content.res.Resources
import com.guidovezzoni.venice.R

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60

fun formatDuration(durationSeconds: Int, resources: Resources): String {
    val minutes = durationSeconds / SECONDS_PER_MINUTE
    return if (minutes >= MINUTES_PER_HOUR) {
        val hours = minutes / MINUTES_PER_HOUR
        val remainingMinutes = minutes % MINUTES_PER_HOUR
        resources.getString(R.string.trip_detail_leg_duration_hours_minutes, hours, remainingMinutes)
    } else {
        resources.getString(R.string.trip_detail_leg_duration_minutes, minutes)
    }
}
