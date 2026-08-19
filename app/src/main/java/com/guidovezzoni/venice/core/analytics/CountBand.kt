package com.guidovezzoni.venice.core.analytics

enum class CountBand(val value: String) {
    ZERO("0"),
    ONE("1"),
    RANGE_2_5("2_5"),
    SIX_PLUS("6_plus"),
    ;

    companion object {
        private const val RANGE_2_5_MAX = 5

        fun fromCount(count: Int): CountBand = when {
            count == 0 -> ZERO
            count == 1 -> ONE
            count <= RANGE_2_5_MAX -> RANGE_2_5
            else -> SIX_PLUS
        }
    }
}
