package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.SetStopUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ARG_TRIP_ID = "tripId"
private const val STARTING_POINT_ORDER = 0
private const val MAX_STOP_COUNT = 25
private const val UNKNOWN_ERROR = "Unknown error"

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val setStopUseCase: SetStopUseCase,
    observeStopsUseCase: ObserveStopsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle[ARG_TRIP_ID])

    private val _uiState = MutableStateFlow(TripDetailUiState(tripId = tripId))
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TripDetailUiEffect>()
    val uiEffect: SharedFlow<TripDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        observeStopsUseCase(tripId)
            .onEach { stops ->
                val startingPoint = stops.firstOrNull { it.order == STARTING_POINT_ORDER }
                val destination = stops.filter { it.order > STARTING_POINT_ORDER }
                    .maxByOrNull { it.order }
                val intermediateStops = if (destination != null) {
                    stops.filter { it.order > STARTING_POINT_ORDER && it.order < destination.order }
                        .sortedBy { it.order }
                } else {
                    emptyList()
                }
                val canAddMoreStops = stops.size < MAX_STOP_COUNT
                _uiState.update {
                    it.copy(
                        startingPoint = startingPoint,
                        destination = destination,
                        intermediateStops = intermediateStops,
                        canAddMoreStops = canAddMoreStops,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TripDetailUiIntent) {
        when (intent) {
            TripDetailUiIntent.OnSetStartingPointClicked ->
                _uiState.update { it.copy(isSetStartingPointDialogVisible = true) }

            TripDetailUiIntent.OnDismissStartingPointDialog ->
                _uiState.update { it.copy(isSetStartingPointDialogVisible = false) }

            is TripDetailUiIntent.OnStartingPointConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.STARTING_POINT) {
                    it.copy(isSetStartingPointDialogVisible = false)
                }

            TripDetailUiIntent.OnSetDestinationClicked ->
                _uiState.update { it.copy(isSetDestinationDialogVisible = true) }

            TripDetailUiIntent.OnDismissDestinationDialog ->
                _uiState.update { it.copy(isSetDestinationDialogVisible = false) }

            is TripDetailUiIntent.OnDestinationConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.DESTINATION) {
                    it.copy(isSetDestinationDialogVisible = false)
                }

            TripDetailUiIntent.OnAddStopClicked ->
                _uiState.update { it.copy(isAddStopDialogVisible = true) }

            TripDetailUiIntent.OnDismissAddStopDialog ->
                _uiState.update { it.copy(isAddStopDialogVisible = false) }

            is TripDetailUiIntent.OnAddStopConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.INTERMEDIATE) {
                    it.copy(isAddStopDialogVisible = false)
                }
        }
    }

    private fun setStop(
        placeName: String,
        latitude: Double,
        longitude: Double,
        stopType: StopType,
        dismissDialog: (TripDetailUiState) -> TripDetailUiState,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            setStopUseCase(tripId, placeName, latitude, longitude, stopType)
                .onSuccess {
                    _uiState.update { dismissDialog(it).copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }
}
