package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val FIELD_SPACING = 8.dp
private val PREVIEW_PADDING = 16.dp
private val SPINNER_SIZE = 20.dp
private val SPINNER_SPACING = 8.dp
private val SEARCH_SPINNER_SIZE = 16.dp
private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0
private const val COORDINATE_DECIMAL_PLACES = 4
private const val COORDINATE_FORMAT = "%.${COORDINATE_DECIMAL_PLACES}f"

@Composable
fun SetStopDialog(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isResolvingPlace: Boolean = false,
    placeDetailError: String? = null,
    @StringRes dialogTitleRes: Int,
    @StringRes placeNameHintRes: Int,
    @StringRes placeNameErrorRes: Int,
    @StringRes latitudeHintRes: Int,
    @StringRes latitudeErrorRes: Int,
    @StringRes longitudeHintRes: Int,
    @StringRes longitudeErrorRes: Int,
    initialPlaceName: String = "",
    initialLatitude: String = "",
    initialLongitude: String = "",
    suggestions: List<PlaceSuggestion> = emptyList(),
    isSearchingPlaces: Boolean = false,
    searchError: String? = null,
    selectedPlaceDetail: PlaceDetail? = null,
    onSearchQueryChanged: (String) -> Unit = {},
    onSuggestionSelected: (PlaceSuggestion) -> Unit = {},
    onConfirm: (placeName: String, latitude: Double, longitude: Double) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit = {},
) {
    var placeName by remember { mutableStateOf(initialPlaceName) }
    var latitudeText by remember { mutableStateOf(initialLatitude) }
    var longitudeText by remember { mutableStateOf(initialLongitude) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var coordinatesFromAutocomplete by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlaceDetail) {
        if (selectedPlaceDetail != null) {
            placeName = selectedPlaceDetail.name
            latitudeText = COORDINATE_FORMAT.format(selectedPlaceDetail.latitude)
            longitudeText = COORDINATE_FORMAT.format(selectedPlaceDetail.longitude)
            coordinatesFromAutocomplete = true
        }
    }

    val placeNameError = hasAttemptedSubmit && placeName.isBlank()
    val latitude = latitudeText.toDoubleOrNull()
    val latitudeError = hasAttemptedSubmit && (latitude == null || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE)
    val longitude = longitudeText.toDoubleOrNull()
    val longitudeError = hasAttemptedSubmit && (longitude == null || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(dialogTitleRes)) },
        text = {
            StopForm(
                placeName = placeName,
                onPlaceNameChange = { newValue ->
                    placeName = newValue
                    onSearchQueryChanged(newValue)
                    if (coordinatesFromAutocomplete && newValue != selectedPlaceDetail?.name) {
                        coordinatesFromAutocomplete = false
                    }
                },
                placeNameError = placeNameError,
                placeNameHintRes = placeNameHintRes,
                placeNameErrorRes = placeNameErrorRes,
                latitudeText = latitudeText,
                onLatitudeChange = { latitudeText = it },
                latitudeError = latitudeError,
                latitudeHintRes = latitudeHintRes,
                latitudeErrorRes = latitudeErrorRes,
                longitudeText = longitudeText,
                onLongitudeChange = { longitudeText = it },
                longitudeError = longitudeError,
                longitudeHintRes = longitudeHintRes,
                longitudeErrorRes = longitudeErrorRes,
                coordinatesReadOnly = coordinatesFromAutocomplete,
                suggestions = suggestions,
                isSearchingPlaces = isSearchingPlaces,
                searchError = searchError,
                isResolvingPlace = isResolvingPlace,
                placeDetailError = placeDetailError,
                onSuggestionSelected = onSuggestionSelected,
            )
        },
        confirmButton = {
            val loadingDescription = stringResource(R.string.global_loading)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(SPINNER_SIZE)
                            .semantics { contentDescription = loadingDescription },
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(SPINNER_SPACING))
                }
                TextButton(
                    onClick = {
                        hasAttemptedSubmit = true
                        val lat = latitudeText.toDoubleOrNull()
                        val lng = longitudeText.toDoubleOrNull()
                        if (placeName.isNotBlank() &&
                            lat != null && lat in MIN_LATITUDE..MAX_LATITUDE &&
                            lng != null && lng in MIN_LONGITUDE..MAX_LONGITUDE
                        ) {
                            onConfirm(placeName, lat, lng)
                        }
                    },
                    enabled = !isLoading && !isResolvingPlace,
                ) {
                    Text(stringResource(R.string.global_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.global_cancel))
            }
        },
    )
}

@Composable
private fun StopForm(
    placeName: String,
    onPlaceNameChange: (String) -> Unit,
    placeNameError: Boolean,
    @StringRes placeNameHintRes: Int,
    @StringRes placeNameErrorRes: Int,
    latitudeText: String,
    onLatitudeChange: (String) -> Unit,
    latitudeError: Boolean,
    @StringRes latitudeHintRes: Int,
    @StringRes latitudeErrorRes: Int,
    longitudeText: String,
    onLongitudeChange: (String) -> Unit,
    longitudeError: Boolean,
    @StringRes longitudeHintRes: Int,
    @StringRes longitudeErrorRes: Int,
    coordinatesReadOnly: Boolean = false,
    suggestions: List<PlaceSuggestion> = emptyList(),
    isSearchingPlaces: Boolean = false,
    searchError: String? = null,
    isResolvingPlace: Boolean = false,
    placeDetailError: String? = null,
    onSuggestionSelected: (PlaceSuggestion) -> Unit = {},
) {
    val searchingPlacesDescription = stringResource(R.string.global_searching_places)
    val resolvingPlaceDescription = stringResource(R.string.global_resolving_place)

    Column {
        OutlinedTextField(
            value = placeName,
            onValueChange = onPlaceNameChange,
            label = { Text(stringResource(placeNameHintRes)) },
            isError = placeNameError,
            supportingText = if (placeNameError) {
                { Text(stringResource(placeNameErrorRes)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isSearchingPlaces) {
            Spacer(modifier = Modifier.height(FIELD_SPACING))
            CircularProgressIndicator(
                modifier = Modifier
                    .size(SEARCH_SPINNER_SIZE)
                    .semantics { contentDescription = searchingPlacesDescription },
                strokeWidth = 2.dp,
            )
        }

        if (searchError != null) {
            Spacer(modifier = Modifier.height(FIELD_SPACING))
            Text(
                text = searchError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FIELD_SPACING))
            LazyColumn {
                items(suggestions) { suggestion ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionSelected(suggestion) }
                            .padding(vertical = FIELD_SPACING),
                    ) {
                        Text(
                            text = suggestion.primaryText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = suggestion.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (isResolvingPlace) {
            Spacer(modifier = Modifier.height(FIELD_SPACING))
            CircularProgressIndicator(
                modifier = Modifier
                    .size(SEARCH_SPINNER_SIZE)
                    .semantics { contentDescription = resolvingPlaceDescription },
                strokeWidth = 2.dp,
            )
        }

        if (placeDetailError != null) {
            Spacer(modifier = Modifier.height(FIELD_SPACING))
            Text(
                text = placeDetailError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(FIELD_SPACING))
        OutlinedTextField(
            value = latitudeText,
            onValueChange = onLatitudeChange,
            label = { Text(stringResource(latitudeHintRes)) },
            isError = latitudeError,
            supportingText = if (latitudeError) {
                { Text(stringResource(latitudeErrorRes)) }
            } else null,
            singleLine = true,
            readOnly = coordinatesReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FIELD_SPACING))
        OutlinedTextField(
            value = longitudeText,
            onValueChange = onLongitudeChange,
            label = { Text(stringResource(longitudeHintRes)) },
            isError = longitudeError,
            supportingText = if (longitudeError) {
                { Text(stringResource(longitudeErrorRes)) }
            } else null,
            singleLine = true,
            readOnly = coordinatesReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogLoading() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Rome, Italy",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "41.9028",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "12.4964",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {}) {
                    Text(stringResource(R.string.global_cancel))
                }
                val loadingDescription = stringResource(R.string.global_loading)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(SPINNER_SIZE)
                        .semantics { contentDescription = loadingDescription },
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(SPINNER_SPACING))
                TextButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.global_confirm))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogStartingPointEmpty() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogStartingPointWithErrors() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = true,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "999",
                onLatitudeChange = {},
                latitudeError = true,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = true,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogDestinationEmpty() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set destination",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_destination_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_destination_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_destination_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_destination_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_destination_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_destination_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogStartingPointPrePopulated() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point (pre-populated)",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Rome, Italy",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "41.9028",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "12.4964",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogDestinationPrePopulated() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set destination (pre-populated)",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Barcelona, Spain",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_destination_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_destination_place_name_error,
                latitudeText = "41.3851",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_destination_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_destination_latitude_error,
                longitudeText = "2.1734",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_destination_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_destination_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogDestinationWithErrors() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set destination",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = true,
                placeNameHintRes = R.string.trip_detail_destination_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_destination_place_name_error,
                latitudeText = "999",
                onLatitudeChange = {},
                latitudeError = true,
                latitudeHintRes = R.string.trip_detail_destination_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_destination_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = true,
                longitudeHintRes = R.string.trip_detail_destination_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_destination_longitude_error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogWithSuggestions() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Search with suggestions",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Colos",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                suggestions = listOf(
                    PlaceSuggestion(
                        placeId = "place-1",
                        primaryText = "Colosseum",
                        secondaryText = "Rome, Metropolitan City of Rome Capital, Italy",
                    ),
                    PlaceSuggestion(
                        placeId = "place-2",
                        primaryText = "Colosseo",
                        secondaryText = "Via Sacra, Rome, Italy",
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogSearchLoading() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Search loading",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Colos",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                isSearchingPlaces = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogReadOnlyCoordinates() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Read-only coordinates after selection",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Colosseum",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "41.8902",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "12.4922",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                coordinatesReadOnly = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogResolvingPlace() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Resolving place coordinates",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Colosseum",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                isResolvingPlace = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStopDialogPlaceDetailError() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Place detail error",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StopForm(
                placeName = "Invalid Place",
                onPlaceNameChange = {},
                placeNameError = false,
                placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
                longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                placeDetailError = "Could not resolve place. Tap a suggestion to try again.",
            )
        }
    }
}
