# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt — the library ships its own consumer rules; only keep annotated entry points
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Firebase Crashlytics — keep component registrar classes referenced by name in the merged manifest.
# Firebase's ComponentDiscoveryService instantiates registrars by the class names listed in manifest
# meta-data. R8 renames these classes, causing instantiation to silently fail and the Crashlytics
# component to never register. The Crashlytics Gradle plugin is supposed to generate these keep
# rules, but has a compatibility gap with AGP 9.x.
-keep class com.google.firebase.crashlytics.CrashlyticsRegistrar { *; }
-keep class com.google.firebase.crashlytics.FirebaseCrashlyticsKtxRegistrar { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Google Places SDK — keep only the classes the app references directly
-keep class com.google.android.libraries.places.api.Places { *; }
-keep class com.google.android.libraries.places.api.model.AutocompletePrediction { *; }
-keep class com.google.android.libraries.places.api.model.AutocompleteSessionToken { *; }
-keep class com.google.android.libraries.places.api.model.Place { *; }
-keep class com.google.android.libraries.places.api.net.FetchPlaceRequest { *; }
-keep class com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest { *; }
-keep class com.google.android.libraries.places.api.net.PlacesClient { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin serialization (if used in the future)
-keepattributes *Annotation*, InnerClasses

# Keep data classes used by Room or as API models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
