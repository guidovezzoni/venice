## Context

`AnalyticsTracking` already declares `trackException(throwable, operation)` and every failure path
already calls it via the default `trackFailure` method (which also calls `logEvent(OperationFailed)`).
No provider currently does anything with it: `FirebaseAnalyticsProvider.trackException` is an explicit
no-op with a comment pointing at this story. `FirebaseAnalyticsProvider` and `DebugAnalyticsProvider`
are the only two bound providers today; both are registered via `@Binds @IntoSet` into
`Set<AnalyticsProvider>`, fanned out by `CompositeAnalyticsClient`.

The analytics guidelines are explicit that exceptions are a separate channel from events specifically
*because* crash reporting needs a real throwable and stack trace, which the bounded-enum event
taxonomy cannot carry. This story is the first to give that channel a real backend.

## Goals / Non-Goals

**Goals:**
- Give `trackException` a working implementation: real throwables reach Crashlytics with contextual
  custom keys.
- Keep `OperationFailed` events and `trackException` calls strictly separate — a Crashlytics non-fatal
  must only ever originate from a `trackException` call, never be synthesised inside `logEvent`.
- Forward the two existing user properties as Crashlytics custom keys so a crash report carries the
  same segmentation context as Firebase Analytics.
- Keep the provider stateless and thread-safe, matching the existing `FirebaseAnalyticsProvider`
  pattern.
- Contain all Crashlytics types inside `core/analytics/` and `di/`, per the `core/` charter.

**Non-Goals:**
- Consent gating (`setCrashlyticsCollectionEnabled`) — reserved for 9.3.1, when a consent surface
  exists.
- Native symbol / ProGuard mapping upload — the app is pure Kotlin/JVM with no NDK, so there is no
  native symbol table to upload. `-keepattributes SourceFile,LineNumberTable` (already present in
  `proguard-rules.pro`) is sufficient for Kotlin stack-trace deobfuscation.
- Changing `FirebaseAnalyticsProvider` in any way — its `trackException` no-op is correct and stays
  as-is; Crashlytics is a second, independent provider, not a replacement.
- Any change to the event taxonomy or tracking plan — `operation_failed` and `trackException` are
  already documented as separate channels.

## Decisions

### A new, independent provider — not a change to `FirebaseAnalyticsProvider`

Crashlytics and Firebase Analytics are different SDKs with different failure semantics (crash
reporting vs. product analytics) and different lifecycles (Crashlytics collection will eventually be
gated by its own consent decision, independent of analytics consent). Keeping them as two providers in
the multibound set, each owning its own backend's quirks, matches the existing architecture principle
("Each provider owns its own backend's quirks... no other layer knows which backends exist") and keeps
9.3.1's future consent work scoped to one provider at a time.

**Alternative considered**: extend `FirebaseAnalyticsProvider` to also call Crashlytics. Rejected —
it would couple two independently-versioned SDKs in one class and complicate the future consent split.

### `logEvent` never calls `recordException`

The guidelines are explicit that a non-fatal must carry a real throwable to be useful, and
`OperationFailed` carries only two bounded enum values by design (that boundedness is what keeps the
event taxonomy privacy-safe). Synthesising a `recordException` call from an event would mean either
fabricating a throwable (useless stack trace) or skipping `recordException` and only setting a key —
which is exactly what this design already does. `logEvent(OperationFailed)` therefore only ever sets
the `error_type` custom key; it never calls `recordException`, and there is a specced boundary
scenario asserting this to prevent silent regression.

### Custom keys are all `String`, set via `setCustomKey(String, String)`

`FirebaseCrashlytics.setCustomKey` is overloaded per value type. All four values this provider ever
sets (`operation`, `error_type`, `trip_count_band`, `distance_unit`) are already `.value: String` on
their respective enums, so using the `String` overload uniformly avoids type-branching that the
existing `FirebaseAnalyticsProvider` needs for its `Bundle` mapping (that branching exists there
because event parameters are genuinely mixed-type; Crashlytics custom keys in this provider are not).

### `operation` custom key is set before `recordException`

Crashlytics custom keys are visible on whichever crash report happens to follow their being set — a
key set after `recordException` would not be guaranteed to be attached to that report. Setting
`operation` first, then calling `recordException`, guarantees the operation context appears alongside
the throwable it describes. This ordering is specced as a scenario.

### No extra synchronization

`FirebaseCrashlytics`'s own API is documented as safe to call from any thread. `CrashlyticsAnalyticsProvider`
holds no mutable instance state (same pattern as `FirebaseAnalyticsProvider`), so no synchronization
is added — thread safety follows directly from statelessness plus the underlying SDK's own guarantee.

## Risks / Trade-offs

- **[Risk]** A future change could accidentally call `recordException` from inside `logEvent`,
  silently turning every `operation_failed` event into a non-fatal flood. → **Mitigation**: a specced
  scenario asserts `logEvent(OperationFailed(...))` never invokes `recordException`, backed by a unit
  test that fails if that invariant breaks.
- **[Risk]** Adding a second Firebase Gradle plugin (`com.google.firebase.crashlytics`) alongside
  `google-services` could break the build if applied in the wrong order or without `google-services.json`
  present. → **Mitigation**: follow the exact `apply false` (root) / real-apply (`app/build.gradle.kts`)
  pattern already proven for `google.services`, and verify both debug and release builds succeed as a
  task in `tasks.md`.
- **[Risk]** No native symbol upload is configured. → **Mitigation**: explicitly accepted — the app has
  no NDK code, so there is nothing to upload; documented here so it is a recorded decision rather than
  an oversight.

## Migration Plan

No data migration. Deployment is additive: a new provider joins the existing multibound set, so
existing behaviour (Firebase Analytics events, debug logging) is unaffected. Rollback is a revert of
the DI binding and Gradle plugin application; no persisted state to unwind.

## Open Questions

None — all decisions were resolved during story refinement (plugin version 3.0.7, custom-key typing,
consent deferral, native-symbol-upload scope).
