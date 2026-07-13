## Context

Venice's trip detail screen (`TripDetailScreen.kt`, `StopSection.kt`, `TripDetailViewModel.kt`, `MainScreen.kt`) already follows a strict MVI contract: `TripDetailUiIntent` → ViewModel → `TripDetailUiState` / `TripDetailUiEffect`. `MainScreen.kt` is the only place with an Android `Context` in scope and currently collects `TripDetailUiEffect` for a single case (`ShowError` → snackbar).

This is the first feature in the codebase that launches an external app via an implicit `Intent`. Android's package visibility rules (introduced API 30, enforced at `targetSdk 36`) mean `PackageManager.resolveActivity()` returns `null` for intents the manifest hasn't declared in a `<queries>` block — even when a capable app (Google Maps, Waze) is installed. This is easy to miss in emulator testing if the emulator doesn't restrict visibility, so it must be an explicit, verified task rather than an assumption.

Stops already carry persisted `latitude`/`longitude`/`placeName` (`Stop` domain model), so no new resolution/geocoding step is needed — the geo URI is built directly from data already in memory.

## Goals / Non-Goals

**Goals:**
- Let the user launch any installed navigation app, pre-filled with a stop's coordinates and label, via the standard `geo:` URI scheme and system app picker.
- Keep URI construction pure and unit-testable, independent of Android `Context`.
- Keep the composable layer (`StopSection`, `TripDetailScreen`) presentational — visibility and enablement are derived from state/props the ViewModel/caller already computes, not decided inside the composable.
- Guarantee correct behaviour on API 30+ via an explicit manifest `<queries>` declaration.

**Non-Goals:**
- Building any custom map/routing UI inside Venice.
- Supporting turn-by-turn directions between two stops (this is single-destination navigation only, per acceptance criteria).
- Handling app-specific deep links (e.g. Waze's `waze://` scheme) — the standard `geo:` scheme is sufficient and is what the acceptance criteria specify.
- Requesting any runtime permissions (none are required for `ACTION_VIEW` + `geo:`).

## Decisions

### 1. URI construction lives in a standalone `NavigationUriBuilder` utility
Following the existing `CoordinateFormatter`/`DistanceFormatter`/`DurationFormatter` pattern in `ui/util/`, geo URI formatting is extracted into `ui/util/NavigationUriBuilder.kt` with a single pure function, e.g. `fun buildGeoUri(latitude: Double, longitude: Double, placeName: String): Uri` (or returning a `String` passed to `Uri.parse` at the call site — implementation detail for the task, but the function itself must not touch `Context` or Android framework intent APIs beyond `Uri`).
- **Why**: keeps the formatting logic unit-testable without instrumentation, matches the project's established utility pattern, and keeps `MainScreen.kt` thin.
- **Alternative considered**: building the URI inline inside `MainScreen.kt`. Rejected — untestable without a Compose/instrumented test, and inconsistent with existing formatter conventions.
- **URI format** (fixed by acceptance criteria): `geo:{lat},{lon}?q={lat},{lon}({placeName})`. The `placeName` must be percent-encoded via `Uri.encode` (or equivalent) since it may contain spaces or parentheses-breaking characters.

### 2. ViewModel resolves the stop and emits a data-only effect; MainScreen owns all Android intent APIs
`TripDetailViewModel` looks up the `Stop` by `stopId` from current state, and emits `TripDetailUiEffect.LaunchNavigation(latitude, longitude, placeName)` — no `Uri`, `Intent`, or `Context` types cross the ViewModel boundary. `MainScreen.kt`'s effect collector calls `NavigationUriBuilder` to build the URI, constructs the `Intent(Intent.ACTION_VIEW, uri)`, resolves it, and starts the activity or falls through to the error path.
- **Why**: preserves Clean Architecture / MVI boundaries — the UI layer (Composables/Activity-scoped code) owns platform API calls; the ViewModel stays platform-agnostic and testable with plain JUnit + MockK.
- **Alternative considered**: passing a pre-built `Intent` in the effect. Rejected — `Intent` is an Android framework type; keeping it out of the ViewModel/effect keeps effect classes trivially testable and consistent with existing effects (e.g. no existing effect carries Android framework types).

### 3. Error message is pre-localized by the ViewModel, matching the `formatDistance` precedent
`TripDetailUiEffect.ShowNavigationError(message: String)` carries an already-resolved, localized string. The ViewModel obtains it via the injected `Application`/`Resources` (already available for other resource-driven formatting in this codebase), rather than passing a string resource ID for the Composable to resolve.
- **Why**: consistent with the existing pattern where the ViewModel computes display-ready values (per `guidelines-android.md`, composables must not contain formatting/locale logic); also avoids adding a new "string-resource-in-effect" convention for just one case.
- **Alternative considered**: passing a `@StringRes Int` in the effect and resolving it in `MainScreen.kt` via `stringResource()`. Rejected per explicit user clarification — keep the pattern identical to `formatDistance`.

### 4. Visibility/enablement computed by the caller (`TripDetailScreen`), not `StopSection`
`FilledStop` gains a new optional trailing lambda parameter `onNavigate: (() -> Unit)? = null`, following the exact existing convention used by `onDelete`, `onMoveUp`, etc. (`null` hides the button, non-null shows it). `TripDetailScreen.kt` computes whether to pass a non-null lambda based on `stop.status == StopStatus.PENDING`, at all three `StopSection` call sites (starting point, intermediate stops, destination). The button's `enabled` state is driven by the same `isLoading` flag already wired to the other stop actions.
- **Why**: matches the existing "null hides the affordance" pattern exactly — no new conditional-rendering convention introduced. Keeps `StopSection` composable ignorant of `StopStatus` business rules.

### 5. Manifest `<queries>` block for the `geo:` scheme
Add to `AndroidManifest.xml`:
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="geo" />
    </intent>
</queries>
```
- **Why**: mandatory for `PackageManager.resolveActivity()`/`queryIntentActivities()` to return results on API 30+ (`targetSdk 36`). Without this, the app cannot see Maps/Waze regardless of what's installed, and every navigation attempt silently fails the "no app available" path even when it shouldn't.
- **Risk called out explicitly** in the proposal and tracked as its own verification task — this is the single highest-risk item in this change since it's easy to implement the rest of the feature correctly and still have it fail entirely on real devices due to a missing manifest entry.

## Risks / Trade-offs

- **[Risk]** Missing or malformed `<queries>` block silently breaks navigation on API 30+ while appearing to work in code review and even in some emulator configurations. → **Mitigation**: dedicated manifest task in tasks.md, plus on-device verification step (per `guidelines-process.md` On-Device Testing) explicitly exercising the Navigate button on a real/emulated API 30+ device to confirm the system picker (or single app) actually launches.
- **[Risk]** `placeName` may contain characters that break the query string (parentheses, `&`, spaces) if not encoded. → **Mitigation**: `NavigationUriBuilder` uses `Uri.encode`/`Uri.Builder` rather than raw string concatenation, covered by a unit test with a place name containing spaces and special characters.
- **[Risk]** Race between rapid taps on Navigate before `isLoading` flips true could double-launch the intent/activity. → **Mitigation**: reuse the existing `isLoading`-gated `enabled` pattern already applied to other stop actions; no new mechanism needed since this mirrors "Mark as departed" behaviour today.
- **[Trade-off]** Using the generic `geo:` scheme instead of app-specific deep links (e.g. Google Maps' `google.navigation:q=`) means Venice cannot force turn-by-turn navigation mode in the target app — it can only pass a destination. Accepted per acceptance criteria (#3, #4), since forcing a single app's deep-link format would break the "let the user pick" requirement.

## Migration Plan

No data migration required — this change is purely additive (new UI affordance, new MVI intent/effects, new utility, manifest addition, new strings). No existing persisted data, DTOs, or schemas change. Rollback is a simple revert of the change's commits; no feature flag is needed given the low blast radius (a single new button, hidden by default logic for `VISITED` stops).

## Open Questions

None — all ambiguities (icon choice, error message delivery mechanism, URI builder extraction) were resolved with the user prior to this design and are captured in the proposal/decisions above.
