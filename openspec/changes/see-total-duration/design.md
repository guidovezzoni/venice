## Context

Story 3.2.1 added a `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))`
collector to `TripDetailViewModel.init`, alongside (not replacing) the pre-existing
stops-only and legs-only `.onEach` collectors. That combine collector currently computes
only `formattedTotalDistance`: when `legs.size == stops.size - 1 && stops.size >= 2`, it sums
`leg.distanceMetres` and formats it via `formatDistance(sum, Locale.getDefault(),
application.resources)`; otherwise it sets `formattedTotalDistance = null`.
`DurationFormatter.formatDuration(durationSeconds: Int, resources: Resources): String`
already exists and is reused as-is for individual legs (sub-hour: `"%1$d min"`, ≥60 min:
`"%1$dh %2$dmin"`). `Leg.durationSeconds: Int` is a non-null field. `InvalidateRouteUseCase`
deletes all legs for a trip on any stop mutation (all-or-nothing), so — exactly as for
distance — duration is always either fully available (`legs.size == stops.size - 1`) or
entirely absent; a genuinely partial leg set does not occur today.

`TripTotalSummary.kt` is currently a single-value stateless composable (a `Column` rendering
either the distance label + value, or an "unavailable" sentence). This story adds a second
value (duration) to be shown alongside the first, which requires restructuring the layout
from a single-value renderer into a two-value, side-by-side renderer.

The project already has analytics infrastructure (`AnalyticsTracker` /
`AnalyticsEvent` sealed class, injected into `TripDetailViewModel` and used for events such
as `ScreenViewed`, `StopSet`, `RouteCalculated`). However, no existing event tracks the mere
*display* of a derived summary value — `formattedTotalDistance`'s display from 3.2.1 was not
instrumented either. Per user clarification, this change also does not add a new event for
total-duration display; it leaves a `// TODO` marker so the decision of whether/how to track
this is made deliberately in a follow-up rather than folded into a display-only change.

## Goals / Non-Goals

**Goals:**
- Add `TripDetailUiState.formattedTotalDuration: String?` (default `null`), computed entirely
  in the ViewModel, never in a composable.
- Derive the total from the existing `legs` list — no new domain/persisted field, no new
  network call, no new formatter.
- Extend the *existing* 3.2.1 combine collector rather than adding a fourth subscription, so
  both totals are computed from the same `stops`/`legs` emission and applied via a single
  `_uiState.update` call.
- Reuse the exact same completeness rule as 3.2.1 (`legs.size == stops.size - 1 &&
  stops.size >= 2`) so distance and duration always become available/unavailable together.
- Restructure `TripTotalSummary` into a side-by-side (`Row`) layout showing both totals, with
  a single combined "unavailable" message when both are absent, and full preview coverage of
  all four availability combinations.
- Clearly label the duration as driving time only (`"Est. driving time"`), so the user does
  not mistake it for total trip time including stops.

**Non-Goals:**
- No trip-list total (deferred, consistent with 3.2.1 — the trip list has no leg data
  today).
- No new "partial route with a warning" state — not reachable given all-or-nothing
  invalidation; if per-leg invalidation is introduced later, this can be revisited (same
  non-goal carried over from 3.2.1).
- No change to `formatDuration`, `LegSummary`, `TripProgressSummary`, or the existing
  stops-only / legs-only collectors.
- No new `AnalyticsEvent` subtype for this change — left as an explicit follow-up `TODO`,
  not a blocking requirement.

## Decisions

**1. Extend the existing 3.2.1 combine collector — no fourth subscription.**
Rather than adding a new, independent `combine` collector purely for duration (which would
duplicate the exact same `stops`/`legs` inputs and completeness check already computed for
distance), the existing collector's lambda is extended to compute both values and return
`Pair<String?, String?>` (`formattedTotalDistance to formattedTotalDuration`). The `.onEach`
destructures the pair and applies both fields in one `_uiState.update { it.copy(...) }` call.
Alternative considered: a small local data class (e.g. `TripTotals(distance, duration)`) —
rejected in favour of `Pair`, since a dedicated model class for a purely transient,
single-use combine-result would conflict with the "one class per file" convention for a
type that carries no independent meaning outside this one collector.

**2. Completeness rule is unchanged and shared: `legs.size == stops.size - 1 &&
stops.size >= 2`.** Both `formattedTotalDistance` and `formattedTotalDuration` are derived
from the same boolean check inside the same lambda invocation, guaranteeing they always
transition to/from "unavailable" together — this is what makes the combined
`trip_detail_totals_unavailable` message safe to introduce (see Decision 5).

**3. Duration summed as `Long`, then narrowed to `Int` for `formatDuration`.**
`legs.sumOf { it.durationSeconds.toLong() }` avoids `Int` overflow during accumulation (the
same pattern already applied to distance in 3.2.1, adapted for duration); the `Long` sum is
then narrowed with `.toInt()` before being passed to the existing
`formatDuration(durationSeconds: Int, resources: Resources)`, which is not modified. As with
distance, overflow would require a physically unrealistic number of legs/duration to occur
in practice — this is a defensive-but-inexpensive safeguard, not a response to an observed
bug.

**4. `TripTotalSummary` restructured to a `Row` of two equal-width halves, with a combined
unavailable message.** The composable keeps its outer `Column` (existing padding), but
switches to:
- **Both totals available** (or at least one available, per Decision 4a below): a `Row`
  containing two `Modifier.weight(1f)` halves, distance first (left) then duration (right),
  matching the existing distance-then-duration ordering used elsewhere on this screen
  (`formattedLegDistances` / `formattedLegDurations`, `LegSummary`).
- **Both totals unavailable**: a single `Text` showing `trip_detail_totals_unavailable`,
  replacing the `Row` entirely (not two independent unavailable texts side by side), per
  user clarification — since Decision 2 guarantees the two metrics are only ever
  simultaneously available or simultaneously unavailable today, this is the only reachable
  "unavailable" path, but the combined message is scoped to exactly the both-null case so it
  degrades safely if that invariant is ever relaxed (see Decision 4a).

**4a. One-available/one-unavailable is handled defensively, even though unreachable today.**
Per user clarification ("should be handled" for the mixed case), each half of the `Row`
independently falls back to its own metric-specific unavailable text
(`trip_detail_total_distance_unavailable` / `trip_detail_total_duration_unavailable`) if only
that value is `null`, rather than collapsing to the combined message. This keeps the
combined `trip_detail_totals_unavailable` message strictly reserved for the "both absent"
case and avoids silently hiding a value that legitimately became available. This branch has
no current code path that can exercise it (Decision 2), but is included as a defensive
fallback and is covered by a dedicated preview per the story's clarification.

**5. Three new string resources, no formatted templates.** `trip_detail_total_duration_label
= "Est. driving time"`, `trip_detail_total_duration_unavailable = "Est. driving time
unavailable"`, and `trip_detail_totals_unavailable = "Trip totals unavailable"` are plain
strings (no placeholders), mirroring the plain-string pattern already used for
`trip_detail_total_distance_label` / `trip_detail_total_distance_unavailable` in 3.2.1.

**6. Analytics: `TODO` marker, no new event in this change.** Per user clarification, a
dedicated analytics event for total-duration display is desired eventually, but this project
already has analytics infrastructure (`AnalyticsTracker`) that is not currently used to track
the *display* of any derived summary value (3.2.1's total distance display is not tracked
either). Rather than introduce a first-of-its-kind "value displayed" event as a side effect
of a display-only change, a `// TODO` code comment and a corresponding task record the need
for a follow-up decision on what to track and how (e.g. on first render vs. every
recomposition), so the eventual event is designed deliberately rather than bolted on here.

## Risks / Trade-offs

- **[Risk] Existing `TripDetailViewModelTest` "Section 2: Total Distance" assertions must
  keep passing unchanged** → Extending the combine lambda's return type from `String?` to
  `Pair<String?, String?>` changes an internal implementation detail only; all existing
  assertions read `viewModel.uiState.value.formattedTotalDistance`, which is unaffected.
  **Mitigation**: none needed beyond running the existing suite; new assertions for
  `formattedTotalDuration` are added alongside, not replacing, the existing ones.
- **[Risk] `TripTotalSummary` layout restructuring could regress the 3.2.1
  distance-only visual/test expectations** → The existing `TripTotalSummaryTest` cases
  (`nonNullFormattedTotalDistance_showsLabelAndFormattedValue`,
  `nullFormattedTotalDistance_showsUnavailableTextAndNoNumericValue`) assert the previous
  single-value strings/behaviour. **Mitigation**: those tests are updated in this change (not
  left stale) to pass both parameters and assert the new combined/side-by-side behaviour;
  the underlying label/value text content for distance is otherwise unchanged.
- **[Risk] Preview coverage gap** → Adding `formattedTotalDuration` to `TripDetailUiState`
  requires at least one non-default (non-null) value across `TripDetailScreen`'s previews,
  and `TripTotalSummary` needs coverage of all four availability combinations.
  **Mitigation**: update `PreviewTripDetailScreenWithLegs` to pass a non-null
  `formattedTotalDuration`, and add four dedicated `TripTotalSummary` previews per the
  story's preview matrix.
- **[Risk] Locale-dependent string collisions in Compose UI tests** → The formatted duration
  total could textually collide with an individual leg's formatted duration (e.g. both
  showing "15 min"). **Mitigation**: use `onAllNodesWithText(...)[index]` or match on the
  total's distinguishing label text ("Est. driving time"), exactly as already done for the
  distance total's "Total distance" label.
- **[Carried over, out of scope] Pre-existing spec drift** — the main spec
  (`openspec/specs/trip-detail/spec.md`) still has some fields that predate the `DialogState`
  / `RouteCalculationState` consolidation; not addressed by this change (same note as 3.2.1's
  design).

## Migration Plan

No data migration required — this is a UI-layer/derived-value addition with no persistence
or API changes. Single-branch rollout; a plain revert is sufficient if issues surface, since
no stored data or schema is touched.

## Open Questions

None — signature, flow structure, string wording, layout, and the analytics deferral were
all resolved via user clarification before this design was written.
