package com.guidovezzoni.venice.data.network.dto

import org.json.JSONObject

data class DirectionsResponse(
    val legs: List<DirectionsLeg>,
) {
    companion object {
        fun fromJson(json: String): DirectionsResponse {
            val root = JSONObject(json)

            if (root.has("error")) {
                val apiError = root.getJSONObject("error")
                val message = apiError.optString("message", "Unknown API error")
                val status = apiError.optString("status", "UNKNOWN")
                error("Routes API error: $status — $message")
            }

            val legsArray = root.optJSONArray("routes")
                ?.takeIf { it.length() > 0 }
                ?.getJSONObject(0)
                ?.optJSONArray("legs")
                ?.takeIf { it.length() > 0 }
                ?: return DirectionsResponse(legs = emptyList())

            val legs = (0 until legsArray.length()).map { index ->
                val legObject = legsArray.getJSONObject(index)
                val distanceMetres = legObject.optInt("distanceMeters")
                val durationString = legObject.optString("duration")
                val durationSeconds = parseDurationSeconds(durationString)
                val encodedPolyline = legObject.optJSONObject("polyline")
                    ?.optString("encodedPolyline")
                DirectionsLeg(
                    distanceMetres = distanceMetres,
                    durationSeconds = durationSeconds,
                    encodedPolyline = encodedPolyline,
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
