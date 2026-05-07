package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.Trip

data class TripListUiState(
    val trips: List<Trip> = emptyList(),
    val isCreateDialogVisible: Boolean = false,
    val tripNameInput: String = "",
    val isLoading: Boolean = false,
)
