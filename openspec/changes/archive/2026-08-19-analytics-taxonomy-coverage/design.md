## Context

`core/analytics/` (from story 9.1.1) already has the structural machinery — `AnalyticsTracking`,
`AnalyticsClient`, `AnalyticsProvider`, `CompositeAnalyticsClient`, `DebugAnalyticsProvider`,
`AnalyticsLogFormatter` — but the taxonomy it carries (`AnalyticsEvent`'s 10 subtypes,
`AnalyticsOperation`'s 7 values, `AnalyticsUserProperty`'s empty `ForTesting` placeholder) is the
pre-9.1.1 legacy shape: raw identifiers, `error.message` forwarded verbatim, `stopType.name` leaking
enum casing. `docs/analytics/tracking-plan.md` is the agreed target: 14 events, 14 parameters, 2 user
properties, all built from bounded aggregates. This design covers how the code gets from the current
shape to that target, and the handful of call sites and domain return types that need to change to
source the new event data.

Two ViewModels own every tracking call: `TripListViewModel` (2 call sites: init, `createTrip`) and
`TripDetailViewModel` (11 existing call sites across `setStop`/`editStop`/`removeStop`/`moveStop`/
`markStopDeparted`/`calculateRoute`/init, plus 5 currently-silent user actions). No composable, use
case, or repository calls `AnalyticsClient` today, and that does not change here.

## Goals / Non-Goals

**Goals:**
- Replace all 10 legacy `AnalyticsEvent` subtypes with the 14 tracking-plan events, exact names and
  parameters.
- Close the 5 coverage gaps (`stop_departure_undone`, `place_search_performed`,
  `place_suggestion_selected`, `navigation_launched`, and the two new `search_place`/`resolve_place`
  operation values for failure paths at lines 287/323).
- Make every failure path dual-channel: `logEvent(OperationFailed(...))` alongside
  `trackException(throwable, operation)`, with no exception.
- Keep every enumerated parameter value flowing through a typed enum's `.value`, never `.name`.
- Set both user properties (`trip_count_band`, `distance_unit`) at the moments the tracking plan
  specifies.
- Make the minimum domain-layer changes needed to source event data without re-deriving it from
  possibly-stale UI state.

**Non-Goals:**
- Wiring a real backend provider (Firebase, Amplitude, …) — still just the debug Logcat sink.
- Consent gating — deferred to story 9.3.1 per the tracking plan's recorded posture.
- `shouldLog`-style per-provider filtering — noted as absent in 9.1.1, out of scope here.
- Finer-grained error classification (parsing HTTP status codes out of `DirectionsApiService`'s
  `IllegalStateException`, or Google Places `ApiException` status codes) — deferred per user
  clarification #2; classification is by exception type only in this change.
- Making `route_state` reach `stale` — `InvalidateRouteUseCase` deletes legs rather than marking them
  stale, so only `none`/`complete` are reachable. Documented as a known gap, not fixed here.

## Decisions

### 1. Enum inventory: 6 new files, `AnalyticsOperation` extended, no `Direction`/`RouteState` enum

New top-level types, one per file in `core/analytics/`, each carrying an explicit `value: String` in
`snake_case`:

| File | Kind | Values |
|------|------|--------|
| `DistanceBand.kt` | banding function | `under_50km`, `50_200km`, `200_500km`, `500_1000km`, `over_1000km` |
| `DurationBand.kt` | banding function | `under_1h`, `1_3h`, `3_6h`, `6_12h`, `over_12h` |
| `CountBand.kt` | banding function | `0`, `1`, `2_5`, `6_plus` |
| `AnalyticsScreen.kt` | flat vocabulary | `trip_list`, `trip_detail` |
| `StopTypeParam.kt` | flat vocabulary | `starting_point`, `destination`, `intermediate` |
| `AnalyticsErrorType.kt` | flat vocabulary | `network`, `timeout`, `not_found`, `permission_denied`, `quota_exceeded`, `persistence`, `unknown` |

Plus a top-level `AnalyticsErrorClassifier.kt` function (not an enum) classifying `Throwable ->
AnalyticsErrorType`, and `AnalyticsOperation.kt` extended from 7 to 10 values (adding
`UNDO_MARK_DEPARTED`, `SEARCH_PLACE`, `RESOLVE_PLACE`).

`direction` (`up`/`down`) and `route_state` (`none`/`complete`/`stale`) stay as plain `String`
constants built inline at the call site, **not** as dedicated enum types. Rationale: the tracking
plan's Parameter Reference table marks `stop_type`, `screen_name`, `error_type`, and `operation` with
"From `X.value` — never enum `.name`", but `direction` and `route_state` carry no such annotation —
they are two-and-three-value inline vocabularies with no classification logic behind them, unlike the
banding functions. Introducing enums for them would add two files that guard nothing a `when`
expression at a single call site doesn't already guard, and the story's own file inventory (6 new
files) does not include them.

**Boundary tests required** (per `guidelines-analytics.md`'s "behaviour not label list" rule) for
`DistanceBand`, `DurationBand`, `CountBand`, and `AnalyticsErrorClassifier` — each is a
mapping/classifying function. `AnalyticsScreen`, `StopTypeParam`, `AnalyticsErrorType` are flat
vocabularies and need no such tests.

`DistanceBand`/`DurationBand`/`CountBand` are top-level types (not nested inside `AnalyticsEvent`)
because `CountBand` is shared between `RouteCalculated`'s... no — `CountBand` is used only by
`TripCountBand`. `DistanceBand`/`DurationBand` are used only by `RouteCalculated`. None of the three
is actually shared between an event parameter and a user property in this taxonomy (`is_first_trip` is
a `Boolean`, not `CountBand`), but they are kept top-level regardless, consistent with every other
enum in `core/analytics/` and to leave room for a future event that legitimately shares a band.

### 2. Error classification: exception type only, per clarification #2

`AnalyticsErrorClassifier(throwable: Throwable): AnalyticsErrorType` inspects only the throwable's
runtime type:

```
SocketTimeoutException           -> TIMEOUT
other IOException (incl. subtypes) -> NETWORK
SQLiteException                  -> PERSISTENCE
everything else                  -> UNKNOWN
```

`SocketTimeoutException` is checked before the general `IOException` branch since it is itself an
`IOException` subtype. `not_found`, `permission_denied`, and `quota_exceeded` are declared in
`AnalyticsErrorType` per the tracking plan's parameter reference (so the enum is forward-compatible)
but are **unreachable from this classifier** — reaching them needs HTTP-status or `ApiException`-code
parsing, explicitly deferred. This is documented with a code comment on the classifier per the
project's data-modelling discipline ("note the limitation so future maintainers know to revisit").

Test fixtures: `SocketTimeoutException()`, a plain `IOException()` and a named subtype (e.g.
`java.net.UnknownHostException`) for the network branch, `android.database.sqlite.SQLiteException()`
for persistence, and a generic `IllegalStateException()` (what `DirectionsApiService` and the use
cases' precondition checks actually throw today) for the `unknown` fallback — this directly covers the
`CalculateRouteUseCase`/`MarkStopDepartedUseCase` failure paths, which currently throw
`IllegalStateException`.

### 3. Domain return-type changes: minimal, no repository interface change for stops

**`RouteRepository.calculateRoute`** changes from `Result<Unit>` to `Result<List<Leg>>`.
`RouteRepositoryImpl` already builds the `legs` list in memory before persisting it (see current
implementation: `directionsApiService.fetchRoute` → map to `legs` → `legDao.insertAll`) — returning
that same list costs nothing extra and avoids a second DB read or a `Flow.first()` race against Room's
invalidation tracker. `CalculateRouteUseCase` forwards the repository's `Result<List<Leg>>` unchanged.
This is a repository-interface change, but it stays inside `domain/repository/` (the interface already
lives in the domain layer) and only exposes data the implementation already has in hand — no
analytics awareness leaks into `data/` or `domain/`.

**`MarkStopDepartedUseCase`** changes from `Result<Unit>` to `Result<Stop>`. No `StopRepository`
interface change is needed: the use case already loads `currentStop` (the stop being marked departed)
before calling `updateStopStatus`. On success it returns `currentStop.copy(status =
StopStatus.VISITED)` — constructed locally, not re-fetched. `stop_position` in the `StopDeparted` event
is then `Stop.order` from this returned value, not derived from possibly-stale `uiState`.

**`UndoMarkStopDepartedUseCase`** changes from `Result<Unit>` to `Result<Stop>` by the same pattern:
it already loads `lastDeparted` before calling `updateStopStatus`; on success it returns
`lastDeparted.copy(status = StopStatus.PENDING)`. This sources `stop_position` for
`StopDepartureUndone`.

**`stop_count` is never sourced from a domain return type.** For `StopAdded`, `StopRemoved`,
`StopReordered`, and `StopDeparted`, the ViewModel computes it from the stop lists it already holds in
`uiState` (`startingPoint` + `intermediateStops` + `destination`) at the moment the success callback
runs — before `observeStopsUseCase`'s `Flow` has necessarily re-emitted the post-mutation list.
Concretely: a private `TripDetailViewModel` helper `currentStopCount(): Int` reads the pre-mutation
`uiState` snapshot; `setStop`'s success handler logs `stopCount = currentStopCount() + 1` (the added
stop is not yet in `uiState`), `removeStop`'s logs `currentStopCount() - 1` (the removed stop is still
in `uiState`) computed before mutation, `moveStop`/`markStopDeparted` log `currentStopCount()`
unchanged. This avoids adding a return-type change to `SetStopUseCase`/`RemoveStopUseCase`/
`MoveStopUseCase` — none of which the story's clarifications named — and avoids a race against the
async `Flow`, since the delta is applied deterministically rather than read back from state that may
or may not have caught up.

**Alternative considered**: change `SetStopUseCase`/`RemoveStopUseCase` to also return a stop count
(e.g. via `StopRepository.getStopCount`, which already exists). Rejected: it widens the blast radius
to three more use cases the user's clarifications did not scope in, for a value the ViewModel can
already compute correctly from data it holds.

### 4. `trip_opened` moves to `TripDetailViewModel.init`, fires once

Per clarification #1, `TripOpened` is removed from `TripListViewModel.onIntent(OnTripClicked)` and
added to `TripDetailViewModel.init`. It must fire once per screen entry (matching `screen_viewed`'s
once-per-navigation contract), not on every subsequent stops/legs update the way the existing
totals-calculation `combine` block does. Implementation: a **separate** `combine(observeStopsUseCase
(tripId), observeLegsUseCase(tripId)) { stops, legs -> ... }` piped through `.take(1)` before
`.onEach { … logEvent(TripOpened(...)) }.launchIn(viewModelScope)`, kept distinct from the existing
continuously-updating totals `combine` block (which must keep updating on every mutation). Using a
separate `.take(1)`-limited combine avoids coupling the one-shot event to the display logic and avoids
any risk of an accidental resubscription re-firing it.

`stopCount = stops.size`. `routeState`: `"complete"` if `legs.size == stops.size - 1 && stops.size >=
2` (the same completeness condition already used for `formattedTotalDistance`/
`isRouteRecalculationPromptVisible`), else `"none"`. `"stale"` is never emitted — see Non-Goals.

### 5. Dual-channel failure emission: via `trackFailure` on `AnalyticsTracking`

`AnalyticsTracking` provides a default `trackFailure(operation, errorType, throwable)` method that
calls `logEvent(AnalyticsEvent.OperationFailed(operation, errorType))` then `trackException(throwable,
operation)` — encoding the two-call contract once so no failure path can fire one without the other.

Every existing and new failure path calls `trackFailure` in the same `onFailure { error -> ... }`
lambda, in this order: classify once (`val errorType = classifyAnalyticsError(error)`), then
`analyticsClient.trackFailure(operation, errorType, error)`. This touches all 8 existing
`OperationFailed` sites (2 in `TripListViewModel`, 6 in `TripDetailViewModel`) plus the 2 new ones
(`SEARCH_PLACE`, `RESOLVE_PLACE`).

The `trackException` method remains on the interface (each implementation owns its own backend's
exception handling) but is no longer called directly from ViewModels — only from `trackFailure`
within `core/analytics/` itself.

### 6. `VeniceApplication` ordering

`AnalyticsClient` is injected via Hilt (the class is already `@HiltAndroidApp`, so member injection
via `@Inject lateinit var` is used, consistent with `Application` not supporting constructor
injection). `setUserProperty(AnalyticsUserProperty.DistanceUnit(...))` is called as the **first**
statement in `onCreate()`, before `super.onCreate()` is not possible (Hilt's injection happens during
`super.onCreate()` via `AndroidInjector` for `Application`... actually Hilt injects `Application`
fields immediately after `super.onCreate()` completes for `@HiltAndroidApp`) — so the call is placed
immediately after `super.onCreate()` and before `Places.initializeWithNewPlacesApiEnabled(...)`, which
is the earliest point at which injection has completed and no other code in the app has run. This
satisfies "before the first event of the session" since no `AnalyticsClient.logEvent` call exists
anywhere in application startup before this point — the earliest possible event is `ScreenViewed` in
`TripListViewModel.init`, which cannot run before `Application.onCreate()` completes.

`distance_unit` value: `if (DistanceFormatter.isImperialLocale(...)) "imperial" else "metric"` — reuses
the existing formatter utility rather than re-deriving locale logic.

### 7. `AnalyticsLogFormatter` needs no change

`formatEventLog`/`formatUserPropertyLog`/`formatExceptionLog` operate generically on
`event.name`/`event.properties`/`property` (via `toString()`)/`throwable.message` — none of them
pattern-match on the old subtypes. Confirmed by inspection; a test is still added asserting the
formatter produces a distinguishable line for one of the new event shapes, to guard against silent
breakage if a future change makes the formatter type-aware.

## Risks / Trade-offs

- **[Risk]** Moving `TripOpened` out of `TripListViewModel` and using a second `combine` block in
  `TripDetailViewModel.init` could double-fire if `.take(1)` is misapplied to the wrong flow.
  → **Mitigation**: dedicated BDD test asserting `TripOpened` is logged exactly once across multiple
  stops/legs emissions in the same ViewModel instance.
- **[Risk]** `RouteRepository.calculateRoute`'s return-type change is a public interface change; any
  other caller would need updating. → **Mitigation**: grep confirms `CalculateRouteUseCase` is the
  only caller; covered by the use case's updated unit test.
- **[Risk]** Reusing `currentStop`/`lastDeparted` inside the use cases (rather than re-fetching after
  `updateStopStatus`) returns a locally-constructed `copy()`, not a DB read-back — if
  `updateStopStatus` silently changed other fields, the returned `Stop` would not reflect that.
  → **Mitigation**: `updateStopStatus` only ever changes `status`, confirmed by inspecting
  `StopRepository`'s signature (`status: StopStatus` is its only mutable input); acceptable.
  Documented as an implicit contract in the use case's KDoc.
  → **Follow-up if it breaks**: switch to a repository read-back rather than a local copy.
- **[Trade-off]** `route_state` can only be `none`/`complete` until `InvalidateRouteUseCase` is
  changed to mark legs stale rather than delete them — a real product gap the tracking plan already
  flags as a design decision, not a defect of this change. No task in this change touches
  `InvalidateRouteUseCase`.
- **[Risk]** `AnalyticsErrorType` declares 3 values (`not_found`, `permission_denied`,
  `quota_exceeded`) the classifier can never produce in this change, which could look like dead code to
  a reviewer or a coverage tool. → **Mitigation**: documented inline per the project's data-modelling
  discipline; Kover's line coverage is unaffected since the enum constants themselves need no test
  coverage (they are declarations, not branches) — only the classifier function's branches need
  boundary tests, and all of *its* branches are exercised.

## Migration Plan

No runtime migration — this is a code-and-test change to files with no live provider/consumer beyond
the debug Logcat sink (confirmed in the tracking plan's migration table: "Nothing consumes them...
this migration is free"). Deployment is a normal PR merge; no feature flag or staged rollout needed.
Rollback is a normal revert if a build or test regression surfaces post-merge.

## Open Questions

None outstanding — all ambiguities were resolved in the story's clarifications (routeState scope,
error-classification granularity, route-calculated banding source, stop-position sourcing) before this
design was written.
