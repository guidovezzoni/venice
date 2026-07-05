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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private val LEG_SUMMARY_ICON_SIZE = 16.dp
private val LEG_SUMMARY_HORIZONTAL_PADDING = 16.dp
private val LEG_SUMMARY_VERTICAL_PADDING = 8.dp
private val LEG_SUMMARY_ICON_SPACING = 6.dp

@Composable
fun LegSummary(
    modifier: Modifier = Modifier,
    formattedDistance: String,
    formattedDuration: String,
) {
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
            text = "$formattedDistance · $formattedDuration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLegSummaryShortDistance() {
    HeadingToVeniceTheme {
        LegSummary(
            formattedDistance = "750 m",
            formattedDuration = "9 min",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLegSummaryLongDistance() {
    HeadingToVeniceTheme {
        LegSummary(
            formattedDistance = "12.5 km",
            formattedDuration = "2h 30min",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLegSummaryImperialLocale() {
    HeadingToVeniceTheme {
        LegSummary(
            formattedDistance = "7.8 mi",
            formattedDuration = "2h 30min",
        )
    }
}
