package com.guidovezzoni.venice.core.analytics

private const val SECONDS_1H = 3_600
private const val SECONDS_3H = 10_800
private const val SECONDS_6H = 21_600
private const val SECONDS_12H = 43_200

enum class DurationBand(val value: String) {
    UNDER_1H("under_1h"),
    RANGE_1_3H("1_3h"),
    RANGE_3_6H("3_6h"),
    RANGE_6_12H("6_12h"),
    OVER_12H("over_12h"),
    ;

    companion object {
        fun fromSeconds(seconds: Int): DurationBand = when {
            seconds < SECONDS_1H -> UNDER_1H
            seconds < SECONDS_3H -> RANGE_1_3H
            seconds < SECONDS_6H -> RANGE_3_6H
            seconds < SECONDS_12H -> RANGE_6_12H
            else -> OVER_12H
        }
    }
}
