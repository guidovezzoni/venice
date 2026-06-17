package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val LEG_SUMMARY_ICON_SIZE = 16.dp
private val LEG_SUMMARY_HORIZONTAL_PADDING = 16.dp
private val LEG_SUMMARY_VERTICAL_PADDING = 8.dp
private val LEG_SUMMARY_ICON_SPACING = 6.dp
private const val METRES_PER_KILOMETRE = 1000
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60

@Composable
fun LegSummary(
    modifier: Modifier = Modifier,
    leg: Leg,
) {
    val distanceText = formatDistance(leg.distanceMetres)
    val durationText = formatDuration(leg.durationSeconds)

    Row(
        modifier = modifier
            .padding(horizontal = LEG_SUMMARY_HORIZONTAL_PADDING, vertical = LEG_SUMMARY_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Route,
            contentDescription = null,
            modifier = Modifier.size(LEG_SUMMARY_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(LEG_SUMMARY_ICON_SPACING))
        Text(
            text = "$distanceText · $durationText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatDistance(distanceMetres: Int): String =
    if (distanceMetres >= METRES_PER_KILOMETRE) {
        stringResource(R.string.trip_detail_leg_distance_kilometres, distanceMetres / METRES_PER_KILOMETRE.toFloat())
    } else {
        stringResource(R.string.trip_detail_leg_distance_metres, distanceMetres)
    }

@Composable
private fun formatDuration(durationSeconds: Int): String {
    val minutes = durationSeconds / SECONDS_PER_MINUTE
    return if (minutes >= MINUTES_PER_HOUR) {
        val hours = minutes / MINUTES_PER_HOUR
        val remainingMinutes = minutes % MINUTES_PER_HOUR
        stringResource(R.string.trip_detail_leg_duration_hours_minutes, hours, remainingMinutes)
    } else {
        stringResource(R.string.trip_detail_leg_duration_minutes, minutes)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLegSummaryShortDistance() {
    HeadingToTheAlpsTheme {
        LegSummary(
            leg = Leg(
                id = "leg-1",
                tripId = "trip-1",
                fromStopId = "stop-1",
                toStopId = "stop-2",
                distanceMetres = 750,
                durationSeconds = 540,
                encodedPolyline = "",
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLegSummaryLongDistance() {
    HeadingToTheAlpsTheme {
        LegSummary(
            leg = Leg(
                id = "leg-2",
                tripId = "trip-1",
                fromStopId = "stop-2",
                toStopId = "stop-3",
                distanceMetres = 12500,
                durationSeconds = 9000,
                encodedPolyline = "",
            ),
        )
    }
}
