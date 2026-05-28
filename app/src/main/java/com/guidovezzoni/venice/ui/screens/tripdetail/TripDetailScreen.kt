package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val CONTENT_SPACING = 16.dp
private val BUTTON_PADDING = 16.dp
private val ICON_SIZE = 18.dp
private val ICON_SPACING = 8.dp
private const val MIN_REORDERABLE_STOPS = 2

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
            items(uiState.intermediateStops.size) { index ->
                val stop = uiState.intermediateStops[index]
                val showReorderButtons = uiState.intermediateStops.size >= MIN_REORDERABLE_STOPS
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    stop = stop,
                    onSetStopClicked = {
                        onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
                    },
                    icon = Icons.Filled.LocationOn,
                    titleRes = R.string.trip_detail_intermediate_stop_label,
                    setButtonTextRes = R.string.trip_detail_add_stop,
                    changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
                    filledLabelRes = R.string.trip_detail_intermediate_stop_label,
                    onMoveUp = if (showReorderButtons && index > 0) {
                        { onIntent(TripDetailUiIntent.OnMoveStopUp(stop.id, stop.order)) }
                    } else {
                        null
                    },
                    onMoveDown = if (showReorderButtons && index < uiState.intermediateStops.size - 1) {
                        { onIntent(TripDetailUiIntent.OnMoveStopDown(stop.id, stop.order)) }
                    } else {
                        null
                    },
                )
            }
            if (uiState.canAddMoreStops) {
                item {
                    val addStopDescription = stringResource(R.string.trip_detail_add_stop)
                    Spacer(modifier = Modifier.height(CONTENT_SPACING))
                    OutlinedButton(
                        onClick = { onIntent(TripDetailUiIntent.OnAddStopClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BUTTON_PADDING)
                            .semantics { contentDescription = addStopDescription },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddLocation,
                            contentDescription = null,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                        Spacer(modifier = Modifier.width(ICON_SPACING))
                        Text(text = stringResource(R.string.trip_detail_add_stop))
                    }
                }
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
            initialPlaceName = uiState.startingPoint?.placeName ?: "",
            initialLatitude = uiState.startingPoint?.latitude?.toString() ?: "",
            initialLongitude = uiState.startingPoint?.longitude?.toString() ?: "",
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnStartingPointConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissStartingPointDialog) },
        )
    }

    if (uiState.isAddStopDialogVisible) {
        SetStopDialog(
            dialogTitleRes = R.string.trip_detail_add_stop_dialog_title,
            placeNameHintRes = R.string.trip_detail_add_stop_place_name_hint,
            placeNameErrorRes = R.string.trip_detail_add_stop_place_name_error,
            latitudeHintRes = R.string.trip_detail_add_stop_latitude_hint,
            latitudeErrorRes = R.string.trip_detail_add_stop_latitude_error,
            longitudeHintRes = R.string.trip_detail_add_stop_longitude_hint,
            longitudeErrorRes = R.string.trip_detail_add_stop_longitude_error,
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnAddStopConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissAddStopDialog) },
        )
    }

    if (uiState.isEditStopDialogVisible) {
        val editingStop = uiState.editingStop
        SetStopDialog(
            dialogTitleRes = R.string.trip_detail_edit_stop_dialog_title,
            placeNameHintRes = R.string.trip_detail_add_stop_place_name_hint,
            placeNameErrorRes = R.string.trip_detail_add_stop_place_name_error,
            latitudeHintRes = R.string.trip_detail_add_stop_latitude_hint,
            latitudeErrorRes = R.string.trip_detail_add_stop_latitude_error,
            longitudeHintRes = R.string.trip_detail_add_stop_longitude_hint,
            longitudeErrorRes = R.string.trip_detail_add_stop_longitude_error,
            initialPlaceName = editingStop?.placeName ?: "",
            initialLatitude = editingStop?.latitude?.toString() ?: "",
            initialLongitude = editingStop?.longitude?.toString() ?: "",
            onConfirm = { placeName, latitude, longitude ->
                editingStop?.let {
                    onIntent(TripDetailUiIntent.OnEditStopConfirmed(it.id, placeName, latitude, longitude))
                }
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissEditStopDialog) },
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
            initialPlaceName = uiState.destination?.placeName ?: "",
            initialLatitude = uiState.destination?.latitude?.toString() ?: "",
            initialLongitude = uiState.destination?.longitude?.toString() ?: "",
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
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithIntermediateStops() {
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
                intermediateStops = listOf(
                    Stop(
                        id = "2",
                        tripId = "trip-1",
                        placeName = "Florence, Italy",
                        latitude = 43.7696,
                        longitude = 11.2558,
                        order = 1,
                        status = StopStatus.PENDING,
                    ),
                    Stop(
                        id = "3",
                        tripId = "trip-1",
                        placeName = "Nice, France",
                        latitude = 43.7102,
                        longitude = 7.2620,
                        order = 2,
                        status = StopStatus.PENDING,
                    ),
                ),
                destination = Stop(
                    id = "4",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 3,
                    status = StopStatus.PENDING,
                ),
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenAtStopLimit() {
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
                intermediateStops = listOf(
                    Stop(
                        id = "2",
                        tripId = "trip-1",
                        placeName = "Florence, Italy",
                        latitude = 43.7696,
                        longitude = 11.2558,
                        order = 1,
                        status = StopStatus.PENDING,
                    ),
                ),
                destination = Stop(
                    id = "3",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 2,
                    status = StopStatus.PENDING,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenLoading() {
    HeadingToTheAlpsTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                isLoading = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenStartingPointDialog() {
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
                isSetStartingPointDialogVisible = true,
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenDestinationDialog() {
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
                isSetDestinationDialogVisible = true,
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenAddStopDialog() {
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
                isAddStopDialogVisible = true,
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenEditStopDialog() {
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
                intermediateStops = listOf(
                    Stop(
                        id = "2",
                        tripId = "trip-1",
                        placeName = "Florence, Italy",
                        latitude = 43.7696,
                        longitude = 11.2558,
                        order = 1,
                        status = StopStatus.PENDING,
                    ),
                ),
                destination = Stop(
                    id = "3",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 2,
                    status = StopStatus.PENDING,
                ),
                isEditStopDialogVisible = true,
                editingStop = Stop(
                    id = "2",
                    tripId = "trip-1",
                    placeName = "Florence, Italy",
                    latitude = 43.7696,
                    longitude = 11.2558,
                    order = 1,
                    status = StopStatus.PENDING,
                ),
                canAddMoreStops = true,
            ),
        )
    }
}
