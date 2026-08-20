package com.guidovezzoni.venice.core.analytics

import com.guidovezzoni.venice.domain.model.StopType

enum class StopTypeParam(val value: String) {
    STARTING_POINT("starting_point"),
    DESTINATION("destination"),
    INTERMEDIATE("intermediate"),
}

fun StopType.toStopTypeParam(): StopTypeParam = when (this) {
    StopType.STARTING_POINT -> StopTypeParam.STARTING_POINT
    StopType.DESTINATION -> StopTypeParam.DESTINATION
    StopType.INTERMEDIATE -> StopTypeParam.INTERMEDIATE
}
