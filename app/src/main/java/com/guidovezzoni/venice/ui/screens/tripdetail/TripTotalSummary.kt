package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private val TRIP_TOTAL_SUMMARY_HORIZONTAL_PADDING = 16.dp
private val TRIP_TOTAL_SUMMARY_VERTICAL_PADDING = 8.dp

@Composable
fun TripTotalSummary(
    formattedTotalDistance: String?,
    formattedTotalDuration: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = TRIP_TOTAL_SUMMARY_HORIZONTAL_PADDING,
                vertical = TRIP_TOTAL_SUMMARY_VERTICAL_PADDING,
            ),
    ) {
        if (formattedTotalDistance == null && formattedTotalDuration == null) {
            Text(
                text = stringResource(R.string.trip_detail_totals_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    if (formattedTotalDistance != null) {
                        Text(
                            text = stringResource(R.string.trip_detail_total_distance_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formattedTotalDistance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.trip_detail_total_distance_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (formattedTotalDuration != null) {
                        Text(
                            text = stringResource(R.string.trip_detail_total_duration_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formattedTotalDuration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.trip_detail_total_duration_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripTotalSummaryBothAvailable() {
    HeadingToVeniceTheme {
        TripTotalSummary(
            formattedTotalDistance = "12.5 km",
            formattedTotalDuration = "25 min",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripTotalSummaryBothUnavailable() {
    HeadingToVeniceTheme {
        TripTotalSummary(
            formattedTotalDistance = null,
            formattedTotalDuration = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripTotalSummaryDistanceOnlyAvailable() {
    HeadingToVeniceTheme {
        TripTotalSummary(
            formattedTotalDistance = "12.5 km",
            formattedTotalDuration = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripTotalSummaryDurationOnlyAvailable() {
    HeadingToVeniceTheme {
        TripTotalSummary(
            formattedTotalDistance = null,
            formattedTotalDuration = "25 min",
        )
    }
}
