package com.guidovezzoni.venice.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
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
import com.guidovezzoni.venice.ui.state.DialogState
import com.guidovezzoni.venice.ui.screens.triplist.TripListScreen
import com.guidovezzoni.venice.ui.util.buildGeoUri
import com.guidovezzoni.venice.ui.viewmodel.TripDetailViewModel
import com.guidovezzoni.venice.ui.viewmodel.TripListViewModel
import kotlinx.coroutines.flow.Flow

private const val ROUTE_TRIP_LIST = "tripList"
private const val ROUTE_TRIP_DETAIL = "tripDetail/{tripId}"

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
                onTripClick = { tripId -> viewModel.onIntent(TripListUiIntent.OnTripClicked(tripId)) },
                onCreateTripClick = { viewModel.onIntent(TripListUiIntent.OnCreateTripClicked) },
                onNameChange = { name -> viewModel.onIntent(TripListUiIntent.OnTripNameChanged(name)) },
                onConfirmCreateTrip = { viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip) },
                onDismissCreateDialog = { viewModel.onIntent(TripListUiIntent.OnDismissCreateDialog) },
            )
        }

        composable(ROUTE_TRIP_DETAIL) {
            val viewModel: TripDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val startingPointErrorMessage = stringResource(R.string.trip_detail_starting_point_error)
            val destinationErrorMessage = stringResource(R.string.trip_detail_destination_error)
            val noAppAvailableError = stringResource(R.string.nav_action_no_app_available_error)

            TripDetailEffectHandler(
                effects = viewModel.uiEffect,
                snackbarHostState = snackbarHostState,
                startingPointErrorMessage = startingPointErrorMessage,
                destinationErrorMessage = destinationErrorMessage,
                noAppAvailableError = noAppAvailableError,
                getDialogState = { uiState.dialogState },
            )

            TripDetailScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
internal fun TripDetailEffectHandler(
    effects: Flow<TripDetailUiEffect>,
    snackbarHostState: SnackbarHostState,
    startingPointErrorMessage: String,
    destinationErrorMessage: String,
    noAppAvailableError: String,
    getDialogState: () -> DialogState,
    resolveActivity: ((Intent) -> Boolean)? = null,
    startActivity: ((Intent) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentGetDialogState by rememberUpdatedState(getDialogState)
    val currentResolveActivity by rememberUpdatedState(resolveActivity)
    val currentStartActivity by rememberUpdatedState(startActivity)
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is TripDetailUiEffect.ShowError -> {
                    val message = if (currentGetDialogState() is DialogState.SetDestination) {
                        destinationErrorMessage
                    } else {
                        startingPointErrorMessage
                    }
                    snackbarHostState.showSnackbar(message)
                }
                is TripDetailUiEffect.LaunchNavigation -> {
                    val uri = buildGeoUri(effect.latitude, effect.longitude, effect.placeName)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    val canResolve = currentResolveActivity?.invoke(intent)
                        ?: (context.packageManager.resolveActivity(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY,
                        ) != null)
                    if (canResolve) {
                        val activityStarter = currentStartActivity
                        if (activityStarter != null) {
                            activityStarter(intent)
                        } else {
                            context.startActivity(intent)
                        }
                    } else {
                        snackbarHostState.showSnackbar(noAppAvailableError)
                    }
                }
                is TripDetailUiEffect.ShowNavigationError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
}
