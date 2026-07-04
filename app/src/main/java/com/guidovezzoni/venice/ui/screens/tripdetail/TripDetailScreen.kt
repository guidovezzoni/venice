package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

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
        val allStops = buildList {
            uiState.startingPoint?.let { add(it) }
            addAll(uiState.intermediateStops)
            uiState.destination?.let { add(it) }
        }
        val currentStopId = allStops.sortedBy { it.order }
            .firstOrNull { it.status == StopStatus.PENDING }?.id
        val lastDepartedStopId = allStops
            .filter { it.status == StopStatus.VISITED }
            .maxByOrNull { it.order }?.id
        val totalCount = allStops.size
        val departedCount = allStops.count { it.status == StopStatus.VISITED }

        val legByFromStopId = uiState.legs.associateBy { it.fromStopId }
        val loadingDescription = stringResource(R.string.global_loading)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (totalCount > 0) {
                item {
                    TripProgressSummary(
                        departedCount = departedCount,
                        totalCount = totalCount,
                    )
                }
            }
            item {
                val startingPoint = uiState.startingPoint
                val startingPointDisplayState = startingPoint?.let { deriveDisplayState(it, currentStopId) }
                    ?: StopDisplayState.UPCOMING
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    isLoading = uiState.isLoading,
                    stop = startingPoint,
                    coordinatesText = startingPoint?.let { uiState.formattedStopCoordinates[it.id] },
                    onSetStopClicked = {
                        onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
                    },
                    icon = Icons.Filled.TripOrigin,
                    titleRes = R.string.trip_detail_starting_point_label,
                    setButtonTextRes = R.string.trip_detail_set_starting_point,
                    changeDescriptionRes = R.string.trip_detail_change_starting_point,
                    filledLabelRes = R.string.trip_detail_starting_point_start_label,
                    onDelete = startingPoint?.let { stop ->
                        { onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop)) }
                    },
                    stopDisplayState = startingPointDisplayState,
                    onMarkDeparted = startingPoint?.takeIf { it.id == currentStopId }?.let {
                        { onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked(it.id)) }
                    },
                    onUndoDeparted = startingPoint?.takeIf { it.id == lastDepartedStopId }?.let {
                        { onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked(it.id)) }
                    },
                )
                startingPoint?.let { stop ->
                    legByFromStopId[stop.id]?.let {
                        LegSummary(
                            formattedDistance = uiState.formattedLegDistances[stop.id] ?: "",
                            formattedDuration = uiState.formattedLegDurations[stop.id] ?: "",
                        )
                    }
                }
            }
            items(uiState.intermediateStops.size) { index ->
                val stop = uiState.intermediateStops[index]
                val showReorderButtons = uiState.intermediateStops.size >= MIN_REORDERABLE_STOPS
                val displayState = deriveDisplayState(stop, currentStopId)
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    isLoading = uiState.isLoading,
                    stop = stop,
                    coordinatesText = uiState.formattedStopCoordinates[stop.id],
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
                    onDelete = { onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop)) },
                    stopDisplayState = displayState,
                    onMarkDeparted = if (stop.id == currentStopId) {
                        { onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked(stop.id)) }
                    } else {
                        null
                    },
                    onUndoDeparted = if (stop.id == lastDepartedStopId) {
                        { onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked(stop.id)) }
                    } else {
                        null
                    },
                )
                legByFromStopId[stop.id]?.let {
                    LegSummary(
                        formattedDistance = uiState.formattedLegDistances[stop.id] ?: "",
                        formattedDuration = uiState.formattedLegDurations[stop.id] ?: "",
                    )
                }
            }
            if (uiState.canAddMoreStops) {
                item {
                    val addStopDescription = stringResource(R.string.trip_detail_add_stop)
                    Spacer(modifier = Modifier.height(CONTENT_SPACING))
                    OutlinedButton(
                        onClick = { onIntent(TripDetailUiIntent.OnAddStopClicked) },
                        enabled = !uiState.isLoading,
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
                val destination = uiState.destination
                val destinationDisplayState = destination?.let { deriveDisplayState(it, currentStopId) }
                    ?: StopDisplayState.UPCOMING
                Spacer(modifier = Modifier.height(CONTENT_SPACING))
                StopSection(
                    isLoading = uiState.isLoading,
                    stop = destination,
                    coordinatesText = destination?.let { uiState.formattedStopCoordinates[it.id] },
                    onSetStopClicked = {
                        onIntent(TripDetailUiIntent.OnSetDestinationClicked)
                    },
                    icon = Icons.Filled.Place,
                    titleRes = R.string.trip_detail_destination_label,
                    setButtonTextRes = R.string.trip_detail_set_destination,
                    changeDescriptionRes = R.string.trip_detail_change_destination,
                    filledLabelRes = R.string.trip_detail_destination_label,
                    onDelete = destination?.let { stop ->
                        { onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop)) }
                    },
                    stopDisplayState = destinationDisplayState,
                    onMarkDeparted = destination?.takeIf { it.id == currentStopId }?.let {
                        { onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked(it.id)) }
                    },
                    onUndoDeparted = destination?.takeIf { it.id == lastDepartedStopId }?.let {
                        { onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked(it.id)) }
                    },
                )
            }
            if (allStops.size >= 2) {
                item {
                    Spacer(modifier = Modifier.height(CONTENT_SPACING))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BUTTON_PADDING),
                    ) {
                        Button(
                            onClick = { onIntent(TripDetailUiIntent.OnCalculateRouteClicked) },
                            enabled = !uiState.isCalculatingRoute && !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.isCalculatingRoute) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ICON_SIZE),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(ICON_SPACING))
                                Text(stringResource(R.string.trip_detail_calculating_route))
                            } else {
                                Text(stringResource(R.string.trip_detail_calculate_route))
                            }
                        }
                        uiState.routeError?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = loadingDescription },
                )
            }
        }
    }

    if (uiState.isSetStartingPointDialogVisible) {
        SetStopDialog(
            isLoading = uiState.isLoading,
            isResolvingPlace = uiState.isResolvingPlace,
            placeDetailError = uiState.placeDetailError,
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
            suggestions = uiState.placeSuggestions,
            isSearchingPlaces = uiState.isSearchingPlaces,
            searchError = uiState.searchError,
            selectedPlaceDetail = uiState.selectedPlaceDetail,
            onSearchQueryChanged = { query -> onIntent(TripDetailUiIntent.OnSearchQueryChanged(query)) },
            onSuggestionSelected = { suggestion -> onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion)) },
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnStartingPointConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissStartingPointDialog) },
        )
    }

    if (uiState.isAddStopDialogVisible) {
        SetStopDialog(
            isLoading = uiState.isLoading,
            isResolvingPlace = uiState.isResolvingPlace,
            placeDetailError = uiState.placeDetailError,
            dialogTitleRes = R.string.trip_detail_add_stop_dialog_title,
            placeNameHintRes = R.string.trip_detail_add_stop_place_name_hint,
            placeNameErrorRes = R.string.trip_detail_add_stop_place_name_error,
            latitudeHintRes = R.string.trip_detail_add_stop_latitude_hint,
            latitudeErrorRes = R.string.trip_detail_add_stop_latitude_error,
            longitudeHintRes = R.string.trip_detail_add_stop_longitude_hint,
            longitudeErrorRes = R.string.trip_detail_add_stop_longitude_error,
            suggestions = uiState.placeSuggestions,
            isSearchingPlaces = uiState.isSearchingPlaces,
            searchError = uiState.searchError,
            selectedPlaceDetail = uiState.selectedPlaceDetail,
            onSearchQueryChanged = { query -> onIntent(TripDetailUiIntent.OnSearchQueryChanged(query)) },
            onSuggestionSelected = { suggestion -> onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion)) },
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnAddStopConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissAddStopDialog) },
        )
    }

    if (uiState.isEditStopDialogVisible) {
        val editingStop = uiState.editingStop
        SetStopDialog(
            isLoading = uiState.isLoading,
            isResolvingPlace = uiState.isResolvingPlace,
            placeDetailError = uiState.placeDetailError,
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
            suggestions = uiState.placeSuggestions,
            isSearchingPlaces = uiState.isSearchingPlaces,
            searchError = uiState.searchError,
            selectedPlaceDetail = uiState.selectedPlaceDetail,
            onSearchQueryChanged = { query -> onIntent(TripDetailUiIntent.OnSearchQueryChanged(query)) },
            onSuggestionSelected = { suggestion -> onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion)) },
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
            isLoading = uiState.isLoading,
            isResolvingPlace = uiState.isResolvingPlace,
            placeDetailError = uiState.placeDetailError,
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
            suggestions = uiState.placeSuggestions,
            isSearchingPlaces = uiState.isSearchingPlaces,
            searchError = uiState.searchError,
            selectedPlaceDetail = uiState.selectedPlaceDetail,
            onSearchQueryChanged = { query -> onIntent(TripDetailUiIntent.OnSearchQueryChanged(query)) },
            onSuggestionSelected = { suggestion -> onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion)) },
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnDestinationConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissDestinationDialog) },
        )
    }

    if (uiState.isRemoveStopDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(TripDetailUiIntent.OnDismissRemoveStopDialog) },
            title = { Text(stringResource(R.string.trip_detail_remove_stop_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.trip_detail_remove_stop_dialog_message,
                        uiState.stopToRemove?.placeName ?: "",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(TripDetailUiIntent.OnRemoveStopConfirmed) },
                    enabled = !uiState.isLoading,
                ) {
                    Text(stringResource(R.string.global_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(TripDetailUiIntent.OnDismissRemoveStopDialog) }) {
                    Text(stringResource(R.string.global_cancel))
                }
            },
        )
    }
}

private fun deriveDisplayState(stop: Stop, currentStopId: String?): StopDisplayState = when {
    stop.status == StopStatus.VISITED -> StopDisplayState.DEPARTED
    stop.id == currentStopId -> StopDisplayState.CURRENT
    else -> StopDisplayState.UPCOMING
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenEmpty() {
    HeadingToVeniceTheme {
        TripDetailScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithStartingPoint() {
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
private fun PreviewTripDetailScreenWithProgress() {
    HeadingToVeniceTheme {
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
                    status = StopStatus.VISITED,
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
private fun PreviewTripDetailScreenLoading() {
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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
    HeadingToVeniceTheme {
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

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenRemoveStopDialog() {
    HeadingToVeniceTheme {
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
                isRemoveStopDialogVisible = true,
                stopToRemove = Stop(
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

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithSuggestions() {
    HeadingToVeniceTheme {
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
                placeSuggestions = listOf(
                    PlaceSuggestion(
                        placeId = "place-1",
                        primaryText = "Colosseum",
                        secondaryText = "Rome, Italy",
                    ),
                    PlaceSuggestion(
                        placeId = "place-2",
                        primaryText = "Vatican City",
                        secondaryText = "Rome, Italy",
                    ),
                    PlaceSuggestion(
                        placeId = "place-3",
                        primaryText = "Roman Forum",
                        secondaryText = "Rome, Italy",
                    ),
                ),
                selectedPlaceDetail = PlaceDetail(
                    name = "Colosseum",
                    latitude = 41.8902,
                    longitude = 12.4924,
                ),
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenSearchLoading() {
    HeadingToVeniceTheme {
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
                isSearchingPlaces = true,
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenSearchError() {
    HeadingToVeniceTheme {
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
                searchError = "Search unavailable",
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenPlaceDetailError() {
    HeadingToVeniceTheme {
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
                placeDetailError = "Unable to resolve location",
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenResolvingPlace() {
    HeadingToVeniceTheme {
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
                isResolvingPlace = true,
                canAddMoreStops = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithLegs() {
    HeadingToVeniceTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                startingPoint = Stop(
                    id = "stop-1",
                    tripId = "trip-1",
                    placeName = "Rome, Italy",
                    latitude = 41.9028,
                    longitude = 12.4964,
                    order = 0,
                    status = StopStatus.PENDING,
                ),
                destination = Stop(
                    id = "stop-2",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 1,
                    status = StopStatus.PENDING,
                ),
                canAddMoreStops = true,
                legs = listOf(
                    Leg(
                        id = "leg-1",
                        tripId = "trip-1",
                        fromStopId = "stop-1",
                        toStopId = "stop-2",
                        distanceMetres = 12500,
                        durationSeconds = 900,
                        encodedPolyline = "",
                    ),
                ),
                formattedLegDistances = mapOf("stop-1" to "12.5 km"),
                formattedLegDurations = mapOf("stop-1" to "15 min"),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenCalculatingRoute() {
    HeadingToVeniceTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                startingPoint = Stop(
                    id = "stop-1",
                    tripId = "trip-1",
                    placeName = "Rome, Italy",
                    latitude = 41.9028,
                    longitude = 12.4964,
                    order = 0,
                    status = StopStatus.PENDING,
                ),
                destination = Stop(
                    id = "stop-2",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 1,
                    status = StopStatus.PENDING,
                ),
                canAddMoreStops = true,
                isCalculatingRoute = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailScreenWithRouteError() {
    HeadingToVeniceTheme {
        TripDetailScreen(
            uiState = TripDetailUiState(
                tripId = "trip-1",
                startingPoint = Stop(
                    id = "stop-1",
                    tripId = "trip-1",
                    placeName = "Rome, Italy",
                    latitude = 41.9028,
                    longitude = 12.4964,
                    order = 0,
                    status = StopStatus.PENDING,
                ),
                destination = Stop(
                    id = "stop-2",
                    tripId = "trip-1",
                    placeName = "Barcelona, Spain",
                    latitude = 41.3851,
                    longitude = 2.1734,
                    order = 1,
                    status = StopStatus.PENDING,
                ),
                canAddMoreStops = true,
                routeError = "Network error",
            ),
        )
    }
}
