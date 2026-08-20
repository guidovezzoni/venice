## Why

The current 10-event taxonomy will not survive contact with a real analytics backend: almost every
event carries a raw UUID (`trip_id`, `stop_id`) that collapses into GA4's `(other)` bucket while
consuming the limited custom-dimension budget, `operation_failed` forwards `error.message` verbatim
(unbounded cardinality and a live PII path), and `stop_set` emits `stopType.name`, leaking
`SCREAMING_SNAKE_CASE` into an otherwise `snake_case` dataset. Five user actions — undoing a
departure, place-search success/failure, suggestion selection, and navigation launch — emit nothing at
all. `docs/analytics/tracking-plan.md` already defines the target 14-event taxonomy; this change makes
the code match it before any real backend is wired, while the rewrite is still free (no historical data,
no dashboards, no funnels to break).

## What Changes

- **BREAKING**: `AnalyticsEvent`'s 10 legacy subtypes are replaced by the 14 tracking-plan events.
  Every event that carried a raw identifier now carries low-cardinality aggregates instead
  (`stop_count`, `stop_position`, bands) — no call site constructs an `AnalyticsEvent` the same way
  as before.
- **BREAKING**: `AnalyticsOperation` grows from 7 to 10 values (`UNDO_MARK_DEPARTED`, `SEARCH_PLACE`,
  `RESOLVE_PLACE` added); `OperationFailed.errorMessage: String` is replaced by
  `OperationFailed.errorType: AnalyticsErrorType`, a bounded enum classified from the throwable's type.
- Six new parameter-vocabulary types added to `core/analytics/`, one per file:
  `DistanceBand`, `DurationBand`, `CountBand` (banding/classifying functions, boundary-tested),
  `AnalyticsScreen`, `StopTypeParam`, `AnalyticsErrorType` (flat vocabularies), plus a top-level
  `AnalyticsErrorClassifier` function that maps a `Throwable` to an `AnalyticsErrorType` by exception
  type only (`SocketTimeoutException` → `timeout`, other `IOException` → `network`, `SQLiteException`
  → `persistence`, else `unknown`).
- `AnalyticsUserProperty`'s `ForTesting` placeholder is replaced by two real subtypes —
  `TripCountBand` and `DistanceUnit` — wired to fire at the moments the tracking plan specifies (trip
  list load / trip created / trip removed; app start, before the first event).
- Every failure path that logs `OperationFailed` is paired with a `trackException(throwable, operation)`
  call on the same path — the dual-channel pattern the abstraction layer (9.1.1) built but nothing yet
  calls.
- Five previously-silent user actions gain events: `stop_departure_undone`, `place_search_performed`,
  `place_suggestion_selected`, `navigation_launched`, plus the two new `operation` values covering
  place-search and place-detail failures. The deferred TODO at `TripDetailViewModel.kt:148` is resolved
  — `route_calculated`'s `duration_band` now covers what that TODO deferred.
- `trip_opened` moves from `TripListViewModel.onIntent(OnTripClicked)` to
  `TripDetailViewModel.init`, where `stop_count` and `route_state` are already observable; it fires
  once per screen entry, sourced from the first combined stops/legs emission.
- Two small, well-scoped domain-layer return-type changes, needed only to source event data without
  re-deriving it from possibly-stale UI state:
  - `RouteRepository.calculateRoute` / `CalculateRouteUseCase` return the calculated `List<Leg>` on
    success (previously `Result<Unit>`), so the ViewModel can band the totals it already has in hand
    instead of re-fetching.
  - `MarkStopDepartedUseCase` / `UndoMarkStopDepartedUseCase` return the affected `Stop` on success
    (previously `Result<Unit>`), so the ViewModel can source `stop_position` from `Stop.order` directly.
- `VeniceApplication` gains an injected `AnalyticsClient` and sets the `distance_unit` user property in
  `onCreate()`, before any other initialisation that could emit an event.

## Capabilities

### New Capabilities
- `analytics-taxonomy`: the event dictionary itself — the 14 `AnalyticsEvent` subtypes, the parameter
  vocabularies and banding/classifying functions that back them (`DistanceBand`, `DurationBand`,
  `CountBand`, `AnalyticsScreen`, `StopTypeParam`, `AnalyticsErrorType`, `AnalyticsErrorClassifier`),
  and the two `AnalyticsUserProperty` subtypes. Governs what the analytics layer is capable of
  representing and the bounding/privacy rules each event and parameter must satisfy.
- `analytics-instrumentation`: where and when the taxonomy is emitted — the ViewModel call sites in
  `TripListViewModel` and `TripDetailViewModel`, the dual-channel failure-emission requirement, the
  `VeniceApplication` startup user-property assignment, and the domain use-case return-type changes
  that exist solely to source event data.

### Modified Capabilities
- `analytics-abstraction`: two requirements from the 9.1.1 structural upgrade pinned the *old*
  taxonomy shape and are superseded here — "`AnalyticsOperation` carries exactly the existing seven
  values" (now ten) and "the analytics taxonomy is unchanged by this structural upgrade" (the 10
  subclasses are intentionally and completely replaced by 14). The structural requirements of that
  spec (layering, `AnalyticsTracking` surface, composite fan-out, debug-provider gating) are untouched.

## Impact

- **Code**: `core/analytics/AnalyticsEvent.kt` (rewrite), `AnalyticsOperation.kt`,
  `AnalyticsUserProperty.kt`, `AnalyticsLogFormatter.kt` (verify only); 7 new files in
  `core/analytics/`; `ui/viewmodel/TripListViewModel.kt` and `TripDetailViewModel.kt` (all call sites);
  `VeniceApplication.kt`; `domain/usecase/CalculateRouteUseCase.kt`,
  `MarkStopDepartedUseCase.kt`, `UndoMarkStopDepartedUseCase.kt`;
  `domain/repository/RouteRepository.kt` and its implementation.
- **Tests**: full rewrite of `AnalyticsEventTest.kt`; new test files for each banding/classifying
  function; `AnalyticsUserPropertyTest.kt`; updates to `TripListViewModelTest.kt`,
  `TripDetailViewModelTest.kt`, and the three affected use-case tests. 95%+ Kover coverage maintained.
- **No consumers affected**: per the tracking plan's migration section, the only current provider is
  the debug Logcat sink — this rewrite is free, with no historical data or dashboards to break.
- **Dependencies**: none added; no new third-party SDK.
