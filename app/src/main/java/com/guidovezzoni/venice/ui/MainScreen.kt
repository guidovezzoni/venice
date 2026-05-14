package com.guidovezzoni.venice.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.effect.TripListUiEffect
import com.guidovezzoni.venice.ui.intent.TripListUiIntent
import com.guidovezzoni.venice.ui.screens.tripdetail.TripDetailScreen
import com.guidovezzoni.venice.ui.screens.triplist.TripListScreen
import com.guidovezzoni.venice.ui.viewmodel.TripDetailViewModel
import com.guidovezzoni.venice.ui.viewmodel.TripListViewModel

private const val ROUTE_TRIP_LIST = "tripList"
private const val ROUTE_TRIP_DETAIL = "tripDetail/{tripId}"
private const val ARG_TRIP_ID = "tripId"

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_TRIP_LIST) {
        composable(ROUTE_TRIP_LIST) {
            val viewModel: TripListViewModel =
                hiltViewModel(checkNotNull<ViewModelStoreOwner>(LocalViewModelStoreOwner.current) {
                    "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
                }, null)
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val errorMessage = stringResource(R.string.create_trip_error_message)

            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is TripListUiEffect.NavigateToTripDetail ->
                            navController.navigate("tripDetail/${effect.tripId}")
                        is TripListUiEffect.ShowError ->
                            snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }

            TripListScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onTripClicked = { tripId -> viewModel.onIntent(TripListUiIntent.OnTripClicked(tripId)) },
                onCreateTripClicked = { viewModel.onIntent(TripListUiIntent.OnCreateTripClicked) },
                onNameChange = { viewModel.onIntent(TripListUiIntent.OnTripNameChanged(it)) },
                onConfirmCreateTrip = { viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip) },
                onDismissCreateDialog = { viewModel.onIntent(TripListUiIntent.OnDismissCreateDialog) },
            )
        }

        composable(ROUTE_TRIP_DETAIL) {
            val viewModel: TripDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val errorMessage = stringResource(R.string.trip_detail_starting_point_error)

            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is TripDetailUiEffect.ShowError ->
                            snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }

            TripDetailScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
