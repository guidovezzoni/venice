package com.guidovezzoni.venice.data.network.dto

import org.json.JSONObject

data class DirectionsResponse(
    val legs: List<DirectionsLeg>,
) {
    companion object {
        fun fromJson(json: String): DirectionsResponse {
            val root = JSONObject(json)

            if (root.has("error")) {
                val error = root.getJSONObject("error")
                val message = error.optString("message", "Unknown API error")
                val status = error.optString("status", "UNKNOWN")
                throw IllegalStateException("Routes API error: $status — $message")
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
                val distanceMetres = legObject.optInt("distanceMeters")
                val durationString = legObject.optString("duration")
                val durationSeconds = parseDurationSeconds(durationString)
                val encodedPolyline = legObject.optJSONObject("polyline")
                    ?.optString("encodedPolyline")
                legs.add(
                    DirectionsLeg(
                        distanceMetres = distanceMetres,
                        durationSeconds = durationSeconds,
                        encodedPolyline = encodedPolyline,
                    )
                )
            }
            return DirectionsResponse(legs = legs)
        }

        private fun parseDurationSeconds(duration: String?): Int? {
            if (duration.isNullOrBlank()) return null
            return duration.removeSuffix("s").toIntOrNull()
        }
    }
}

data class DirectionsLeg(
    val distanceMetres: Int?,
    val durationSeconds: Int?,
    val encodedPolyline: String?,
)
