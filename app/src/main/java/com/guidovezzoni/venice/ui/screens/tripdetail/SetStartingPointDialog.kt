package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val FIELD_SPACING = 8.dp
private val PREVIEW_PADDING = 16.dp
private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

@Composable
fun SetStartingPointDialog(
    modifier: Modifier = Modifier,
    onConfirm: (placeName: String, latitude: Double, longitude: Double) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit = {},
) {
    var placeName by rememberSaveable { mutableStateOf("") }
    var latitudeText by rememberSaveable { mutableStateOf("") }
    var longitudeText by rememberSaveable { mutableStateOf("") }
    var hasAttemptedSubmit by rememberSaveable { mutableStateOf(false) }

    val placeNameError = hasAttemptedSubmit && placeName.isBlank()
    val latitude = latitudeText.toDoubleOrNull()
    val latitudeError = hasAttemptedSubmit && (latitude == null || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE)
    val longitude = longitudeText.toDoubleOrNull()
    val longitudeError = hasAttemptedSubmit && (longitude == null || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.trip_detail_starting_point_dialog_title)) },
        text = {
            StartingPointForm(
                placeName = placeName,
                onPlaceNameChange = { placeName = it },
                placeNameError = placeNameError,
                latitudeText = latitudeText,
                onLatitudeChange = { latitudeText = it },
                latitudeError = latitudeError,
                longitudeText = longitudeText,
                onLongitudeChange = { longitudeText = it },
                longitudeError = longitudeError,
            )
        },
        confirmButton = {
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
            ) {
                Text(stringResource(R.string.global_confirm))
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
private fun StartingPointForm(
    placeName: String,
    onPlaceNameChange: (String) -> Unit,
    placeNameError: Boolean,
    latitudeText: String,
    onLatitudeChange: (String) -> Unit,
    latitudeError: Boolean,
    longitudeText: String,
    onLongitudeChange: (String) -> Unit,
    longitudeError: Boolean,
) {
    Column {
        OutlinedTextField(
            value = placeName,
            onValueChange = onPlaceNameChange,
            label = { Text(stringResource(R.string.trip_detail_starting_point_place_name_hint)) },
            isError = placeNameError,
            supportingText = if (placeNameError) {
                { Text(stringResource(R.string.trip_detail_starting_point_place_name_error)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FIELD_SPACING))
        OutlinedTextField(
            value = latitudeText,
            onValueChange = onLatitudeChange,
            label = { Text(stringResource(R.string.trip_detail_starting_point_latitude_hint)) },
            isError = latitudeError,
            supportingText = if (latitudeError) {
                { Text(stringResource(R.string.trip_detail_starting_point_latitude_error)) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(FIELD_SPACING))
        OutlinedTextField(
            value = longitudeText,
            onValueChange = onLongitudeChange,
            label = { Text(stringResource(R.string.trip_detail_starting_point_longitude_hint)) },
            isError = longitudeError,
            supportingText = if (longitudeError) {
                { Text(stringResource(R.string.trip_detail_starting_point_longitude_error)) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStartingPointDialogEmpty() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StartingPointForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = false,
                latitudeText = "",
                onLatitudeChange = {},
                latitudeError = false,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetStartingPointDialogWithErrors() {
    HeadingToTheAlpsTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "Set starting point",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            StartingPointForm(
                placeName = "",
                onPlaceNameChange = {},
                placeNameError = true,
                latitudeText = "999",
                onLatitudeChange = {},
                latitudeError = true,
                longitudeText = "",
                onLongitudeChange = {},
                longitudeError = true,
            )
        }
    }
}
