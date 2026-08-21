## 1. Prerequisites: Firebase console setup (manual)

- [ ] 1.1 Confirm a Firebase project exists (the story notes one is partially set up); create it if not.
- [ ] 1.2 Register application ID `com.guidovezzoni.venice` (release) in the Firebase console.
- [ ] 1.3 Register application ID `com.guidovezzoni.venice.debug` (debug, due to `applicationIdSuffix`) in the Firebase console.
- [ ] 1.4 Download `google-services.json` covering both registered app entries; place it at `app/google-services.json` for local development only (never commit).
- [ ] 1.5 In GitHub repo settings, add a `GOOGLE_SERVICES_JSON_BASE64` secret containing the base64-encoded contents of `google-services.json`, for use by CI/CD (task 12).

## 2. Prerequisites: Gradle, dependencies, and gitignore

- [ ] 2.1 Add `firebaseBom` version and `firebase-bom` / `firebase-analytics` library entries to `[versions]` / `[libraries]` in `gradle/libs.versions.toml` (no hardcoded version strings; `firebase-analytics` has no explicit version, managed by the BOM platform).
- [ ] 2.2 Add a `google-services` plugin entry (`com.google.gms:google-services`) to `[plugins]` in `gradle/libs.versions.toml`.
- [ ] 2.3 Declare `alias(libs.plugins.google.services) apply false` in the root `build.gradle.kts`, alongside the other `apply false` plugins.
- [ ] 2.4 In `app/build.gradle.kts`, apply the `google-services` plugin and add `implementation(platform(libs.firebase.bom))` plus `implementation(libs.firebase.analytics)`.
- [ ] 2.5 Add `google-services.json` to `.gitignore`, positioned near `local.properties` / `keystore.properties`.
- [ ] 2.6 Verify `settings.gradle.kts`'s `google()` repository filter (`includeGroupByRegex("com\\.google.*")`) resolves both `com.google.firebase:*` and `com.google.gms:google-services` artifacts by running a dependency resolution (e.g. `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep firebase`); widen the filter only if resolution actually fails.
- [ ] 2.7 Add a `doFirst`/pre-check guard in `app/build.gradle.kts` that verifies `app/google-services.json` exists before the `google-services` plugin's task runs, throwing a `GradleException` that names the expected path and points to the Firebase console / the `GOOGLE_SERVICES_JSON_BASE64` CI secret when it is missing.
- [ ] 2.8 Manually verify the guard: temporarily rename `app/google-services.json` away, run `./gradlew assembleDebug`, confirm the custom clear error appears (not a generic plugin stack trace), then restore the file.

## 3. FirebaseAnalyticsProvider construction (BDD)

- [ ] 3.1 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with a mocked `FirebaseAnalytics` WHEN its constructor is inspected THEN it implements `AnalyticsProvider` and performs no static SDK lookup, in `FirebaseAnalyticsProviderTest`.
- [ ] 3.2 Implement: create `app/src/main/java/.../core/analytics/FirebaseAnalyticsProvider.kt` with a constructor-injected `FirebaseAnalytics` parameter (via `@Inject`), implementing `AnalyticsProvider` with stub `logEvent`/`setUserProperty`/`trackException` bodies.
- [ ] 3.3 Implement: add a `@Provides` function (in `AnalyticsModule.kt` or a small dedicated Firebase module) that returns `FirebaseAnalytics.getInstance(context)` using the injected `@ApplicationContext Context`, so `FirebaseAnalytics` itself is injectable.

## 4. Generic event property mapping (BDD)

- [ ] 4.1 Write test: GIVEN an `AnalyticsEvent` whose `properties` contains a `String` value WHEN `logEvent` is called THEN the resulting `Bundle` contains that value via `putString` under the same key, in `FirebaseAnalyticsProviderTest`.
- [ ] 4.2 Implement: build a `Bundle` from `event.properties`, mapping `String` values with `putString`.
- [ ] 4.3 Write test: GIVEN an `AnalyticsEvent` whose `properties` contains an `Int` value WHEN `logEvent` is called THEN the resulting `Bundle` contains that value via `putLong` under the same key, in `FirebaseAnalyticsProviderTest`.
- [ ] 4.4 Implement: extend the `Bundle` mapping to convert `Int` and `Long` values with `putLong`.
- [ ] 4.5 Write test: GIVEN `AnalyticsEvent.TripCreated(isFirstTrip = true)` WHEN `logEvent` is called THEN `FirebaseAnalytics.logEvent` is invoked with a `Bundle` containing `"is_first_trip"` mapped to the String `"true"`, not a Boolean, in `FirebaseAnalyticsProviderTest`.
- [ ] 4.6 Implement: extend the `Bundle` mapping to convert `Boolean` values to `"true"`/`"false"` Strings via `putString`.
- [ ] 4.7 Write test: GIVEN any `AnalyticsEvent` other than `ScreenViewed` WHEN `logEvent` is called THEN `FirebaseAnalytics.logEvent` is invoked with `event.name` as the event name argument, in `FirebaseAnalyticsProviderTest` (parameterised or looped over all 13 non-screen events).
- [ ] 4.8 Implement: wire `logEvent`'s default branch to call `firebaseAnalytics.logEvent(event.name, bundle)`.

## 5. ScreenViewed special-case mapping (BDD)

- [ ] 5.1 Write test: GIVEN `AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_LIST)` WHEN `logEvent` is called THEN `FirebaseAnalytics.logEvent` is invoked with `FirebaseAnalytics.Event.SCREEN_VIEW`, not `"screen_viewed"`, in `FirebaseAnalyticsProviderTest`.
- [ ] 5.2 Write test: GIVEN `AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_DETAIL)` WHEN `logEvent` is called THEN the resulting `Bundle` contains `"trip_detail"` keyed by `FirebaseAnalytics.Param.SCREEN_NAME`, in `FirebaseAnalyticsProviderTest`.
- [ ] 5.3 Implement: add a branch in `logEvent`, checked before the generic mapping, that special-cases `AnalyticsEvent.ScreenViewed` to call `firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screen.value))`.

## 6. User property forwarding (BDD)

- [ ] 6.1 Write test: GIVEN `AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5)` WHEN `setUserProperty` is called THEN `FirebaseAnalytics.setUserProperty` is invoked with `"trip_count_band"` and `"2_5"`, in `FirebaseAnalyticsProviderTest`.
- [ ] 6.2 Write test: GIVEN `AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL)` WHEN `setUserProperty` is called THEN `FirebaseAnalytics.setUserProperty` is invoked with `"distance_unit"` and `"imperial"`, in `FirebaseAnalyticsProviderTest`.
- [ ] 6.3 Implement: add an exhaustive `when (property)` mapping `AnalyticsUserProperty.TripCountBand` to `"trip_count_band" to band.value` and `AnalyticsUserProperty.DistanceUnit` to `"distance_unit" to unit.value`, and call `firebaseAnalytics.setUserProperty(name, value)` in `setUserProperty`.

## 7. trackException no-op (BDD)

- [ ] 7.1 Write test: GIVEN a `FirebaseAnalyticsProvider` with a mocked `FirebaseAnalytics` WHEN `trackException(RuntimeException("boom"), AnalyticsOperation.CREATE_TRIP)` is called THEN no method is invoked on the mocked `FirebaseAnalytics` instance and no exception propagates, in `FirebaseAnalyticsProviderTest`.
- [ ] 7.2 Implement: make `trackException`'s body a documented no-op (comment explaining Crashlytics is a future, separate provider), touching nothing on `firebaseAnalytics`.

## 8. Platform-limit enforcement (BDD)

- [ ] 8.1 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with an event whose `name` exceeds 40 characters THEN an `IllegalStateException` is thrown and `FirebaseAnalytics.logEvent` is not invoked, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.2 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `false` WHEN `logEvent` is called with an event whose `name` exceeds 40 characters THEN no exception is thrown, `FirebaseAnalytics.logEvent` is not invoked, and an error is logged, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.3 Implement: add an injected `isDebugBuild: Boolean` constructor parameter (default `BuildConfig.DEBUG`) and a private `reportPlatformLimitViolation(message: String)` that throws `IllegalStateException` when `isDebugBuild` is `true`, else logs via `Log.e` and returns; wire an event/parameter-name length+character+prefix validation into `logEvent` that calls it before forwarding to `FirebaseAnalytics`.
- [ ] 8.4 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with an event whose `properties` map contains a key starting with `"firebase_"`, `"google_"`, or `"ga_"` THEN an `IllegalStateException` is thrown and `FirebaseAnalytics.logEvent` is not invoked, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.5 Implement: extend the validation to reject reserved-prefix parameter names (`firebase_`, `google_`, `ga_`).
- [ ] 8.6 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with a String parameter value of exactly 100 characters THEN no exception is thrown and `FirebaseAnalytics.logEvent` is invoked, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.7 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with a String parameter value of 101 characters THEN an `IllegalStateException` is thrown, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.8 Implement: extend the validation to enforce the 100-character String value limit (boundary at exactly 100 accepted, 101 rejected).
- [ ] 8.9 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with an event carrying exactly 25 parameters THEN no exception is thrown, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.10 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `logEvent` is called with an event carrying 26 parameters THEN an `IllegalStateException` is thrown, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.11 Implement: extend the validation to enforce the maximum-25-parameters-per-event limit (boundary at exactly 25 accepted, 26 rejected).
- [ ] 8.12 Write test: GIVEN a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true` WHEN `setUserProperty` is called with a mapped property name exceeding 24 characters THEN an `IllegalStateException` is thrown and `FirebaseAnalytics.setUserProperty` is not invoked, in `FirebaseAnalyticsProviderTest`.
- [ ] 8.13 Implement: apply the same name-length/character/prefix validation (with the 24-character limit) to `setUserProperty` before forwarding to `FirebaseAnalytics`.

## 9. Thread safety (BDD)

- [ ] 9.1 Write test: GIVEN a `FirebaseAnalyticsProvider` with a mocked `FirebaseAnalytics` WHEN `logEvent` is invoked concurrently from multiple threads (e.g. via `runBlocking` + multiple launched coroutines on `Dispatchers.Default`) with distinct valid events THEN every call reaches the mocked `FirebaseAnalytics` exactly once with no exceptions, in `FirebaseAnalyticsProviderTest`.
- [ ] 9.2 Implement: confirm (and if needed, adjust) `FirebaseAnalyticsProvider` holds no mutable instance state, so no implementation change should be required beyond documenting thread-safety in a class-level KDoc comment, matching `DebugAnalyticsProvider`'s existing convention.

## 10. DI wiring (integration)

- [ ] 10.1 Add a `@Binds @IntoSet` function to the `main` source set's `AnalyticsModule.kt` (`app/src/main/java/.../di/AnalyticsModule.kt`) binding `FirebaseAnalyticsProvider` as an `AnalyticsProvider`.
- [ ] 10.2 Confirm `DebugAnalyticsModule.kt` (debug source set) is unchanged — the Firebase binding lives only in `main`.
- [ ] 10.3 Run existing `CompositeAnalyticsClientTest`, `DebugAnalyticsProviderTest`, `AnalyticsTrackingTest`, `AnalyticsUserPropertyTest`, `AnalyticsEventTest` to confirm none break with the new provider registered.

## 11. Manifest changes (integration)

- [ ] 11.1 Add `<meta-data android:name="google_analytics_automatic_screen_reporting_enabled" android:value="false" />` inside `<application>` in `app/src/main/AndroidManifest.xml`.
- [ ] 11.2 Confirm no other manifest change is made for `AD_ID` — this deliberately deviates from the source story's AC 19 per explicit user instruction; leave the permission exactly as Firebase's manifest merge adds it.
- [ ] 11.3 Review Firebase's other automatically-collected events (`first_open`, `session_start`, `user_engagement`, `app_update`, `app_remove`, `os_update`, `in_app_purchase`, etc.) against the tracking plan; confirm none has a manual replacement in this change and none is individually toggleable beyond `screen_view`.

## 12. CI/CD updates (integration)

- [ ] 12.1 Add a "Set up Firebase config" step to `.github/workflows/ci.yml` (after "Grant execute permission for gradlew") that decodes the `GOOGLE_SERVICES_JSON_BASE64` secret to `app/google-services.json`.
- [ ] 12.2 Add the same step to `.github/workflows/deploy.yml`, positioned consistently with the existing keystore/Maps-key/Play-Store-key decode steps.

## 13. Documentation updates (integration)

- [ ] 13.1 Add an **Autocapture** note to `docs/analytics/tracking-plan.md` recording that `screen_view` autocapture is disabled (replaced by the manual `SCREEN_VIEW` mapping) and that Firebase's other automatic events (session/engagement/lifecycle) are reviewed and knowingly left enabled with no manual toggle available, per the guidelines' "anything deliberately left on gets an entry here" rule.
- [ ] 13.2 Add a note to `docs/analytics/tracking-plan.md`'s parameter reference confirming `is_first_trip` is sent to Firebase as a String (`"true"`/`"false"`) at the provider level, while remaining a `Boolean` on the domain `AnalyticsEvent.TripCreated` type (no taxonomy change).
- [ ] 13.3 Update `CLAUDE.md` "Completed stories" list to include Epic 9 / Feature 9.2 story 9.2.1.

## 14. Compliance review (manual)

- [ ] 14.1 Review `docs/publishing/Privacy Policy.md` against what the Firebase Analytics SDK actually collects (device metadata, IP-derived country, US data processing); confirm existing SCC/international-transfer language covers it or update if a material gap exists.
- [ ] 14.2 Review the Play Data Safety form against what the SDK collects, including the `AD_ID` permission being present (per the kept-permission decision in task 11.2).

## 15. Final verification

- [ ] 15.1 Run `./gradlew detektDebug` (or `detektRelease`) and resolve every finding — zero tolerance.
- [ ] 15.2 Run `./gradlew test` and confirm all unit tests pass, including the new `FirebaseAnalyticsProviderTest`.
- [ ] 15.3 Run Kover and confirm coverage remains at or above the 95% bound.
- [ ] 15.4 Run `./gradlew assembleDebug assembleRelease` with a real `google-services.json` present and confirm both succeed.
- [ ] 15.5 Search the codebase for `import com.google.firebase` outside `core/analytics/` and `di/` and confirm zero matches.
- [ ] 15.6 On-device DebugView verification: with `adb shell setprop debug.firebase.analytics.app com.guidovezzoni.venice.debug`, exercise the full activation funnel and confirm every event arrives with the expected name/parameters, correct types, no `(other)` bucketing, both user properties attach to subsequent events, `screen_viewed` appears as `screen_view`, and no duplicate or unexpected automatic events appear.
- [ ] 15.7 Verify a release-signed build emits events to Firebase (first release build with a real analytics destination).
- [ ] 15.8 Register all 14 event parameters and both user properties as custom definitions in the Firebase console under *Analytics > Custom Definitions*.
- [ ] 15.9 Confirm CI (`ci.yml`) is green end-to-end with the `GOOGLE_SERVICES_JSON_BASE64` secret in place.
