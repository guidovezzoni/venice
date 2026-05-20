package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.Stop

data class TripDetailUiState(
    val tripId: String = "",
    val startingPoint: Stop? = null,
    val destination: Stop? = null,
    val intermediateStops: List<Stop> = emptyList(),
    val isLoading: Boolean = false,
    val isSetStartingPointDialogVisible: Boolean = false,
    val isSetDestinationDialogVisible: Boolean = false,
    val isAddStopDialogVisible: Boolean = false,
    val canAddMoreStops: Boolean = false,
)
