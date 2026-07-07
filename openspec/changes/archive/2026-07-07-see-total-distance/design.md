## Context

`TripDetailViewModel.init` currently runs two independent `.onEach { ... }.launchIn(viewModelScope)`
collectors: one on `observeStopsUseCase(tripId)` (updates `startingPoint`, `destination`,
`intermediateStops`, `canAddMoreStops`, `formattedStopCoordinates`) and one on
`observeLegsUseCase(tripId)` (updates `legs`, `formattedLegDistances`, `formattedLegDurations`
via `formatDistance`/`formatDuration`). `TripDetailUiState` is a flat immutable data class;
`DistanceFormatter.formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String`
already exists and is reused as-is for individual legs. `Leg.distanceMetres` is a non-null
`Int`. Every `Stop` is placed (no "unplaced" concept), so `placedStopCount == stops.size`.
`InvalidateRouteUseCase` deletes all legs for a trip on any stop mutation (all-or-nothing),
so legs are always either complete (`legs.size == stops.size - 1`, `stops.size >= 2`) or
entirely absent — a genuinely partial set does not occur today.

Note on pre-existing spec drift (out of scope for this change): the main spec
(`openspec/specs/trip-detail/spec.md`) still describes `TripDetailUiState` with fields such
as `isSetStartingPointDialogVisible` / `isCalculatingRoute` / `routeError`, but the actual
code (post `Feature/pl012 refactor`) has since consolidated these into `dialogState:
DialogState` and `routeCalculationState: RouteCalculationState`, without an accompanying
spec sync. This delta only touches the bullet/scenario relevant to the new field and does
not attempt to correct that unrelated drift; a future documentation-only change should
resync `trip-detail`'s `TripDetailUiState` requirement with the current code.

## Goals / Non-Goals

**Goals:**
- Add `TripDetailUiState.formattedTotalDistance: String?` (default `null`), computed
  entirely in the ViewModel, never in a composable.
- Derive the total from the existing `legs` list — no new domain/persisted field, no new
  network call, no new formatter.
- Update the total automatically whenever stops or legs change, using a Flow-driven
  collector rather than a manual recomputation trigger.
- Treat any non-complete route (no legs, partial legs, or fewer than 2 stops) uniformly as
  "unavailable" (`null`), matching the all-or-nothing invalidation model.
- Add a small, stateless, presentational composable (`TripTotalSummary`) that renders the
  precomputed value or an "unavailable" label, with full preview coverage.

**Non-Goals:**
- No trip-list total (deferred to a future story — the trip list has no leg data today).
- No new "partial route with a warning" state — not reachable given all-or-nothing
  invalidation; if per-leg invalidation is introduced later, this can be revisited.
- No new string-format placeholders beyond the two new static-text keys — the total reuses
  `formatDistance`'s existing distance strings for the value itself.
- No change to `formatDistance`, `LegSummary`, `TripProgressSummary`, or the existing
  stops-only / legs-only collectors.
- No new analytics event.

## Decisions

**1. A third, independent `combine()` collector — existing collectors untouched.**
Rather than folding the total into the existing legs-only `.onEach` collector (which only
sees `legs`, not `stops`, and cannot alone evaluate completeness), a new
`combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId)) { stops, legs -> ... }`
collector is added in `init`, alongside (not replacing) the two existing collectors.
Alternative considered: extend the legs-only collector to also read `_uiState.value.stops`
snapshot-style — rejected because it would read stale/racy state instead of reacting to
the current `stops` emission, and would silently couple two independently-tested code
paths. A dedicated `combine` collector keeps the total's derivation isolated, testable, and
Flow-correct (re-evaluates whenever either source changes, in any order).

**2. Completeness rule: `legs.size == stops.size - 1 && stops.size >= 2`.**
Directly reflects `placedStopCount == stops.size` (every `Stop` is placed) and the
all-or-nothing invalidation model confirmed in exploration: a trip with `stops.size >= 2`
either has exactly `stops.size - 1` legs (complete) or zero (invalidated/never
calculated) — a strictly-partial state does not occur in the current codebase, so no
separate "partial" branch is needed.

**3. Summation and formatting reuse `formatDistance` as-is; no new formatter.**
`legs.sumOf { it.distanceMetres }` returns an `Int`; safe for any realistic trip (would
require ~2.1 billion metres, i.e. more legs/distance than physically representable on
Earth, to overflow — consistent with the story's own overflow-safety note). The sum is
passed straight into the existing `formatDistance(sum, Locale.getDefault(),
application.resources)`, guaranteeing the total uses the exact same metric/imperial
unit-selection and rounding rules as each individual leg (AC 2).

**4. No `isRouteComplete` field — a nullable `String` carries both signal and value.**
Per user clarification, `TripDetailUiState` gains only `formattedTotalDistance: String?`.
`null` means "unavailable"; a non-null value means "complete and formatted". This keeps
`UiState` minimal (no redundant boolean) and keeps `TripTotalSummary` a pure
null-fallback renderer (`formattedTotalDistance ?: stringResource(unavailable)`), which is
presentation, not business logic — the completeness *decision* still happens in the
ViewModel.

**5. New composable `TripTotalSummary` mirrors `LegSummary` / `TripProgressSummary`.**
Stateless, `modifier: Modifier = Modifier` first parameter, `formattedTotalDistance:
String?` as its only data parameter, private `Dp` layout constants at the file top, three
`@Preview(showBackground = true)` private functions wrapped in `HeadingToVeniceTheme`
(present value, unavailable value — matching the project's per-field preview-coverage
rule). Placed as a new `LazyColumn` item in `TripDetailScreen.kt`, positioned after the
existing calculate-route button / route-error item block (footer area), per resolved
placement decision.

**6. String resources are two static keys, not a formatted template.**
`trip_detail_total_distance_label = "Total distance"` and
`trip_detail_total_distance_unavailable = "Total distance unavailable"` are plain strings
(no `%1$s` placeholder) — `TripTotalSummary` renders the label alongside the
already-formatted `formattedTotalDistance` value (two separate `Text` composables, mirroring
how `LegSummary` composes pre-formatted distance and duration side by side), or the single
unavailable sentence when the value is `null`.

## Risks / Trade-offs

- **[Risk] Unmocked resource-string calls in `TripDetailViewModelTest`** → Once the
  combine collector calls `formatDistance` for the total, existing tests that already stub
  the three distance-resource keys (`trip_detail_leg_distance_metres/kilometres/miles`) in
  `setUp()` are unaffected, since `formatDistance` reuses those same keys for any distance
  value including the sum. **Mitigation**: none needed beyond the existing stubs; no new
  resource keys are read by `formatDistance` itself (the two new string keys are read
  directly by the composable via `stringResource`, not by the ViewModel).
- **[Risk] `combine()` requires both source flows to emit before producing a value** →
  Tests using `createViewModel(stops = ..., legs = ...)` already stub both
  `observeStopsUseCase(TRIP_ID)` and `observeLegsUseCase(TRIP_ID)` with `flowOf(...)`, so
  the new collector's `combine` immediately receives both emissions — no factory change or
  new test scaffolding is required, per existing exploration.
- **[Risk] Preview coverage gap** → Adding `formattedTotalDistance` to `TripDetailUiState`
  requires at least one non-default (non-null) value across `TripDetailScreen`'s previews.
  **Mitigation**: update the preview that already shows legs (`PreviewTripDetailScreenWithLegs`
  or equivalent) to also pass a non-null `formattedTotalDistance`, and add one preview
  variant showing the unavailable state.
- **[Risk] Locale-dependent string collisions in Compose UI tests** → The formatted total
  value could textually collide with an individual leg's formatted distance (e.g. both
  showing "12.3 km"). **Mitigation**: use `onAllNodesWithText(...)[index]` or match on the
  total's distinguishing label text ("Total distance") per the project's Compose testing
  guideline, exactly as already done for other ambiguous-text cases in this screen's tests.
- **[Documentation drift, out of scope]** — see the Context note above regarding
  `TripDetailUiState`'s main spec already being stale relative to `DialogState` /
  `RouteCalculationState`; not addressed by this change.

## Migration Plan

No data migration required — this is a UI-layer/derived-value addition with no persistence
or API changes. Single-branch rollout; a plain revert is sufficient if issues surface, since
no stored data or schema is touched.

## Open Questions

None — signature, flow structure, string wording, and placement were all resolved via user
clarification before this design was written.
