package com.guidovezzoni.venice.data.network

import com.guidovezzoni.venice.BuildConfig
import com.guidovezzoni.venice.data.network.dto.DirectionsResponse
import com.guidovezzoni.venice.domain.model.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

private const val ROUTES_API_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
private const val FIELD_MASK = "routes.legs.distanceMeters,routes.legs.duration,routes.legs.polyline.encodedPolyline"
private const val HEADER_API_KEY = "X-Goog-Api-Key"
private const val HEADER_FIELD_MASK = "X-Goog-FieldMask"
private const val TRAVEL_MODE = "DRIVE"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
private const val MINIMUM_STOP_COUNT = 2

// TODO: Replace with Retrofit when additional API endpoints are added
class DirectionsApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val mapsApiKey: String = BuildConfig.MAPS_API_KEY

    suspend fun fetchRoute(stops: List<Stop>): Result<DirectionsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(stops.size >= MINIMUM_STOP_COUNT) { "At least 2 stops are required to calculate a route" }
            val origin = stops.first()
            val destination = stops.last()
            val intermediates = stops.drop(1).dropLast(1)

            val requestBody = buildRequestBody(origin, destination, intermediates)

            val request = Request.Builder()
                .url(ROUTES_API_URL)
                .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader(HEADER_API_KEY, mapsApiKey)
                .addHeader(HEADER_FIELD_MASK, FIELD_MASK)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response body from Routes API")

            if (!response.isSuccessful) {
                throw IllegalStateException("Routes API request failed with status ${response.code}: $responseBody")
            }

            DirectionsResponse.fromJson(responseBody)
        }
    }

    private fun buildRequestBody(origin: Stop, destination: Stop, intermediates: List<Stop>): JSONObject {
        val body = JSONObject()
        body.put("origin", buildWaypoint(origin))
        body.put("destination", buildWaypoint(destination))
        body.put("travelMode", TRAVEL_MODE)
        if (intermediates.isNotEmpty()) {
            val intermediatesArray = JSONArray()
            intermediates.forEach { intermediatesArray.put(buildWaypoint(it)) }
            body.put("intermediates", intermediatesArray)
        }
        return body
    }

    private fun buildWaypoint(stop: Stop): JSONObject {
        val latLng = JSONObject()
            .put("latitude", stop.latitude)
            .put("longitude", stop.longitude)
        val location = JSONObject().put("latLng", latLng)
        return JSONObject().put("location", location)
    }
}
