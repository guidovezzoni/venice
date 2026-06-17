package com.guidovezzoni.venice.data.network

import com.guidovezzoni.venice.BuildConfig
import com.guidovezzoni.venice.data.network.dto.DirectionsResponse
import com.guidovezzoni.venice.domain.model.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

private const val DIRECTIONS_BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"

// TODO: Replace with Retrofit when additional API endpoints are added
class DirectionsApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val mapsApiKey: String = BuildConfig.MAPS_API_KEY

    suspend fun fetchRoute(stops: List<Stop>): Result<DirectionsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(stops.size >= 2) { "At least 2 stops are required to calculate a route" }
            val origin = stops.first()
            val destination = stops.last()
            val waypoints = stops.drop(1).dropLast(1)

            val urlBuilder = StringBuilder(DIRECTIONS_BASE_URL)
            urlBuilder.append("?origin=${origin.latitude},${origin.longitude}")
            urlBuilder.append("&destination=${destination.latitude},${destination.longitude}")
            if (waypoints.isNotEmpty()) {
                val waypointsParam = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
                urlBuilder.append("&waypoints=$waypointsParam")
            }
            urlBuilder.append("&key=$mapsApiKey")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response body from Directions API")

            if (!response.isSuccessful) {
                throw IllegalStateException("Directions API request failed with status ${response.code}")
            }

            DirectionsResponse.fromJson(responseBody)
        }
    }

}
