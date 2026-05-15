package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.Stop

data class TripDetailUiState(
    val tripId: String = "",
    val startingPoint: Stop? = null,
    val isLoading: Boolean = false,
    val isSetStartingPointDialogVisible: Boolean = false,
)
