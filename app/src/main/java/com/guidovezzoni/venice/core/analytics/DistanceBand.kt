package com.guidovezzoni.venice.core.analytics

private const val METRES_50KM = 50_000
private const val METRES_200KM = 200_000
private const val METRES_500KM = 500_000
private const val METRES_1000KM = 1_000_000

enum class DistanceBand(val value: String) {
    UNDER_50KM("under_50km"),
    RANGE_50_200KM("50_200km"),
    RANGE_200_500KM("200_500km"),
    RANGE_500_1000KM("500_1000km"),
    OVER_1000KM("over_1000km"),
    ;

    companion object {
        fun fromMetres(metres: Int): DistanceBand = when {
            metres < METRES_50KM -> UNDER_50KM
            metres < METRES_200KM -> RANGE_50_200KM
            metres < METRES_500KM -> RANGE_200_500KM
            metres < METRES_1000KM -> RANGE_500_1000KM
            else -> OVER_1000KM
        }
    }
}
