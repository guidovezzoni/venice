package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private const val EMPTY_STATE_PADDING_DP = 16

@Composable
fun TripEmptyState(
    modifier: Modifier = Modifier,
    onCreateTripClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(EMPTY_STATE_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.trip_list_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = onCreateTripClick,
            modifier = Modifier.padding(top = EMPTY_STATE_PADDING_DP.dp),
        ) {
            Text(stringResource(R.string.trip_list_empty_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripEmptyState() {
    HeadingToVeniceTheme {
        TripEmptyState()
    }
}
