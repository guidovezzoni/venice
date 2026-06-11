package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme

private val SECTION_PADDING = 16.dp
private val ICON_SIZE = 24.dp
private val ICON_SPACING = 12.dp
private val VERTICAL_SPACING = 4.dp
private val CURRENT_STOP_BORDER_WIDTH = 2.dp
private const val DEPARTED_ALPHA = 0.6f
private const val COORDINATE_DECIMAL_PLACES = 4
private const val COORDINATE_FORMAT = "%.${COORDINATE_DECIMAL_PLACES}f"

@Composable
fun StopSection(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    stop: Stop? = null,
    onSetStopClicked: () -> Unit = {},
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes setButtonTextRes: Int,
    @StringRes changeDescriptionRes: Int,
    @StringRes filledLabelRes: Int,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    stopDisplayState: StopDisplayState = StopDisplayState.UPCOMING,
    onMarkDeparted: (() -> Unit)? = null,
    onUndoDeparted: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = SECTION_PADDING)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = VERTICAL_SPACING),
        )

        if (stop != null) {
            FilledStop(
                stop = stop,
                onClick = onSetStopClicked,
                icon = icon,
                changeDescriptionRes = changeDescriptionRes,
                filledLabelRes = filledLabelRes,
                isLoading = isLoading,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
                stopDisplayState = stopDisplayState,
                onMarkDeparted = onMarkDeparted,
                onUndoDeparted = onUndoDeparted,
            )
        } else {
            EmptyStop(
                onClick = onSetStopClicked,
                setButtonTextRes = setButtonTextRes,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
private fun FilledStop(
    stop: Stop,
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes changeDescriptionRes: Int,
    @StringRes filledLabelRes: Int,
    isLoading: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    stopDisplayState: StopDisplayState = StopDisplayState.UPCOMING,
    onMarkDeparted: (() -> Unit)? = null,
    onUndoDeparted: (() -> Unit)? = null,
) {
    val changeDescription = stringResource(changeDescriptionRes)
    val departedDescription = stringResource(R.string.trip_detail_stop_departed_icon_description)
    val isDeparted = stopDisplayState == StopDisplayState.DEPARTED
    val contentAlpha = if (isDeparted) DEPARTED_ALPHA else 1f
    val iconTint = if (stopDisplayState == StopDisplayState.CURRENT) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val displayIcon = if (isDeparted) Icons.Filled.CheckCircle else icon
    val iconContentDescription = if (isDeparted) departedDescription else null
    val isCurrent = stopDisplayState == StopDisplayState.CURRENT
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) {
                    Modifier.border(CURRENT_STOP_BORDER_WIDTH, primaryColor, CardDefaults.shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = changeDescription },
    ) {
        Row(
            modifier = Modifier.padding(SECTION_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = iconContentDescription,
                modifier = Modifier
                    .size(ICON_SIZE)
                    .graphicsLayer { alpha = contentAlpha },
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(ICON_SPACING))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(filledLabelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                )
                Text(
                    text = stop.placeName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                )
                Text(
                    text = "${COORDINATE_FORMAT.format(stop.latitude)}, ${COORDINATE_FORMAT.format(stop.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                )
            }
            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.trip_detail_move_stop_up),
                    )
                }
            }
            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.trip_detail_move_stop_down),
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.trip_detail_remove_stop),
                    )
                }
            }
        }
        if (onMarkDeparted != null) {
            OutlinedButton(
                onClick = onMarkDeparted,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SECTION_PADDING)
                    .padding(bottom = VERTICAL_SPACING),
            ) {
                Text(text = stringResource(R.string.trip_detail_mark_departed))
            }
        }
        if (onUndoDeparted != null) {
            OutlinedButton(
                onClick = onUndoDeparted,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SECTION_PADDING)
                    .padding(bottom = VERTICAL_SPACING),
            ) {
                Text(text = stringResource(R.string.trip_detail_undo_departed))
            }
        }
    }
}

@Composable
private fun EmptyStop(
    onClick: () -> Unit,
    @StringRes setButtonTextRes: Int,
    isLoading: Boolean = false,
) {
    val buttonDescription = stringResource(setButtonTextRes)
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = buttonDescription },
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(ICON_SPACING))
        Text(text = stringResource(setButtonTextRes))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionStartingPointEmpty() {
    HeadingToTheAlpsTheme {
        StopSection(
            icon = Icons.Filled.TripOrigin,
            titleRes = R.string.trip_detail_starting_point_label,
            setButtonTextRes = R.string.trip_detail_set_starting_point,
            changeDescriptionRes = R.string.trip_detail_change_starting_point,
            filledLabelRes = R.string.trip_detail_starting_point_start_label,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionStartingPointFilled() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "1",
                tripId = "trip-1",
                placeName = "Rome, Italy",
                latitude = 41.9028,
                longitude = 12.4964,
                order = 0,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.TripOrigin,
            titleRes = R.string.trip_detail_starting_point_label,
            setButtonTextRes = R.string.trip_detail_set_starting_point,
            changeDescriptionRes = R.string.trip_detail_change_starting_point,
            filledLabelRes = R.string.trip_detail_starting_point_start_label,
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionIntermediateWithBothButtons() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "3",
                tripId = "trip-1",
                placeName = "Florence, Italy",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 2,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_intermediate_stop_label,
            setButtonTextRes = R.string.trip_detail_add_stop,
            changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
            filledLabelRes = R.string.trip_detail_intermediate_stop_label,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionIntermediateWithMoveDownOnly() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "3",
                tripId = "trip-1",
                placeName = "Florence, Italy",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 1,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_intermediate_stop_label,
            setButtonTextRes = R.string.trip_detail_add_stop,
            changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
            filledLabelRes = R.string.trip_detail_intermediate_stop_label,
            onMoveDown = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionIntermediateWithMoveUpOnly() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "3",
                tripId = "trip-1",
                placeName = "Nice, France",
                latitude = 43.7102,
                longitude = 7.2620,
                order = 2,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_intermediate_stop_label,
            setButtonTextRes = R.string.trip_detail_add_stop,
            changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
            filledLabelRes = R.string.trip_detail_intermediate_stop_label,
            onMoveUp = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionDestinationEmpty() {
    HeadingToTheAlpsTheme {
        StopSection(
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_destination_label,
            setButtonTextRes = R.string.trip_detail_set_destination,
            changeDescriptionRes = R.string.trip_detail_change_destination,
            filledLabelRes = R.string.trip_detail_destination_label,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionDestinationFilled() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "2",
                tripId = "trip-1",
                placeName = "Barcelona, Spain",
                latitude = 41.3851,
                longitude = 2.1734,
                order = 1,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_destination_label,
            setButtonTextRes = R.string.trip_detail_set_destination,
            changeDescriptionRes = R.string.trip_detail_change_destination,
            filledLabelRes = R.string.trip_detail_destination_label,
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionDeparted() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "1",
                tripId = "trip-1",
                placeName = "Rome, Italy",
                latitude = 41.9028,
                longitude = 12.4964,
                order = 0,
                status = StopStatus.VISITED,
            ),
            icon = Icons.Filled.TripOrigin,
            titleRes = R.string.trip_detail_starting_point_label,
            setButtonTextRes = R.string.trip_detail_set_starting_point,
            changeDescriptionRes = R.string.trip_detail_change_starting_point,
            filledLabelRes = R.string.trip_detail_starting_point_start_label,
            stopDisplayState = StopDisplayState.DEPARTED,
            onUndoDeparted = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionCurrent() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "2",
                tripId = "trip-1",
                placeName = "Florence, Italy",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 1,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_intermediate_stop_label,
            setButtonTextRes = R.string.trip_detail_add_stop,
            changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
            filledLabelRes = R.string.trip_detail_intermediate_stop_label,
            stopDisplayState = StopDisplayState.CURRENT,
            onMarkDeparted = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionLoading() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "3",
                tripId = "trip-1",
                placeName = "Florence, Italy",
                latitude = 43.7696,
                longitude = 11.2558,
                order = 1,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_intermediate_stop_label,
            setButtonTextRes = R.string.trip_detail_add_stop,
            changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
            filledLabelRes = R.string.trip_detail_intermediate_stop_label,
            stopDisplayState = StopDisplayState.CURRENT,
            isLoading = true,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            onMarkDeparted = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStopSectionUpcoming() {
    HeadingToTheAlpsTheme {
        StopSection(
            stop = Stop(
                id = "3",
                tripId = "trip-1",
                placeName = "Barcelona, Spain",
                latitude = 41.3851,
                longitude = 2.1734,
                order = 2,
                status = StopStatus.PENDING,
            ),
            icon = Icons.Filled.Place,
            titleRes = R.string.trip_detail_destination_label,
            setButtonTextRes = R.string.trip_detail_set_destination,
            changeDescriptionRes = R.string.trip_detail_change_destination,
            filledLabelRes = R.string.trip_detail_destination_label,
            stopDisplayState = StopDisplayState.UPCOMING,
        )
    }
}
