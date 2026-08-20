package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 8.dp

@Composable
fun TripProgressSummary(
    departedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount > 0) departedCount.toFloat() / totalCount.toFloat() else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
    ) {
        Text(
            text = stringResource(R.string.trip_detail_progress_summary, departedCount, totalCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VERTICAL_PADDING),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripProgressSummaryZero() {
    HeadingToVeniceTheme {
        TripProgressSummary(departedCount = 0, totalCount = 4)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripProgressSummaryPartial() {
    HeadingToVeniceTheme {
        TripProgressSummary(departedCount = 3, totalCount = 6)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripProgressSummaryComplete() {
    HeadingToVeniceTheme {
        TripProgressSummary(departedCount = 6, totalCount = 6)
    }
}
