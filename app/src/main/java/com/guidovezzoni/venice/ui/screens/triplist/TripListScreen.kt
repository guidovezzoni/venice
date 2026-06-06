package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.ui.state.TripListUiState
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    modifier: Modifier = Modifier,
    uiState: TripListUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTripClicked: (tripId: String) -> Unit = {},
    onCreateTripClicked: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onConfirmCreateTrip: () -> Unit = {},
    onDismissCreateDialog: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTripClicked,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.trip_list_fab_label)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.trips.isEmpty()) {
                TripEmptyState(
                    onCreateTripClicked = onCreateTripClicked,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.trips, key = { it.id }) { trip ->
                        TripListItem(
                            modifier = Modifier.fillMaxWidth(),
                            tripName = trip.name,
                            stopCount = trip.stopCount,
                            onClick = { onTripClicked(trip.id) },
                        )
                    }
                }
            }
        }

        if (uiState.isCreateDialogVisible) {
            CreateTripDialog(
                isLoading = uiState.isLoading,
                tripName = uiState.tripNameInput,
                onNameChange = onNameChange,
                onConfirm = onConfirmCreateTrip,
                onDismiss = onDismissCreateDialog,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListScreenEmpty() {
    HeadingToTheAlpsTheme {
        TripListScreen(uiState = TripListUiState())
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListScreenWithTrips() {
    HeadingToTheAlpsTheme {
        TripListScreen(
            uiState = TripListUiState(
                trips = listOf(
                    Trip(id = "1", name = "Summer Drive", createdAt = 0L, updatedAt = 0L),
                    Trip(id = "2", name = "Coast Trip", createdAt = 0L, updatedAt = 0L),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListScreenLoading() {
    HeadingToTheAlpsTheme {
        TripListScreen(uiState = TripListUiState(isLoading = true))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripListScreenWithDialog() {
    HeadingToTheAlpsTheme {
        TripListScreen(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "Alps 2026",
            ),
        )
    }
}
