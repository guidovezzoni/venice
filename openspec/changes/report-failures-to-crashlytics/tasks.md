## 1. Prerequisites: dependencies and DI plumbing

- [ ] 1.1 In `gradle/libs.versions.toml`, add a `firebaseCrashlyticsPlugin = "3.0.7"` version entry, a
      `firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }`
      library entry (under the existing `firebase-analytics` line, using the existing `firebase-bom`
      for version alignment), and a
      `firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsPlugin" }`
      plugin entry (under the existing `google-services` plugin line).
- [ ] 1.2 In root `build.gradle.kts`, add `alias(libs.plugins.firebase.crashlytics) apply false` to the
      `plugins {}` block, alongside the existing `alias(libs.plugins.google.services) apply false`.
- [ ] 1.3 In `app/build.gradle.kts`, add `alias(libs.plugins.firebase.crashlytics)` to the `plugins {}`
      block (real apply, alongside `alias(libs.plugins.google.services)`), and add
      `implementation(libs.firebase.crashlytics)` to the `dependencies {}` block next to the existing
      `implementation(libs.firebase.analytics)` line. Both plugins share the same
      `app/google-services.json` requirement already guarded by the existing `configureEach` block in
      this file, so no new guard is needed.
- [ ] 1.4 In `app/src/main/java/com/guidovezzoni/venice/di/FirebaseModule.kt`, add a
      `provideFirebaseCrashlytics(): FirebaseCrashlytics` function annotated `@Provides @Singleton`
      that returns `FirebaseCrashlytics.getInstance()` (no `Context` parameter needed, unlike
      `FirebaseAnalytics`), and import `com.google.firebase.crashlytics.FirebaseCrashlytics`.

## 2. trackException records the throwable with the operation key set first (BDD)

- [ ] 2.1 Write test: `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN trackException(RuntimeException("boom"), AnalyticsOperation.CREATE_TRIP) is called THEN setCustomKey("operation", "create_trip") is invoked before recordException is invoked` in `CrashlyticsAnalyticsProviderTest`. Also write the companion test:
      `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN trackException is called with a specific Throwable instance THEN FirebaseCrashlytics.recordException is invoked with that exact instance` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 2.2 Implement: create `app/src/main/java/com/guidovezzoni/venice/core/analytics/CrashlyticsAnalyticsProvider.kt`
      implementing `AnalyticsProvider`, constructor-injecting `FirebaseCrashlytics`. Implement
      `trackException(throwable, operation)` to call
      `firebaseCrashlytics.setCustomKey(KEY_OPERATION, operation.value)` then
      `firebaseCrashlytics.recordException(throwable)`, with `KEY_OPERATION = "operation"` extracted as
      a private companion-object constant. Add a KDoc/class comment documenting thread safety (no
      mutable instance state; safe to call from any thread or coroutine context).

## 3. logEvent sets error_type only for OperationFailed, and never records an exception (BDD)

- [ ] 3.1 Write test: `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent(AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK)) is called THEN setCustomKey("error_type", "network") is invoked` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 3.2 Implement: implement `logEvent(event)` with a `when (event)` branch matching only
      `AnalyticsEvent.OperationFailed`, calling
      `firebaseCrashlytics.setCustomKey(KEY_ERROR_TYPE, event.errorType.value)`, with
      `KEY_ERROR_TYPE = "error_type"` extracted as a private companion-object constant.
- [ ] 3.3 Write test: `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent(AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK)) is called THEN recordException is never invoked on the mocked FirebaseCrashlytics` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 3.4 Implement: verify (no production code change expected — this test guards the invariant that
      `logEvent` never calls `recordException`; if it fails, remove any accidental `recordException`
      call from the `OperationFailed` branch added in 3.2).
- [ ] 3.5 Write test: `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN logEvent is called with an AnalyticsEvent that is not OperationFailed (e.g. TripCreated) THEN no method is invoked on the mocked FirebaseCrashlytics` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 3.6 Implement: ensure the `when (event)` in `logEvent` has an `else -> Unit` (or equivalent)
      branch for every non-`OperationFailed` event, invoking nothing on `firebaseCrashlytics`.

## 4. User properties are forwarded as custom keys (BDD)

- [ ] 4.1 Write test: `GIVEN AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5) WHEN setUserProperty is called THEN FirebaseCrashlytics.setCustomKey is invoked with "trip_count_band" and "2_5"` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 4.2 Implement: implement `setUserProperty(property)` with an exhaustive `when (property)` over
      the sealed `AnalyticsUserProperty` interface; for `TripCountBand`, call
      `firebaseCrashlytics.setCustomKey(KEY_TRIP_COUNT_BAND, property.band.value)`, with
      `KEY_TRIP_COUNT_BAND = "trip_count_band"` extracted as a private companion-object constant.
- [ ] 4.3 Write test: `GIVEN AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL) WHEN setUserProperty is called THEN FirebaseCrashlytics.setCustomKey is invoked with "distance_unit" and "imperial"` in `CrashlyticsAnalyticsProviderTest`.
- [ ] 4.4 Implement: add the `DistanceUnit` branch to the same `when (property)`, calling
      `firebaseCrashlytics.setCustomKey(KEY_DISTANCE_UNIT, property.unit.value)`, with
      `KEY_DISTANCE_UNIT = "distance_unit"` extracted as a private companion-object constant.

## 5. Thread safety (BDD)

- [ ] 5.1 Write test: `GIVEN a CrashlyticsAnalyticsProvider with a mocked FirebaseCrashlytics WHEN trackException is invoked concurrently from multiple threads with distinct throwables and operations THEN every call reaches the mocked FirebaseCrashlytics exactly once, with no shared mutable state read or written by the provider` in `CrashlyticsAnalyticsProviderTest`, following the existing `FirebaseAnalyticsProviderTest` pattern (`Dispatchers.Default` + `runBlocking`/`launch`/`join`).
- [ ] 5.2 Implement: confirm (no production code change expected — `CrashlyticsAnalyticsProvider` holds
      no mutable instance state by construction; if the test fails, remove any instance-level `var` or
      shared mutable collection introduced in earlier steps).

## 6. Integration: DI wiring and manifest/proguard checks

- [ ] 6.1 In `app/src/main/java/com/guidovezzoni/venice/di/AnalyticsModule.kt`, add a
      `bindCrashlyticsAnalyticsProvider(implementation: CrashlyticsAnalyticsProvider): AnalyticsProvider`
      function annotated `@Binds @IntoSet`, alongside the existing `bindFirebaseAnalyticsProvider`
      function, and import `com.guidovezzoni.venice.core.analytics.CrashlyticsAnalyticsProvider`.
- [ ] 6.2 Verify `app/proguard-rules.pro` needs no changes: confirm
      `-keepattributes SourceFile,LineNumberTable` is present (Kotlin/JVM stack-trace deobfuscation) and
      note in the PR/commit description that native symbol upload is out of scope (no NDK code in this
      app).
- [ ] 6.3 Search the codebase for `import com.google.firebase.crashlytics` outside
      `core/analytics/` and `di/` and confirm zero matches (per the spec's containment requirement).

## 7. Final verification

- [ ] 7.1 Run `./gradlew assembleDebug` and `./gradlew assembleRelease` and confirm both build variants
      succeed with the Crashlytics plugin applied.
- [ ] 7.2 Run `./gradlew testDebugUnitTest` and confirm all tests pass, including the new
      `CrashlyticsAnalyticsProviderTest`.
- [ ] 7.3 Run `./gradlew detektDebug` and confirm zero findings.
- [ ] 7.4 Run `./gradlew koverVerify` (or the project's configured Kover verification task) and confirm
      `CrashlyticsAnalyticsProvider` is covered at 95%+.
- [ ] 7.5 Cross-check `docs/analytics/tracking-plan.md`: confirm no changes are needed — `trackException`
      is already documented there as a non-analytics channel with no event name or parameters, and this
      story adds no new event or user property.
