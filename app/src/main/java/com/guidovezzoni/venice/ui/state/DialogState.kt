package com.guidovezzoni.venice.ui.state

import com.guidovezzoni.venice.domain.model.Stop

sealed interface DialogState {
    data object None : DialogState
    data object SetStartingPoint : DialogState
    data object SetDestination : DialogState
    data object AddStop : DialogState
    data class EditStop(val stop: Stop) : DialogState
    data class RemoveStop(val stop: Stop) : DialogState
}
