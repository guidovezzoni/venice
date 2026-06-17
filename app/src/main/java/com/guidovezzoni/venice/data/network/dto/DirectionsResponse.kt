package com.guidovezzoni.venice.data.network.dto

import org.json.JSONObject

data class DirectionsResponse(
    val legs: List<DirectionsLeg>,
) {
    companion object {
        private const val STATUS_OK = "OK"

        fun fromJson(json: String): DirectionsResponse {
            val root = JSONObject(json)
            val status = root.optString("status")
            if (status != STATUS_OK) {
                val errorMessage = root.optString("error_message", "Directions API request failed")
                throw IllegalStateException("Directions API error: $status — $errorMessage")
            }
            val routes = root.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return DirectionsResponse(legs = emptyList())
            }
            val firstRoute = routes.getJSONObject(0)
            val legsArray = firstRoute.optJSONArray("legs")
            if (legsArray == null || legsArray.length() == 0) {
                return DirectionsResponse(legs = emptyList())
            }
            val legs = mutableListOf<DirectionsLeg>()
            for (index in 0 until legsArray.length()) {
                val legObject = legsArray.getJSONObject(index)
                val distanceMetres = legObject.optJSONObject("distance")?.optInt("value")
                val durationSeconds = legObject.optJSONObject("duration")?.optInt("value")
                legs.add(
                    DirectionsLeg(
                        distanceMetres = distanceMetres,
                        durationSeconds = durationSeconds,
                        encodedPolyline = null,
                    )
                )
            }
            return DirectionsResponse(legs = legs)
        }
    }
}

data class DirectionsLeg(
    val distanceMetres: Int?,
    val durationSeconds: Int?,
    val encodedPolyline: String?,
)
