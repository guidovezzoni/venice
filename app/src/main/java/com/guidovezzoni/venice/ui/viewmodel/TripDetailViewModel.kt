package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import com.guidovezzoni.venice.domain.usecase.EditStopUseCase
import com.guidovezzoni.venice.domain.usecase.GetPlaceDetailUseCase
import com.guidovezzoni.venice.domain.usecase.MarkStopDepartedUseCase
import com.guidovezzoni.venice.domain.usecase.MoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.RemoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.SearchPlacesUseCase
import com.guidovezzoni.venice.domain.usecase.SetStopUseCase
import com.guidovezzoni.venice.domain.usecase.UndoMarkStopDepartedUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.util.withMinimumDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val setStopUseCase: SetStopUseCase,
    private val moveStopUseCase: MoveStopUseCase,
    private val editStopUseCase: EditStopUseCase,
    private val removeStopUseCase: RemoveStopUseCase,
    private val markStopDepartedUseCase: MarkStopDepartedUseCase,
    private val undoMarkStopDepartedUseCase: UndoMarkStopDepartedUseCase,
    observeStopsUseCase: ObserveStopsUseCase,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val placeSearchRepository: PlaceSearchRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle[ARG_TRIP_ID])

    private var searchJob: Job? = null

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

            TripDetailUiIntent.OnDismissStartingPointDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(isSetStartingPointDialogVisible = false) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnStartingPointConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.STARTING_POINT) {
                    it.copy(isSetStartingPointDialogVisible = false)
                }

            TripDetailUiIntent.OnSetDestinationClicked ->
                _uiState.update { it.copy(isSetDestinationDialogVisible = true) }

            TripDetailUiIntent.OnDismissDestinationDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(isSetDestinationDialogVisible = false) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnDestinationConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.DESTINATION) {
                    it.copy(isSetDestinationDialogVisible = false)
                }

            TripDetailUiIntent.OnAddStopClicked ->
                _uiState.update { it.copy(isAddStopDialogVisible = true) }

            TripDetailUiIntent.OnDismissAddStopDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(isAddStopDialogVisible = false) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnAddStopConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.INTERMEDIATE) {
                    it.copy(isAddStopDialogVisible = false)
                }

            is TripDetailUiIntent.OnMoveStopUp ->
                moveStop(intent.currentOrder, intent.currentOrder - 1)

            is TripDetailUiIntent.OnMoveStopDown ->
                moveStop(intent.currentOrder, intent.currentOrder + 1)

            is TripDetailUiIntent.OnEditStopClicked ->
                _uiState.update { it.copy(editingStop = intent.stop, isEditStopDialogVisible = true) }

            TripDetailUiIntent.OnDismissEditStopDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(editingStop = null, isEditStopDialogVisible = false) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnEditStopConfirmed ->
                editStop(intent.stopId, intent.placeName, intent.latitude, intent.longitude)

            is TripDetailUiIntent.OnRemoveStopClicked ->
                _uiState.update { it.copy(stopToRemove = intent.stop, isRemoveStopDialogVisible = true) }

            TripDetailUiIntent.OnRemoveStopConfirmed ->
                removeStop()

            TripDetailUiIntent.OnDismissRemoveStopDialog ->
                _uiState.update { it.copy(stopToRemove = null, isRemoveStopDialogVisible = false) }

            is TripDetailUiIntent.OnMarkStopDepartedClicked ->
                markStopDeparted(intent.stopId)

            is TripDetailUiIntent.OnUndoMarkStopDepartedClicked ->
                undoMarkStopDeparted()

            is TripDetailUiIntent.OnSearchQueryChanged -> {
                searchJob?.cancel()
                if (intent.query.isBlank()) {
                    clearSearchState()
                } else {
                    searchJob = viewModelScope.launch {
                        _uiState.update { it.copy(isSearchingPlaces = true, placeDetailError = null) }
                        delay(SEARCH_DEBOUNCE_MILLIS)
                        val result = searchPlacesUseCase(intent.query)
                        result.onSuccess { suggestions ->
                            _uiState.update { it.copy(placeSuggestions = suggestions, isSearchingPlaces = false, searchError = null) }
                        }.onFailure { error ->
                            _uiState.update { it.copy(isSearchingPlaces = false, searchError = error.message, placeSuggestions = emptyList()) }
                        }
                    }
                }
            }

            is TripDetailUiIntent.OnSuggestionSelected -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isResolvingPlace = true, placeDetailError = null) }
                    val result = getPlaceDetailUseCase(intent.suggestion.placeId)
                    result.onSuccess { detail ->
                        _uiState.update { it.copy(isResolvingPlace = false, selectedPlaceDetail = detail, placeSuggestions = emptyList()) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(isResolvingPlace = false, placeDetailError = error.message ?: UNKNOWN_ERROR) }
                    }
                }
            }
        }
    }

    private fun markStopDeparted(stopId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { markStopDepartedUseCase(tripId, stopId) }
            result.onFailure { error ->
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun undoMarkStopDeparted() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { undoMarkStopDepartedUseCase(tripId) }
            result.onFailure { error ->
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun moveStop(fromOrder: Int, toOrder: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { moveStopUseCase(tripId, fromOrder, toOrder) }
            result.onFailure { error ->
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun editStop(stopId: String, placeName: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { editStopUseCase(stopId, placeName, latitude, longitude) }
                .onSuccess {
                    searchJob?.cancel()
                    clearSearchState()
                    placeSearchRepository.resetSession()
                    _uiState.update { it.copy(editingStop = null, isEditStopDialogVisible = false, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }

    private fun removeStop() {
        val stop = _uiState.value.stopToRemove ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { removeStopUseCase(tripId, stop.id) }
                .onSuccess {
                    _uiState.update { it.copy(stopToRemove = null, isRemoveStopDialogVisible = false, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
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
            withMinimumDuration { setStopUseCase(tripId, placeName, latitude, longitude, stopType) }
                .onSuccess {
                    searchJob?.cancel()
                    clearSearchState()
                    placeSearchRepository.resetSession()
                    _uiState.update { dismissDialog(it).copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }

    private fun clearSearchState() {
        _uiState.update {
            it.copy(
                placeSuggestions = emptyList(),
                isSearchingPlaces = false,
                searchError = null,
                selectedPlaceDetail = null,
                isResolvingPlace = false,
                placeDetailError = null,
            )
        }
    }
}
