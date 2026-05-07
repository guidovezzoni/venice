package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private const val MAX_TRIP_NAME_LENGTH = 100

@Composable
fun CreateTripDialog(
    modifier: Modifier = Modifier,
    tripName: String,
    onNameChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.create_trip_dialog_title)) },
        text = {
            Column {
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
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tripName.isNotBlank(),
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

@Preview(showBackground = true)
@Composable
private fun PreviewCreateTripDialogEmpty() {
    HeadingToTheAlpsTheme {
        CreateTripDialog(tripName = "")
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCreateTripDialogFilled() {
    HeadingToTheAlpsTheme {
        CreateTripDialog(tripName = "Summer Drive")
    }
}
