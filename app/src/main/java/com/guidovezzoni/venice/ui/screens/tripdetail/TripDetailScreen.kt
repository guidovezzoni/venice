package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
                StartingPointSection(
                    startingPoint = uiState.startingPoint,
                    onSetStartingPointClicked = {
                        onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
                    },
                )
            }
        }
    }

    if (uiState.isSetStartingPointDialogVisible) {
        SetStartingPointDialog(
            onConfirm = { placeName, latitude, longitude ->
                onIntent(TripDetailUiIntent.OnStartingPointConfirmed(placeName, latitude, longitude))
            },
            onDismiss = { onIntent(TripDetailUiIntent.OnDismissStartingPointDialog) },
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
