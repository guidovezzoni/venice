package com.guidovezzoni.venice

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.guidovezzoni.venice.core.analytics.AnalyticsClient
import com.guidovezzoni.venice.core.analytics.AnalyticsUserProperty
import com.guidovezzoni.venice.ui.util.isImperialLocale
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class VeniceApplication : Application() {

    @Inject lateinit var analyticsClient: AnalyticsClient

    override fun onCreate() {
        super.onCreate()
        val unit = if (isImperialLocale(Locale.getDefault())) "imperial" else "metric"
        analyticsClient.setUserProperty(AnalyticsUserProperty.DistanceUnit(unit))
        Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
    }
}

