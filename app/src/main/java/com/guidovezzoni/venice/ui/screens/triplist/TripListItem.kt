package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

@Composable
fun TripListItem(
    tripName: String,
    stopCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(tripName) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.trip_list_stop_count, stopCount, stopCount))
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListItemWithStops() {
    HeadingToVeniceTheme {
        TripListItem(
            tripName = "Summer Drive",
            stopCount = 3,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListItemNoStops() {
    HeadingToVeniceTheme {
        TripListItem(
            tripName = "Coast Trip",
            stopCount = 0,
        )
    }
}
