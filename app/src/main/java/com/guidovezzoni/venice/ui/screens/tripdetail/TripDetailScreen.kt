package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val CONTENT_SPACING = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    modifier: Modifier = Modifier,
    uiState: TripDetailUiState = TripDetailUiState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onIntent: (TripDetailUiIntent) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    stop = uiState.startingPoint,
                    onSetStopClicked = {
                        onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
                    },
                    icon = Icons.Filled.TripOrigin,
                    titleRes = R.string.trip_detail_starting_point_label,
                    setButtonTextRes = R.string.trip_detail_set_starting_point,
                    changeDescriptionRes = R.string.trip_detail_change_starting_point,
                    filledLabelRes = R.string.trip_detail_starting_point_start_label,
                )
            }
            item {
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    stop = uiState.destination,
                    onSetStopClicked = {
                        onIntent(TripDetailUiIntent.OnSetDestinationClicked)
                    },
                    icon = Icons.Filled.Place,
                    titleRes = R.string.trip_detail_destination_label,
                    setButtonTextRes = R.string.trip_detail_set_destination,
                    changeDescriptionRes = R.string.trip_detail_change_destination,
                    filledLabelRes = R.string.trip_detail_destination_label,
                )
            }
        }
    }

    if (uiState.isSetStartingPointDialogVisible) {
        SetStopDialog(
            dialogTitleRes = R.string.trip_detail_starting_point_dialog_title,
            placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
            placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
            latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
            latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
            longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
            longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnStartingPointConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissStartingPointDialog) },
        )
    }

    if (uiState.isSetDestinationDialogVisible) {
        SetStopDialog(
            dialogTitleRes = R.string.trip_detail_destination_dialog_title,
            placeNameHintRes = R.string.trip_detail_destination_place_name_hint,
            placeNameErrorRes = R.string.trip_detail_destination_place_name_error,
            latitudeHintRes = R.string.trip_detail_destination_latitude_hint,
            latitudeErrorRes = R.string.trip_detail_destination_latitude_error,
            longitudeHintRes = R.string.trip_detail_destination_longitude_hint,
            longitudeErrorRes = R.string.trip_detail_destination_longitude_error,
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnDestinationConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissDestinationDialog) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenEmpty() {
    HeadingToTheAlpsTheme {
        TripDetailScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithStartingPoint() {
    HeadingToTheAlpsTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                startingPoint = Stop(
                    id = "1",
                    tripId = "trip-1",
                    placeName = "Rome, Italy",
                    latitude = 41.9028,
                    longitude = 12.4964,
                    order = 0,
                    status = StopStatus.PENDING,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithStartingPointAndDestination() {
    HeadingToTheAlpsTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                startingPoint = Stop(
                    id = "1",
                    tripId = "trip-1",
                    placeName = "Rome, Italy",
                    latitude = 41.9028,
                    longitude = 12.4964,
                    order = 0,
                    status = StopStatus.PENDING,
                ),
                destination = Stop(
                    id = "2",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 1,
                    status = StopStatus.PENDING,
                ),
            ),
        )
    }
}
