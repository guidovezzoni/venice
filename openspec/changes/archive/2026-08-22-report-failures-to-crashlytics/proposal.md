## Why

`trackException` exists on `AnalyticsTracking` and is already called from every failure path via
`trackFailure`, but no provider implements it — `FirebaseAnalyticsProvider.trackException` is an
explicit no-op. Handled failures and uncaught crashes reach nobody. Story 9.2.2 closes that gap by
adding a Crashlytics-backed provider so real-world failures are visible without waiting for a user
report.

## What Changes

- Add `CrashlyticsAnalyticsProvider`, a new `AnalyticsProvider` implementation backed by
  `FirebaseCrashlytics`, registered independently of `FirebaseAnalyticsProvider` in the multibound
  `Set<AnalyticsProvider>`.
- `trackException(throwable, operation)` sets an `operation` custom key, then calls
  `FirebaseCrashlytics.recordException(throwable)` with the real throwable — the first provider to
  give this channel a real implementation.
- `logEvent` subscribes only to `AnalyticsEvent.OperationFailed`, setting an `error_type` custom key.
  It never calls `recordException` — non-fatals are never synthesised from an event, only ever
  recorded from `trackException`.
- `setUserProperty` forwards both existing user properties (`TripCountBand`, `DistanceUnit`) as
  Crashlytics custom keys, so a crash report carries the same segmentation context Firebase Analytics
  has.
- Add the Crashlytics Gradle plugin and `firebase-crashlytics` dependency via the version catalog, and
  a `FirebaseCrashlytics` `@Provides` in `FirebaseModule.kt`.
- `setCrashlyticsCollectionEnabled` is explicitly out of scope — deferred to the consent story 9.3.1.
  Native symbol upload is an accepted limitation: the app is pure Kotlin/JVM with no NDK, so there is
  nothing to upload.

## Capabilities

### New Capabilities
- `analytics-crashlytics-provider`: The `CrashlyticsAnalyticsProvider` — constructor-injection
  contract, custom-key mapping for exceptions/events/user properties, the trackException/logEvent
  channel separation, thread safety, and DI registration.

### Modified Capabilities
(none — `analytics-firebase-provider`'s `trackException`-is-a-no-op requirement is unaffected; this
change adds a second, independent provider rather than changing Firebase Analytics behaviour)

## Impact

- **New**: `core/analytics/CrashlyticsAnalyticsProvider.kt`,
  `test/core/analytics/CrashlyticsAnalyticsProviderTest.kt`
- **Modified**: `di/AnalyticsModule.kt` (new `@Binds @IntoSet`), `di/FirebaseModule.kt` (new
  `@Provides @Singleton` for `FirebaseCrashlytics`), `gradle/libs.versions.toml` (new library + plugin
  entries), root `build.gradle.kts` (plugin `apply false`), `app/build.gradle.kts` (plugin applied +
  dependency added)
- **Dependencies**: Firebase Crashlytics Gradle plugin 3.0.7, `firebase-crashlytics` KTX library (BOM
  version, already pinned at 33.7.0)
- **No CI/CD changes**: `.github/workflows/ci.yml` already decodes `google-services.json`
- **No tracking-plan changes**: `trackException` is documented as a non-analytics channel with no
  event name or parameters
