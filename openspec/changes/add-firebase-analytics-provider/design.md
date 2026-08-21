## Context

`core/analytics/` already provides a provider-agnostic abstraction (story 9.1.1) and a finished
14-event taxonomy (story 9.1.2, see `docs/analytics/tracking-plan.md`). The only registered provider
today is `DebugAnalyticsProvider`, bound **only** in the `debug` source set — release builds have an
empty `Set<AnalyticsProvider>`, so `CompositeAnalyticsClient` fans out to nobody and every analytics
call is a documented, intentional no-op.

This change adds the first provider bound in `main`: `FirebaseAnalyticsProvider`. Firebase Analytics
(GA4 for Firebase) is free with unlimited event volume, self-initialises via a `ContentProvider`, and
DebugView gives near-real-time on-device verification — which matters because a taxonomy is only as
good as what actually reaches the backend.

Constraints already fixed by prior stories and guidelines and not up for revisiting here:
- `AnalyticsProvider` / `AnalyticsTracking` interface shape (`logEvent`, `setUserProperty`,
  `trackException`) — see `analytics-abstraction` spec.
- The taxonomy itself — 14 events, 2 user properties, all enum `value: String` vocabularies — see
  `analytics-taxonomy` spec. **Not modified by this change.**
- No Firebase type may appear outside `core/analytics/` and `di/` (existing `core/` charter, reinforced
  by the story's AC 8 / NFR).

## Goals / Non-Goals

**Goals:**
- Deliver all 14 tracking-plan events and both user properties to Firebase Analytics, unmodified in
  name and semantics.
- Contain every Firebase-specific concern (`Bundle` mapping, platform limits, `SCREEN_VIEW` special
  case, autocapture control) inside `FirebaseAnalyticsProvider` and its DI binding.
- Make a missing `google-services.json` fail **loudly and specifically**, in both local dev and CI,
  rather than surfacing a generic Gradle/plugin stack trace or silently producing a broken build.
- Give release builds their first real analytics destination via a single `@Binds @IntoSet` addition,
  changing no other wiring.
- Get CI/CD building successfully with a real Firebase config supplied as a secret, and delete the
  stale CI-setup doc that no longer matches reality.

**Non-Goals:**
- Crash reporting (Crashlytics) — `trackException` stays a no-op in this provider; a future story adds
  a separate `CrashlyticsProvider`.
- Any change to the event/parameter taxonomy, `AnalyticsProvider` interface, or `CompositeAnalyticsClient`
  fan-out behaviour.
- A consent gate (story 9.3.1) — consent remains assumed-granted per the tracking plan's recorded
  posture; this change does not touch `setConsent`.
- Removing the `AD_ID` permission. The source story's AC 19 asks for this; **it is explicitly
  overridden here per user instruction** — the permission is left as Firebase's manifest merge adds
  it. Recorded as a deviation, not silently dropped.

## Decisions

### 1. Provider shape mirrors `DebugAnalyticsProvider`, with constructor-injected `FirebaseAnalytics`

```kotlin
class FirebaseAnalyticsProvider @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsProvider
```

`FirebaseAnalytics` is supplied via a `@Provides` function (e.g. in `AnalyticsModule.kt`) returning
`FirebaseAnalytics.getInstance(context)`, using the already-injected `@ApplicationContext Context`.
This — not a static `FirebaseAnalytics.getInstance()` call inside the provider — is what makes the
provider unit-testable with a MockK mock, per the story's testability NFR.

**Alternative considered**: call `Firebase.analytics` (KTX) directly inside the class. Rejected —
untestable without Robolectric/instrumentation, which the story explicitly wants to avoid for this
provider's unit tests.

### 2. Generic `Bundle` mapping from `AnalyticsEvent.properties`, with one special case

`logEvent` builds a `Bundle` by iterating `event.properties: Map<String, Any>` and dispatching on
runtime type:

| Kotlin type | Bundle call | Notes |
|---|---|---|
| `String` | `putString` | validated against the 100-char value limit |
| `Int`, `Long` | `putLong` | GA4 treats both as numeric; `Long` avoids overflow surprises |
| `Double` | `putDouble` | none currently in the taxonomy, included for completeness per the primitive-only rule in the analytics guidelines |
| `Boolean` | `putString` of `"true"`/`"false"` | **AC 17**: GA4 custom definitions only support String and Numeric types; a raw Boolean is unregistrable as a dimension. This is a **generic** rule (any Boolean parameter), not a `TripCreated`-specific special case, so a future Boolean parameter is handled correctly without touching this provider again. |

Any other runtime type is unreachable — `AnalyticsEvent` is a closed sealed class built from typed
constructors restricted to those five primitives by the taxonomy spec, so this is a defensive branch,
not a real code path. Hitting it is itself a platform-limit violation (see Decision 4) rather than a
silent drop.

**`screen_viewed` is a distinct branch**, checked before the generic path: when `event` is
`AnalyticsEvent.ScreenViewed`, the provider calls
`firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screen.value))`
instead of logging a custom `"screen_viewed"` event. This is what populates Firebase's built-in screen
reports (confirmed against current Firebase docs: `FirebaseAnalytics.Event.SCREEN_VIEW` /
`FirebaseAnalytics.Param.SCREEN_NAME` is the documented manual screen-view API). `SCREEN_CLASS` is not
set — the taxonomy has no equivalent concept and inventing one is out of scope.

**Alternative considered**: a `when (event)` exhaustive branch per event type instead of generic map
iteration. Rejected — 14 near-identical branches would duplicate what `properties` already encodes;
the generic mapping is what keeps the provider from having to change every time a taxonomy event's
parameter *set* changes (only new *types* would require a change, which is rare and already handled by
the five-branch table above).

### 3. Platform-limit enforcement: validate-then-log, loud failure gated by a testable flag

A private validation step runs before every `logEvent`/`setUserProperty` call, checking:
- Event/parameter name: ≤40 chars, alphabetic first character, `[A-Za-z0-9_]+`, no `firebase_` /
  `google_` / `ga_` prefix.
- String parameter value: ≤100 chars.
- Parameter count per event: ≤25.
- User property name: ≤24 chars (Android SDK limit — narrower than the parameter-name limit and than
  iOS's 36-char limit), same character/prefix rules.

On violation, the provider calls an internal `reportPlatformLimitViolation(message: String)` that:
- Throws `IllegalStateException(message)` when `isDebugBuild` is `true` (an injected `Boolean`,
  defaulting to `BuildConfig.DEBUG`, **not** read directly inside the function) — so debug builds fail
  fast and visibly during development or DebugView verification.
- Logs `Log.e("Analytics", message)` and returns without throwing when `isDebugBuild` is `false` — so a
  release build never crashes on a data-shape bug; the event is dropped (matching what Firebase itself
  would do) but the failure is not silent to anyone watching Logcat/Crashlytics-adjacent tooling.

Injecting `isDebugBuild` as a constructor parameter (rather than referencing `BuildConfig.DEBUG`
inline) is what makes both branches unit-testable without a debug/release split test source set.

**Alternative considered**: rely solely on Firebase's own server-side validation and error events
(`docs/analytics/errors`). Rejected — server-side drops are invisible until someone thinks to check the
`(other)` bucket or the console's error log days later; the story explicitly asks for a loud, local,
debug-time signal instead.

### 4. `trackException` is a genuine no-op, not a partial implementation

```kotlin
override fun trackException(throwable: Throwable, operation: AnalyticsOperation) {
    // Intentional no-op: Firebase Analytics is a product-analytics destination, not a crash
    // reporter. Crash reporting is delivered by a future Crashlytics provider (story 9.2.2).
}
```

No Firebase Crashlytics dependency is added by this change. This keeps the two channels described in
the analytics guidelines (`operation_failed` event vs. `trackException` diagnostic channel) honestly
separated: this provider only ever touches the first.

### 5. User-property mapping is an exhaustive `when` on the sealed interface

```kotlin
private fun AnalyticsUserProperty.toFirebaseNameValue(): Pair<String, String> = when (this) {
    is AnalyticsUserProperty.TripCountBand -> "trip_count_band" to band.value
    is AnalyticsUserProperty.DistanceUnit -> "distance_unit" to unit.value
}
```

`AnalyticsUserProperty` (unlike `AnalyticsEvent`) does not carry its own `name`/`value` fields today —
the provider owns this mapping locally, matching the tracking plan's parameter/property reference
table rather than duplicating name strings elsewhere. Being a `sealed interface`, the `when` is
exhaustive and adding a third user property without updating this function is a compile error.

### 6. Dependencies, plugin application, and the missing-config-file failure mode

- `gradle/libs.versions.toml`: add a `firebaseBom` version, `firebase-bom` / `firebase-analytics`
  libraries (analytics has no explicit version — managed by the BOM platform), and a
  `google-services` plugin entry (`com.google.gms:google-services`).
- Root `build.gradle.kts`: add `alias(libs.plugins.google.services) apply false`, consistent with
  every other plugin in that file.
- `app/build.gradle.kts`: apply the plugin, add `implementation(platform(libs.firebase.bom))` and
  `implementation(libs.firebase.analytics)`.
- **Missing-file failure mode**: rather than relying on the `google-services` plugin's own error
  (serviceable but generic — it does not mention this project's Firebase console URL or which secret
  CI needs), add a `doFirst` guard (or an early `check()` in a small Gradle script block) in
  `app/build.gradle.kts` that verifies `google-services.json` exists before the plugin task runs, and
  throws a `GradleException` naming the exact expected path and pointing at the Firebase console /
  the `GOOGLE_SERVICES_JSON` CI secret. This satisfies **both** the source story's AC 3 ("not a
  confusing plugin error") and the user's clarification #2 ("must fail with a clear, actionable
  message") — they are the same requirement read two ways: the failure must happen, and it must be
  legible. No dummy/placeholder `google-services.json` is generated — a build that silently "succeeds"
  against a fake config is worse than one that fails clearly, because it would ship a build that
  can never actually reach Firebase.
- `settings.gradle.kts`: the `google()` repository content filter already matches
  `includeGroupByRegex("com\\.google.*")`, which covers both `com.google.firebase` (SDK artifacts) and
  `com.google.gms` (the plugin, resolved via `pluginManagement`'s own `google()` block, same filter).
  Confirmed by resolving dependencies against the real filter in `openspec/changes/.../tasks.md`
  rather than assumed; no change to the filter is expected.
- `.gitignore`: add `google-services.json`, positioned next to `local.properties` and
  `keystore.properties` for discoverability.

### 7. CI/CD: decode the config file as a new secret, delete the stale doc

`.github/workflows/deploy.yml` already decodes three secrets (keystore, Maps key, Play Store key)
using the exact pattern needed here. `ci.yml` currently builds without any local secrets at all
(`MAPS_API_KEY` simply falls back to `""`), which will no longer be sufficient once the
`google-services` plugin requires its config file for **every** build variant, including `check` and
`assembleDebug`/`assembleRelease` in CI.

Both workflows get a new step, immediately after "Grant execute permission for gradlew":
```yaml
- name: Set up Firebase config
  run: echo "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" | base64 --decode > app/google-services.json
```
`docs/improvements/github-ci-setup.md` is deleted outright (per clarification #3) rather than updated
— it already claims "no CI/CD" while `ci.yml` has existed for a while, so it was stale before this
change and updating it further would only extend a doc that should not exist. Anything future
maintainers need about the CI secret set now lives directly in the workflow YAML, which cannot drift
from what actually runs.

**Alternative considered**: keep `github-ci-setup.md` and add a row to its secrets table. Rejected per
explicit user instruction — the whole document's premise (no CI/CD exists yet) is false, and patching
one row would leave the rest of the false premise standing.

### 8. Autocapture: disable `screen_view`, review and keep the rest, record the decision

Confirmed against current Firebase docs: the manifest meta-data key is
`google_analytics_automatic_screen_reporting_enabled`, set to `false` inside `<application>`. This is
the only automatically-collected event the taxonomy has a manual replacement for (`screen_viewed` /
`SCREEN_VIEW`, Decision 2), so it is the only one disabled — leaving both active would double-count
every screen view once story 9.1.3 wires the manual trigger from navigation.

Every other Firebase-automatic event (`first_open`, `session_start`, `user_engagement`, `app_update`,
`app_remove`, `os_update`, `in_app_purchase`, etc.) has no manual equivalent in the 14-event taxonomy
and cannot be individually toggled — Firebase only exposes the one screen-view kill switch. Per the
analytics guidelines ("anything deliberately left on gets an entry here like any other event"),
`docs/analytics/tracking-plan.md` gets a short **Autocapture** note recording this as a reviewed,
intentional decision rather than an unreviewed default, so the "every event has a plan entry" rule
still holds for the events the app cannot switch off.

### 9. `AD_ID` permission: kept, deviating from the source story

The source story's AC 19 asks to strip `AD_ID` via `tools:node="remove"`. Per explicit user
instruction for this change, **that acceptance criterion is not implemented** — the permission is left
exactly as Firebase's manifest merge adds it. No manifest edit is made for `AD_ID` at all. This
decision is intentionally visible in `proposal.md`, here, and in `tasks.md`'s AC-disposition notes so
a future reader does not mistake the omission for an oversight when re-reading the original story.

## Risks / Trade-offs

- **[Risk] The Gradle guard for a missing `google-services.json` could itself be brittle** (e.g. only
  checked for some tasks, not others) → **Mitigation**: hook the check into the same task graph point
  the `google-services` plugin itself uses (`preBuild`/`doFirst` on the app module's build script,
  evaluated before variant configuration), and cover it with the task list's manual verification step
  (build once with the file removed, confirm the message, restore it) rather than relying on inference.
- **[Risk] CI has no Firebase config secret yet** → **Mitigation**: `tasks.md` includes creating the
  `GOOGLE_SERVICES_JSON_BASE64` secret (or equivalent name) as an explicit, clearly-flagged manual
  step the user performs in GitHub repo settings; the change cannot make CI green by itself until that
  secret exists, same as the existing keystore/Maps-key secrets already required manual setup.
- **[Risk] Firebase console setup (both application IDs, all custom definitions) is also manual** →
  **Mitigation**: documented as a prerequisite checklist in `tasks.md`, sequenced before any on-device
  DebugView verification task, so the dependency is explicit rather than discovered mid-verification.
- **[Risk] Deviating from the source story's AC 19 (AD_ID removal) could be missed by a future
  verification pass that reads only the original story** → **Mitigation**: the deviation is recorded
  in three places (proposal, design, tasks) rather than only here.
- **[Trade-off] Generic `Bundle` mapping over per-event branches** trades a small amount of type safety
  (the compiler cannot catch a parameter of an unsupported type at the call site) for not having to
  touch the provider every time an event gains or loses a parameter. The validation layer (Decision 3)
  is what recovers the safety generic mapping gives up — an unsupported type or bad value fails loudly
  in debug rather than compiling silently wrong.
- **[Trade-off] No consent gate** — accepted as already decided by the tracking plan's Consent Posture
  section; this change does not re-litigate it, only inherits the existing recorded risk.

## Migration Plan

1. Firebase console: register both application IDs, download `google-services.json` for local dev use
   only (never committed).
2. Land dependency/plugin/gitignore changes; confirm local builds succeed with the file present and
   fail with the new clear message when absent.
3. Implement and unit-test `FirebaseAnalyticsProvider`; wire the `@Binds @IntoSet` binding.
4. Manifest: disable automatic screen reporting.
5. Add the `GOOGLE_SERVICES_JSON_BASE64` GitHub secret; update `ci.yml` and `deploy.yml`; delete
   `docs/improvements/github-ci-setup.md`.
6. Register all custom definitions (14 parameters + 2 user properties) in the Firebase console.
7. On-device DebugView verification of the full funnel; release-build verification.
8. Documentation: tracking-plan autocapture note, `CLAUDE.md` completed-stories entry, Privacy
   Policy / Play Data Safety review.

**Rollback**: revert the `@Binds @IntoSet` line (one-line change) to instantly return release builds to
the pre-change no-provider state without touching any other code — the abstraction was designed for
exactly this kind of safe addition/removal. No data migration exists to roll back; Firebase-side data
already collected is unaffected either way.

## Open Questions

None outstanding — all ambiguities in the source story were resolved by the four clarifications listed
in `proposal.md`'s "Why"/"What Changes" context before this design was written.
