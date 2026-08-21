## Why

Analytics events currently reach only a Logcat sink in debug builds. Release builds have **zero**
registered providers, so every `logEvent`, `setUserProperty`, and `trackException` call is a silent
no-op in production. The 14-event taxonomy defined in `docs/analytics/tracking-plan.md` (delivered by
stories 9.1.1 and 9.1.2) has never reached a real backend. This change adds Firebase Analytics as the
first real destination, closing that gap without touching the taxonomy or the provider-agnostic
abstraction the app already has in `core/analytics/`.

## What Changes

- Add a new `FirebaseAnalyticsProvider` implementing the existing `AnalyticsProvider` interface.
  It owns every Firebase-specific concern — `Bundle` mapping, event-name translation, platform
  naming/size limits, and the `is_first_trip` Boolean-to-String conversion — so no other layer
  becomes aware Firebase exists.
- Register the provider via a single `@Binds @IntoSet` in `AnalyticsModule.kt`'s **main** source set
  (not `debug`). This is the first story that gives release builds an analytics destination.
- Map `screen_viewed` to Firebase's reserved `SCREEN_VIEW` event (`FirebaseAnalytics.Event.SCREEN_VIEW`
  / `FirebaseAnalytics.Param.SCREEN_NAME`) so it populates Firebase's built-in screen reports instead
  of appearing as an unrelated custom event.
- Enforce Firebase's platform limits inside the provider (event/parameter name length and character
  rules, reserved prefixes, string value length, max parameter count), failing loudly in debug builds
  rather than silently dropping data.
- `trackException` is a no-op in this provider — Firebase Analytics is a product-analytics
  destination, not a crash reporter; crash reporting is deferred to a future Crashlytics provider.
- Add `firebase-bom`, `firebase-analytics`, and the `google-services` Gradle plugin via the version
  catalog; apply the plugin in the app module; gitignore `google-services.json`.
- Make a missing `google-services.json` fail the build with a clear, actionable Gradle error (naming
  the missing file and where to obtain it) rather than a cryptic plugin stack trace, both locally and
  in CI.
- Disable Firebase's automatic `screen_view` autocapture via AndroidManifest meta-data, and document
  the review of every other Firebase automatically-collected event.
- **Explicitly keep** the `AD_ID` permission Firebase's manifest merge adds — a deliberate override of
  the source user story's AC 19, recorded here since it changes what the story asked for.
- Update `.github/workflows/ci.yml` and `.github/workflows/deploy.yml` directly to decode a new
  `GOOGLE_SERVICES_JSON` secret at build time, following the existing keystore/maps-key secret
  pattern in `deploy.yml`.
- Delete `docs/improvements/github-ci-setup.md` — stale (claims "no CI/CD" while `ci.yml` already
  exists); CI documentation now lives directly in the workflow files.
- Document the manual Firebase console prerequisites (registering both application IDs, registering
  all 14 event parameters and 2 user properties as custom definitions) as a prerequisite checklist,
  since these steps cannot be automated by an agent.

**BREAKING**: None. `AnalyticsClient`, `AnalyticsProvider`, and `AnalyticsTracking` interfaces are
unchanged; existing call sites are unaffected.

## Capabilities

### New Capabilities
- `analytics-firebase-provider`: Behavioural contract for `FirebaseAnalyticsProvider` — event and
  user-property mapping to the Firebase SDK, the `screen_viewed` → `SCREEN_VIEW` special case, the
  `trackException` no-op, platform-limit enforcement and its debug-mode failure behaviour, the
  Boolean-to-String conversion for `is_first_trip`, and autocapture being explicitly disabled.

### Modified Capabilities
- None. `analytics-abstraction` (the `AnalyticsProvider` contract, DI multibinding pattern, debug-only
  gating) and `analytics-taxonomy` (event/parameter definitions, including `is_first_trip` remaining a
  `Boolean` at the domain-event level) are consumed as-is, not changed. The Boolean-to-String
  conversion is a Firebase-provider-local mapping detail, not a taxonomy change.

## Impact

- **New code**: `app/src/main/java/.../core/analytics/FirebaseAnalyticsProvider.kt` and its unit test
  `app/src/test/java/.../core/analytics/FirebaseAnalyticsProviderTest.kt`.
- **Modified code**: `di/AnalyticsModule.kt` (new `@Binds @IntoSet` binding, main source set).
- **Build config**: `gradle/libs.versions.toml`, root `build.gradle.kts`, `app/build.gradle.kts`,
  `settings.gradle.kts` (verify `google()` filter resolves Firebase + `google-services` plugin
  artifacts).
- **Manifest**: `app/src/main/AndroidManifest.xml` (autocapture meta-data; `AD_ID` left untouched).
- **CI/CD**: `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`.
- **Removed**: `docs/improvements/github-ci-setup.md`.
- **Docs**: `docs/analytics/tracking-plan.md` (Firebase mapping notes), `CLAUDE.md` (completed-stories
  entry), `docs/publishing/Privacy Policy.md` (review only).
- **Dependencies**: `com.google.firebase:firebase-bom`, `com.google.firebase:firebase-analytics`,
  `com.google.gms:google-services` plugin — all new to the project.
- **External/manual**: Firebase console app registration (both application IDs) and custom-definition
  registration for all 14 event parameters and 2 user properties — cannot be automated, tracked as a
  prerequisite checklist in `design.md`.
