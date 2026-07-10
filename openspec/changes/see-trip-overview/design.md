## Context

`TripDetailScreen` already renders ordered stops, per-leg breakdowns (`LegSummary`), trip
totals (`TripTotalSummary`, from 3.2.1/3.2.2), and progress (`TripProgressSummary`). Its
`TopAppBar` title is hardcoded to `stringResource(R.string.trip_detail_title)` ("Trip
Detail") — the screen never observes the `Trip` entity itself, only its `Stop`s (via
`ObserveStopsUseCase`) and `Leg`s (via `ObserveLegsUseCase`). `TripRepository` currently
exposes only `createTrip` and `observeTrips` (a list); there is no single-trip read.
`TripDao.getById(id): TripWithStopCount?` exists but is `suspend` (one-shot), not a `Flow`,
so it cannot drive a `StateFlow`-backed `UiState` field the way the rest of the screen does.

Route completeness is already computed in `TripDetailViewModel.init`, inside the
`combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))` collector added by 3.2.1
and extended by 3.2.2: `legs.size == stops.size - 1 && stops.size >= 2`. When this is
`false`, `formattedTotalDistance` / `formattedTotalDuration` are both `null` and
`TripTotalSummary` renders "Trip totals unavailable" — but nothing on screen explains *why*
or *what to do*. The "Calculate route" button already exists below the stop list, gated on
`allStops.size >= 2` (computed inline in the composable from `uiState.startingPoint` /
`uiState.intermediateStops` / `uiState.destination`), and already dispatches
`TripDetailUiIntent.OnCalculateRouteClicked`.

`InvalidateRouteUseCase` deletes **all** legs for a trip on any stop mutation
(add/remove/move/edit) — route completeness is always all-or-nothing today, so a single
generic "route needs (re)calculation" prompt is sufficient; there is no reachable
"partially stale" state to design for (same invariant relied on by 3.2.2's combined
"Trip totals unavailable" message).

`TripDetailScreen` is invoked from `MainScreen.kt`'s `NavHost` with only `tripId` in the
route (`"tripDetail/{tripId}"`) — no trip name is passed via navigation args, so the trip
name must be sourced from persistence, not from the caller.

## Goals / Non-Goals

**Goals:**
- Show the real trip name in the `TopAppBar`, observed live via a new `Flow<Trip?>` read
  path, falling back to the existing "Trip Detail" string while the name has not arrived
  yet (first composition, or a not-yet-loaded trip) — no new loading indicator.
- Show a single, generic, tappable prompt explaining that legs/totals are unavailable
  because the route needs to be (re)calculated, reusing the **existing**
  `OnCalculateRouteClicked` intent — no new intent, no schema change, no distinction
  between "never calculated" and "stale".
- Reuse the existing route-completeness check and the existing 2+-stops visibility guard
  (matching the "Calculate route" button) so the prompt's visibility rule is derived once,
  in the ViewModel, from data already loaded — not recomputed ad hoc in the composable.
- Keep `TripDetailViewModel`'s existing collector structure: extend, don't replace, the
  3.2.1/3.2.2 `combine` collector for prompt visibility; add one new, independent collector
  for the trip name (a distinct data source, so a distinct collector, following the same
  pattern already used for the stops-only and legs-only collectors).

**Non-Goals:**
- No new `TripDetailUiIntent` — the prompt dispatches `OnCalculateRouteClicked`.
- No distinction between "route never calculated" and "route stale after a mutation" — a
  single message covers both, per product clarification, since `InvalidateRouteUseCase`'s
  all-or-nothing deletion makes them behaviourally identical from the UI's perspective.
- No database schema/version change — `observeById` reads the same `trips`/`stops` tables
  with the same join shape as the existing `observeAll()` query.
- No editing of the trip name from this screen (out of scope; a future story).
- No new `AnalyticsEvent` for the prompt tap — it dispatches the same
  `OnCalculateRouteClicked` intent already tracked (indirectly, via
  `AnalyticsEvent.RouteCalculated` / `OperationFailed`) by the existing "Calculate route"
  button's handler; no separate "prompt tapped" signal is introduced.

## Decisions

**1. `TripDao.observeById` is a new `Flow`-returning query, not a change to the existing
`suspend fun getById`.** The existing one-shot `getById` is left untouched (it may still be
used elsewhere or in future one-shot contexts). The new query mirrors `observeAll()`'s exact
SQL shape (same `stopCount` correlated subquery) filtered by `WHERE t.id = :id`, returning
`Flow<TripWithStopCount?>` — nullable because a trip ID that does not (or no longer) exist
must not crash the collector. Alternative considered: making `getById` itself return a
`Flow` — rejected because `TripListViewModel`/other call sites may rely on its one-shot
`suspend` semantics, and changing its signature is a wider blast radius than this
display-only change needs.

**2. `TripRepository.observeTripById` and `ObserveTripUseCase` mirror the existing
`observeStopsForTrip` / `ObserveStopsUseCase` and `observeLegsForTrip` / `ObserveLegsUseCase`
shape exactly** — a single-argument `Flow`-returning repository method wrapped by a thin,
single-`operator fun invoke` use case with no extra logic. This keeps the three "observe
this trip's related data" call sites (stops, legs, and now the trip itself) structurally
identical in `TripDetailViewModel.init`.

**3. `tripName` is `String?` (not `String` with an empty-string sentinel).** Per product
clarification, the composable must fall back to the `trip_detail_title` string resource
while the real name has not arrived. Using `null` as "not yet known" (rather than `""`) lets
the composable branch with `uiState.tripName ?: stringResource(R.string.trip_detail_title)`
— a single, unambiguous null-coalescing expression — instead of an
empty-string-means-unknown convention that would be easy to confuse with a
(hypothetically) blank trip name.

**4. `isRouteRecalculationPromptVisible: Boolean` is derived inside the existing
3.2.1/3.2.2 `combine` collector, not a new fourth collector.** The collector already computes
the exact boolean this prompt needs (`stops.size >= 2 && legs.size == stops.size - 1`, i.e.
"complete") in order to decide between formatted totals and `null`/`null`. Extending its
return type from `Pair<String?, String?>` to a small result carrying three values (distance,
duration, and completeness) avoids recomputing the same `stops`/`legs` inputs a second time.
Alternative considered: reusing `formattedTotalDistance == null` as a proxy for "prompt
visible" directly in the composable — rejected because it (a) would put a business-rule
inference back into the composable (violates "composables are purely presentational") and
(b) conflates "totals unavailable" with "2+ stops", which are related but distinct
conditions — a trip with 0 or 1 stops has `formattedTotalDistance == null` too, but must
**not** show the recalculation prompt (there is nothing to calculate yet). Making
`isRouteRecalculationPromptVisible` its own explicit field, computed with its own
`stops.size >= 2` guard, keeps that distinction correct and explicit rather than relying on
the reader to re-derive it from an unrelated field.

**5. The extended combine lambda returns a small local data class, not a wider `Pair`/`Triple`.**
Now that three values are produced together (distance, duration, prompt-visibility), a
`Triple<String?, String?, Boolean>` would be positionally ambiguous at the call site. A
private, file-local data class (e.g. `TripTotalsAndPromptState`) makes the `.onEach { ... }`
destructuring self-documenting. This does not conflict with the "one class per file"
convention, since it is a `private` type scoped to `TripDetailViewModel.kt` itself, not a
shared model — the same rationale that would have applied to 3.2.2's `Pair` choice, revisited
now that a third value is added.

**6. `RouteRecalculationPrompt` is its own composable file**, mirroring `TripTotalSummary.kt`
/ `TripProgressSummary.kt`, rather than being inlined into `TripDetailScreen.kt`. It accepts
`modifier: Modifier = Modifier`, `isEnabled: Boolean` (mirrors the existing "Calculate route"
button's `!isCalculating && !uiState.isLoading` enablement rule, passed as a single
precomputed boolean — no business logic inside the composable), and `onClick: () -> Unit =
{}`. It renders a `Card` (or equivalent tappable container) with a short plain-language
message plus a call-to-action label, both new string resources. Placed in the `LazyColumn`
immediately before `TripTotalSummary`'s item, so the explanation appears right above the
"unavailable" totals it explains.

**7. Two new string resources, no formatted templates.**
`trip_detail_recalculation_prompt_message` (explains why totals/legs are missing) and
`trip_detail_recalculation_prompt_action` (the tappable call-to-action label) are plain
strings, mirroring the plain-string pattern already used for `trip_detail_totals_unavailable`
etc. Both are mirrored, translated, into `values-it` and `values-es-rES`.

## Risks / Trade-offs

- **[Risk] `TripDao.observeById` duplicates `observeAll()`'s SQL shape** → a schema change to
  either query could silently desync the other. **Mitigation**: both queries are covered by
  DAO-level tests (existing for `observeAll`, new for `observeById`) asserting identical
  `stopCount` semantics; no runtime coupling is introduced beyond the shared SQL pattern.
- **[Risk] A trip ID that never resolves (e.g. deleted trip, bad deep link) leaves
  `tripName` permanently `null`** → the screen already handles a similar "trip has no data
  yet" situation for stops/legs (both default to empty), so this degrades to the existing
  "Trip Detail" fallback title indefinitely rather than crashing or showing a blank title —
  an acceptable, non-blocking degradation; no explicit "trip not found" state is introduced
  by this change (would require a new UI state / error path out of scope here).
- **[Risk] `isRouteRecalculationPromptVisible` and `formattedTotalDistance ==
  null`/`formattedTotalDuration == null` could drift out of sync if a future change alters
  one condition but not the other** → both are computed from the same completeness boolean
  in the same `combine` lambda invocation (Decision 4/5), so they are structurally
  guaranteed to agree on the "route incomplete" case; the only difference is the additional
  `stops.size >= 2` gate applied to the prompt. **Mitigation**: covered by ViewModel tests
  asserting the two flags in lockstep for `stops.size >= 2`, and asserting the prompt stays
  `false` for `stops.size < 2` even though totals are also `null` there.
- **[Risk] Preview coverage gap** → two new `UiState` fields (`tripName`,
  `isRouteRecalculationPromptVisible`) require non-default coverage across
  `TripDetailScreen`'s ~17 previews. **Mitigation**: update at least one existing preview
  per field (e.g. `PreviewTripDetailScreenWithLegs` gets a non-null `tripName`;
  `PreviewTripDetailScreenTotalUnavailable` gets `isRouteRecalculationPromptVisible = true`),
  plus dedicated previews for `RouteRecalculationPrompt` itself (enabled / disabled).
- **[Carried over, out of scope] Pre-existing spec drift** — the main `trip-detail` spec and
  `trip-persistence` spec predate some implementation details (e.g. `TripDao.getById` is
  documented as returning `TripEntity?` while the real code returns `TripWithStopCount?`).
  Not addressed by this change (same note carried over from 3.2.1/3.2.2's design docs); the
  new `observeById` delta requirement in this change documents the real
  `TripWithStopCount?`-based return type to avoid adding a second drifted requirement.

## Migration Plan

No data migration required — no schema/version change. The new `observeById` DAO query and
`observeTripById` repository method are purely additive (no existing method signatures
change). Single-branch rollout; a plain revert is sufficient if issues surface.

## Open Questions

None — visibility rule, field types, fallback behaviour, and string wording were all
resolved via user clarification before this design was written.
