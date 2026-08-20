package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private const val MAX_TRIP_NAME_LENGTH = 100
private val PREVIEW_PADDING = 16.dp
private val SPINNER_SIZE = 20.dp
private val SPINNER_SPACING = 8.dp

@Composable
fun CreateTripDialog(
    tripName: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onNameChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.create_trip_dialog_title)) },
        text = {
            CreateTripDialogContent(
                tripName = tripName,
                onNameChange = onNameChange,
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
                    onClick = onConfirm,
                    enabled = tripName.isNotBlank() && !isLoading,
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
private fun CreateTripDialogContent(
    tripName: String,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit = {},
) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.create_trip_field_label))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tripName,
            onValueChange = { if (it.length <= MAX_TRIP_NAME_LENGTH) onNameChange(it) },
            placeholder = { Text(stringResource(R.string.create_trip_field_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCreateTripDialogEmpty() {
    HeadingToVeniceTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "New Roadtrip",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            CreateTripDialogContent(tripName = "")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCreateTripDialogFilled() {
    HeadingToVeniceTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "New Roadtrip",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            CreateTripDialogContent(tripName = "Summer Drive")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCreateTripDialogLoading() {
    HeadingToVeniceTheme {
        Column(modifier = Modifier.padding(PREVIEW_PADDING)) {
            Text(
                text = "New Roadtrip",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(PREVIEW_PADDING))
            CreateTripDialogContent(tripName = "Summer Drive")
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
