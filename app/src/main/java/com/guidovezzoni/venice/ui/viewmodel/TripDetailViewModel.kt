package com.guidovezzoni.venice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.venice.core.analytics.AnalyticsClient
import com.guidovezzoni.venice.core.analytics.AnalyticsEvent
import com.guidovezzoni.venice.core.analytics.AnalyticsOperation
import com.guidovezzoni.venice.core.analytics.AnalyticsScreen
import com.guidovezzoni.venice.core.analytics.DistanceBand
import com.guidovezzoni.venice.core.analytics.DurationBand
import com.guidovezzoni.venice.core.analytics.StopTypeParam
import com.guidovezzoni.venice.core.analytics.toStopTypeParam
import com.guidovezzoni.venice.core.analytics.classifyAnalyticsError
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import com.guidovezzoni.venice.domain.usecase.CalculateRouteUseCase
import com.guidovezzoni.venice.domain.usecase.EditStopUseCase
import com.guidovezzoni.venice.domain.usecase.GetPlaceDetailUseCase
import com.guidovezzoni.venice.domain.usecase.MarkStopDepartedUseCase
import com.guidovezzoni.venice.domain.usecase.MoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveLegsUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveTripUseCase
import com.guidovezzoni.venice.domain.usecase.RemoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.SearchPlacesUseCase
import com.guidovezzoni.venice.domain.usecase.SetStopUseCase
import com.guidovezzoni.venice.domain.usecase.UndoMarkStopDepartedUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.DialogState
import com.guidovezzoni.venice.ui.state.PlaceSearchState
import com.guidovezzoni.venice.ui.state.RouteCalculationState
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.util.formatCoordinates
import com.guidovezzoni.venice.ui.util.formatDistance
import com.guidovezzoni.venice.ui.util.formatDuration
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private data class TripTotalsAndPromptState(
    val formattedTotalDistance: String?,
    val formattedTotalDuration: String?,
    val isRouteRecalculationPromptVisible: Boolean,
)

private const val ARG_TRIP_ID = "tripId"
private const val STARTING_POINT_ORDER = 0
private const val MAX_STOP_COUNT = 25
private const val UNKNOWN_ERROR = "Unknown error"
private const val SEARCH_DEBOUNCE_MILLIS = 300L

// TODO remove Android references that could be abstracted out
@HiltViewModel
@Suppress("LongParameterList")
class TripDetailViewModel @Inject constructor(
    private val application: Application,
    private val setStopUseCase: SetStopUseCase,
    private val moveStopUseCase: MoveStopUseCase,
    private val editStopUseCase: EditStopUseCase,
    private val removeStopUseCase: RemoveStopUseCase,
    private val markStopDepartedUseCase: MarkStopDepartedUseCase,
    private val undoMarkStopDepartedUseCase: UndoMarkStopDepartedUseCase,
    private val calculateRouteUseCase: CalculateRouteUseCase,
    observeStopsUseCase: ObserveStopsUseCase,
    observeLegsUseCase: ObserveLegsUseCase,
    observeTripUseCase: ObserveTripUseCase,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val placeSearchRepository: PlaceSearchRepository,
    private val analyticsClient: AnalyticsClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle[ARG_TRIP_ID])

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(TripDetailUiState(tripId = tripId))
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TripDetailUiEffect>()
    val uiEffect: SharedFlow<TripDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        analyticsClient.logEvent(AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_DETAIL))
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
                        formattedStopCoordinates = stops.associate { stop ->
                            stop.id to formatCoordinates(stop.latitude, stop.longitude)
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        observeLegsUseCase(tripId)
            .onEach { legs ->
                val locale = Locale.getDefault()
                val resources = application.resources
                _uiState.update {
                    it.copy(
                        legs = legs,
                        formattedLegDistances = legs.associate { leg ->
                            leg.fromStopId to formatDistance(leg.distanceMetres, locale, resources)
                        },
                        formattedLegDurations = legs.associate { leg ->
                            leg.fromStopId to formatDuration(leg.durationSeconds, resources)
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId)) { stops, legs ->
            if (legs.size == stops.size - 1 && stops.size >= 2) {
                val formattedTotalDistance = formatDistance(
                    legs.sumOf { it.distanceMetres },
                    Locale.getDefault(),
                    application.resources,
                )
                val totalDurationSeconds = legs.sumOf { it.durationSeconds.toLong() }.toInt()
                val formattedTotalDuration = formatDuration(totalDurationSeconds, application.resources)
                TripTotalsAndPromptState(
                    formattedTotalDistance = formattedTotalDistance,
                    formattedTotalDuration = formattedTotalDuration,
                    isRouteRecalculationPromptVisible = false,
                )
            } else {
                TripTotalsAndPromptState(
                    formattedTotalDistance = null,
                    formattedTotalDuration = null,
                    isRouteRecalculationPromptVisible = stops.size >= 2,
                )
            }
        }
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        formattedTotalDistance = result.formattedTotalDistance,
                        formattedTotalDuration = result.formattedTotalDuration,
                        isRouteRecalculationPromptVisible = result.isRouteRecalculationPromptVisible,
                    )
                }
            }
            .launchIn(viewModelScope)

        observeTripUseCase(tripId)
            .onEach { trip ->
                if (trip != null) {
                    _uiState.update { it.copy(tripName = trip.name) }
                }
            }
            .launchIn(viewModelScope)

        combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId)) { stops, legs ->
            val isComplete = legs.size == stops.size - 1 && stops.size >= 2
            val routeState = if (isComplete) "complete" else "none"
            analyticsClient.logEvent(AnalyticsEvent.TripOpened(stops.size, routeState))
        }
            .take(1)
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TripDetailUiIntent) {
        when (intent) {
            TripDetailUiIntent.OnSetStartingPointClicked ->
                _uiState.update { it.copy(dialogState = DialogState.SetStartingPoint) }

            TripDetailUiIntent.OnDismissStartingPointDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(dialogState = DialogState.None) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnStartingPointConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.STARTING_POINT)

            TripDetailUiIntent.OnSetDestinationClicked ->
                _uiState.update { it.copy(dialogState = DialogState.SetDestination) }

            TripDetailUiIntent.OnDismissDestinationDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(dialogState = DialogState.None) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnDestinationConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.DESTINATION)

            TripDetailUiIntent.OnAddStopClicked ->
                _uiState.update { it.copy(dialogState = DialogState.AddStop) }

            TripDetailUiIntent.OnDismissAddStopDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(dialogState = DialogState.None) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnAddStopConfirmed ->
                setStop(intent.placeName, intent.latitude, intent.longitude, StopType.INTERMEDIATE)

            is TripDetailUiIntent.OnMoveStopUp ->
                moveStop(intent.currentOrder, intent.currentOrder - 1)

            is TripDetailUiIntent.OnMoveStopDown ->
                moveStop(intent.currentOrder, intent.currentOrder + 1)

            is TripDetailUiIntent.OnEditStopClicked ->
                _uiState.update { it.copy(dialogState = DialogState.EditStop(intent.stop)) }

            TripDetailUiIntent.OnDismissEditStopDialog -> {
                searchJob?.cancel()
                _uiState.update { it.copy(dialogState = DialogState.None) }
                clearSearchState()
                placeSearchRepository.resetSession()
            }

            is TripDetailUiIntent.OnEditStopConfirmed ->
                editStop(intent.stopId, intent.placeName, intent.latitude, intent.longitude)

            is TripDetailUiIntent.OnRemoveStopClicked ->
                _uiState.update { it.copy(dialogState = DialogState.RemoveStop(intent.stop)) }

            TripDetailUiIntent.OnRemoveStopConfirmed ->
                removeStop()

            TripDetailUiIntent.OnDismissRemoveStopDialog ->
                _uiState.update { it.copy(dialogState = DialogState.None) }

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
                        _uiState.update {
                            it.copy(
                                placeSearchState = it.placeSearchState.copy(
                                    isSearching = true,
                                    placeDetailError = null,
                                ),
                            )
                        }
                        delay(SEARCH_DEBOUNCE_MILLIS)
                        val result = searchPlacesUseCase(intent.query)
                        result.onSuccess { suggestions ->
                            analyticsClient.logEvent(
                                AnalyticsEvent.PlaceSearchPerformed(suggestionCount = suggestions.size),
                            )
                            _uiState.update {
                                it.copy(
                                    placeSearchState = it.placeSearchState.copy(
                                        suggestions = suggestions,
                                        isSearching = false,
                                        searchError = null,
                                    ),
                                )
                            }
                        }.onFailure { error ->
                            val errorType = classifyAnalyticsError(error)
                            analyticsClient.logEvent(
                                AnalyticsEvent.OperationFailed(
                                    operation = AnalyticsOperation.SEARCH_PLACE,
                                    errorType = errorType,
                                ),
                            )
                            analyticsClient.trackException(error, AnalyticsOperation.SEARCH_PLACE)
                            _uiState.update {
                                it.copy(
                                    placeSearchState = it.placeSearchState.copy(
                                        isSearching = false,
                                        searchError = error.message,
                                        suggestions = emptyList(),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            is TripDetailUiIntent.OnSuggestionSelected -> {
                val suggestionPosition = _uiState.value.placeSearchState.suggestions.indexOf(intent.suggestion)
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            placeSearchState = it.placeSearchState.copy(
                                isResolvingPlace = true,
                                placeDetailError = null,
                            ),
                        )
                    }
                    val result = getPlaceDetailUseCase(intent.suggestion.placeId)
                    result.onSuccess { detail ->
                        analyticsClient.logEvent(
                            AnalyticsEvent.PlaceSuggestionSelected(suggestionPosition = suggestionPosition),
                        )
                        _uiState.update {
                            it.copy(
                                placeSearchState = it.placeSearchState.copy(
                                    isResolvingPlace = false,
                                    selectedPlaceDetail = detail,
                                    suggestions = emptyList(),
                                ),
                            )
                        }
                    }.onFailure { error ->
                        val errorType = classifyAnalyticsError(error)
                        analyticsClient.logEvent(
                            AnalyticsEvent.OperationFailed(
                                operation = AnalyticsOperation.RESOLVE_PLACE,
                                errorType = errorType,
                            ),
                        )
                        analyticsClient.trackException(error, AnalyticsOperation.RESOLVE_PLACE)
                        _uiState.update {
                            it.copy(
                                placeSearchState = it.placeSearchState.copy(
                                    isResolvingPlace = false,
                                    placeDetailError = error.message ?: UNKNOWN_ERROR,
                                ),
                            )
                        }
                    }
                }
            }

            TripDetailUiIntent.OnCalculateRouteClicked -> calculateRoute()

            is TripDetailUiIntent.OnNavigateToStopClicked -> navigateToStop(intent.stopId)
        }
    }

    private fun markStopDeparted(stopId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { markStopDepartedUseCase(tripId, stopId) }
            result.onSuccess { stop ->
                analyticsClient.logEvent(
                    AnalyticsEvent.StopDeparted(stopPosition = stop.order, stopCount = currentStopCount()),
                )
            }.onFailure { error ->
                val errorType = classifyAnalyticsError(error)
                analyticsClient.logEvent(
                    AnalyticsEvent.OperationFailed(
                        operation = AnalyticsOperation.MARK_DEPARTED,
                        errorType = errorType,
                    ),
                )
                analyticsClient.trackException(error, AnalyticsOperation.MARK_DEPARTED)
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun undoMarkStopDeparted() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { undoMarkStopDepartedUseCase(tripId) }
            result.onSuccess { stop ->
                analyticsClient.logEvent(AnalyticsEvent.StopDepartureUndone(stopPosition = stop.order))
            }.onFailure { error ->
                val errorType = classifyAnalyticsError(error)
                analyticsClient.logEvent(
                    AnalyticsEvent.OperationFailed(
                        operation = AnalyticsOperation.UNDO_MARK_DEPARTED,
                        errorType = errorType,
                    ),
                )
                analyticsClient.trackException(error, AnalyticsOperation.UNDO_MARK_DEPARTED)
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun moveStop(fromOrder: Int, toOrder: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withMinimumDuration { moveStopUseCase(tripId, fromOrder, toOrder) }
            result.onSuccess {
                val direction = if (toOrder < fromOrder) "up" else "down"
                analyticsClient.logEvent(
                    AnalyticsEvent.StopReordered(direction = direction, stopCount = currentStopCount()),
                )
            }.onFailure { error ->
                val errorType = classifyAnalyticsError(error)
                analyticsClient.logEvent(
                    AnalyticsEvent.OperationFailed(
                        operation = AnalyticsOperation.MOVE_STOP,
                        errorType = errorType,
                    ),
                )
                analyticsClient.trackException(error, AnalyticsOperation.MOVE_STOP)
                _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun editStop(stopId: String, placeName: String, latitude: Double, longitude: Double) {
        val stopTypeParam = stopTypeForStop(stopId)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { editStopUseCase(stopId, placeName, latitude, longitude) }
                .onSuccess {
                    analyticsClient.logEvent(AnalyticsEvent.StopEdited(stopTypeParam))
                    searchJob?.cancel()
                    clearSearchState()
                    placeSearchRepository.resetSession()
                    _uiState.update { it.copy(dialogState = DialogState.None, isLoading = false) }
                }
                .onFailure { error ->
                    val errorType = classifyAnalyticsError(error)
                    analyticsClient.logEvent(
                        AnalyticsEvent.OperationFailed(
                            operation = AnalyticsOperation.EDIT_STOP,
                            errorType = errorType,
                        ),
                    )
                    analyticsClient.trackException(error, AnalyticsOperation.EDIT_STOP)
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }

    private fun removeStop() {
        val stop = (_uiState.value.dialogState as? DialogState.RemoveStop)?.stop ?: return
        val stopTypeParam = stopTypeForStop(stop.id)
        val stopCountBeforeRemove = currentStopCount()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { removeStopUseCase(tripId, stop.id) }
                .onSuccess {
                    analyticsClient.logEvent(
                        AnalyticsEvent.StopRemoved(stopTypeParam, stopCountBeforeRemove - 1),
                    )
                    _uiState.update { it.copy(dialogState = DialogState.None, isLoading = false) }
                }
                .onFailure { error ->
                    val errorType = classifyAnalyticsError(error)
                    analyticsClient.logEvent(
                        AnalyticsEvent.OperationFailed(
                            operation = AnalyticsOperation.REMOVE_STOP,
                            errorType = errorType,
                        ),
                    )
                    analyticsClient.trackException(error, AnalyticsOperation.REMOVE_STOP)
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
    ) {
        val stopCountBeforeAdd = currentStopCount()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { setStopUseCase(tripId, placeName, latitude, longitude, stopType) }
                .onSuccess {
                    analyticsClient.logEvent(
                        AnalyticsEvent.StopAdded(stopType.toStopTypeParam(), stopCountBeforeAdd + 1),
                    )
                    searchJob?.cancel()
                    clearSearchState()
                    placeSearchRepository.resetSession()
                    _uiState.update { it.copy(dialogState = DialogState.None, isLoading = false) }
                }
                .onFailure { error ->
                    val errorType = classifyAnalyticsError(error)
                    analyticsClient.logEvent(
                        AnalyticsEvent.OperationFailed(
                            operation = AnalyticsOperation.SET_STOP,
                            errorType = errorType,
                        ),
                    )
                    analyticsClient.trackException(error, AnalyticsOperation.SET_STOP)
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripDetailUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }

    private fun calculateRoute() {
        val state = _uiState.value
        val stops = buildList {
            state.startingPoint?.let { add(it) }
            addAll(state.intermediateStops)
            state.destination?.let { add(it) }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(routeCalculationState = RouteCalculationState.Calculating) }
            val result = withMinimumDuration { calculateRouteUseCase(tripId, stops) }
            result.onSuccess { legs ->
                val totalDistanceMetres = legs.sumOf { it.distanceMetres }
                val totalDurationSeconds = legs.sumOf { it.durationSeconds }
                analyticsClient.logEvent(
                    AnalyticsEvent.RouteCalculated(
                        stopCount = currentStopCount(),
                        legCount = legs.size,
                        distanceBand = DistanceBand.fromMetres(totalDistanceMetres),
                        durationBand = DurationBand.fromSeconds(totalDurationSeconds),
                    ),
                )
                _uiState.update { it.copy(routeCalculationState = RouteCalculationState.Idle) }
            }.onFailure { error ->
                val errorType = classifyAnalyticsError(error)
                analyticsClient.logEvent(
                    AnalyticsEvent.OperationFailed(
                        operation = AnalyticsOperation.CALCULATE_ROUTE,
                        errorType = errorType,
                    ),
                )
                analyticsClient.trackException(error, AnalyticsOperation.CALCULATE_ROUTE)
                _uiState.update {
                    it.copy(
                        routeCalculationState = RouteCalculationState.Error(
                            error.message ?: UNKNOWN_ERROR,
                        ),
                    )
                }
            }
        }
    }

    private fun navigateToStop(stopId: String) {
        val state = _uiState.value
        val allStops = buildList {
            state.startingPoint?.let { add(it) }
            addAll(state.intermediateStops)
            state.destination?.let { add(it) }
        }
        val stop = allStops.firstOrNull { it.id == stopId } ?: return
        val stopPosition = allStops.indexOf(stop)
        val stopType = stopTypeForStop(stopId)
        analyticsClient.logEvent(AnalyticsEvent.NavigationLaunched(stopType, stopPosition))
        viewModelScope.launch {
            _uiEffect.emit(
                TripDetailUiEffect.LaunchNavigation(
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    placeName = stop.placeName,
                ),
            )
        }
    }

    private fun clearSearchState() {
        _uiState.update { it.copy(placeSearchState = PlaceSearchState()) }
    }

    private fun currentStopCount(): Int =
        (if (_uiState.value.startingPoint != null) 1 else 0) +
            _uiState.value.intermediateStops.size +
            (if (_uiState.value.destination != null) 1 else 0)

    private fun stopTypeForStop(stopId: String): StopTypeParam {
        val state = _uiState.value
        return when {
            state.startingPoint?.id == stopId -> StopTypeParam.STARTING_POINT
            state.destination?.id == stopId -> StopTypeParam.DESTINATION
            else -> StopTypeParam.INTERMEDIATE
        }
    }

}
