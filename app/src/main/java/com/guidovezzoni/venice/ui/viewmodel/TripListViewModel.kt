package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.venice.domain.repository.TripRepository
import com.guidovezzoni.venice.domain.usecase.CreateTripUseCase
import com.guidovezzoni.venice.ui.effect.TripListUiEffect
import com.guidovezzoni.venice.ui.util.withMinimumDuration
import com.guidovezzoni.venice.ui.intent.TripListUiIntent
import com.guidovezzoni.venice.ui.state.TripListUiState
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

@HiltViewModel
class TripListViewModel @Inject constructor(
    private val createTripUseCase: CreateTripUseCase,
    tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TripListUiEffect>()
    val uiEffect: SharedFlow<TripListUiEffect> = _uiEffect.asSharedFlow()

    init {
        tripRepository.observeTrips()
            .onEach { trips -> _uiState.update { it.copy(trips = trips) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TripListUiIntent) {
        when (intent) {
            TripListUiIntent.OnCreateTripClicked -> _uiState.update {
                it.copy(isCreateDialogVisible = true, tripNameInput = "")
            }
            TripListUiIntent.OnDismissCreateDialog -> _uiState.update {
                it.copy(isCreateDialogVisible = false, tripNameInput = "")
            }
            is TripListUiIntent.OnTripNameChanged -> _uiState.update {
                it.copy(tripNameInput = intent.name)
            }
            TripListUiIntent.ConfirmCreateTrip -> createTrip()
            is TripListUiIntent.OnTripClicked -> viewModelScope.launch {
                _uiEffect.emit(TripListUiEffect.NavigateToTripDetail(intent.tripId))
            }
        }
    }

    private fun createTrip() {
        val name = _uiState.value.tripNameInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withMinimumDuration { createTripUseCase(name) }
                .onSuccess { trip ->
                    _uiState.update { it.copy(isLoading = false, isCreateDialogVisible = false, tripNameInput = "") }
                    _uiEffect.emit(TripListUiEffect.NavigateToTripDetail(trip.id))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(TripListUiEffect.ShowError(error.message ?: UNKNOWN_ERROR))
                }
        }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Unknown error"
    }
}
