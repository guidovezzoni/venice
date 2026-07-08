## Why

Story 3.2.1 added a derived **total distance** to the Trip Detail screen, giving the user a
single figure for the scale of the whole trip. The user still has no equivalent figure for
how long the drive will take — only per-leg durations are shown today. Story 3.2.2 adds a
derived **total estimated driving duration** — the sum of all calculated leg durations —
displayed alongside the total distance, clearly labelled as driving time only (it excludes
stop time, breaks, etc.). No new domain/persisted field and no new network call are
required; the value is computed from data already loaded into `TripDetailUiState.legs`.

## What Changes

- Add `TripDetailUiState.formattedTotalDuration: String?` (default `null`) — a pre-computed,
  locale-aware formatted string, following the existing `formattedTotalDistance` pattern
  (formatting happens in the ViewModel, never in the composable).
- Extend the existing `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))`
  collector in `TripDetailViewModel.init` (added by 3.2.1) to compute **both**
  `formattedTotalDistance` and `formattedTotalDuration` in a single lambda, and apply both
  fields via a single `_uiState.update { it.copy(...) }` call — no new/fourth subscription
  is introduced. The lambda returns a `Pair<String?, String?>` (distance to duration),
  destructured in the `onEach`.
  - Completeness rule is unchanged: `legs.size == stops.size - 1 && stops.size >= 2`.
  - When complete: sums `leg.durationSeconds` (accumulated as `Long` to guard against
    overflow, then narrowed back to `Int`) and formats it via the existing
    `formatDuration(durationSeconds, resources)` (no new formatter).
  - When not complete: both `formattedTotalDistance` and `formattedTotalDuration` are `null`.
- Restructure `ui/screens/tripdetail/TripTotalSummary.kt` from a single-value `Column` into a
  side-by-side layout:
  - Add a `formattedTotalDuration: String?` parameter.
  - When at least one of `formattedTotalDistance` / `formattedTotalDuration` is non-null,
    render a `Row` with two equal-width (`Modifier.weight(1f)`) halves — distance on the
    left, duration on the right — each showing its own label + value, or its own
    metric-specific "unavailable" text if only that one metric is absent (handles the
    currently-unreachable but defensively-supported partial case).
  - When **both** are `null`, render a single combined `trip_detail_totals_unavailable`
    message instead of two independent "unavailable" texts, since both metrics always
    become unavailable simultaneously today (all-or-nothing route invalidation).
- Wire the new `formattedTotalDuration` parameter into the `TripTotalSummary` call site in
  `TripDetailScreen.kt`.
- Add three new string resources (`trip_detail_total_duration_label`,
  `trip_detail_total_duration_unavailable`, `trip_detail_totals_unavailable`) to the base
  `values/strings.xml` and mirror them, translated, to `values-it` and `values-es-rES`.
- Add a `// TODO` code comment (and a corresponding non-blocking task) noting that a
  dedicated analytics event for total-duration display should be wired in a follow-up story;
  this change does not add a new `AnalyticsEvent` subtype, since displaying a derived summary
  value is not currently tracked elsewhere on this screen (e.g. `formattedTotalDistance`
  display from 3.2.1 has no tracking event either) and adding one is a product-analytics
  decision better scoped as its own follow-up rather than bundled into this display-only
  change.
- Update previews for `TripTotalSummary` (four states: both available, both unavailable,
  distance-only available, duration-only available) and update `TripDetailScreen` previews
  so the new `UiState` field has non-default coverage.

Trip-list total display remains out of scope (deferred, per 3.2.1) — the trip list has no
access to leg data today.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `trip-detail`: `TripDetailUiState` gains a `formattedTotalDuration: String?` field; the
  existing combined stops+legs collector in `TripDetailViewModel` (from 3.2.1) is extended to
  also derive the total duration and its completeness in the same emission;
  `TripDetailScreen` renders `TripTotalSummary` with both totals side by side, or a single
  combined "unavailable" message when both are absent.
- `route-display`: the route calculation string resources requirement gains three new keys
  for the total-duration label, its unavailable state, and the combined totals-unavailable
  message.

## Impact

- **Affected code**: `ui/state/TripDetailUiState.kt`, `ui/viewmodel/TripDetailViewModel.kt`,
  `ui/screens/tripdetail/TripTotalSummary.kt`, `ui/screens/tripdetail/TripDetailScreen.kt`,
  `res/values/strings.xml`, `res/values-it/strings.xml`, `res/values-es-rES/strings.xml`.
- **Affected tests**: `TripDetailViewModelTest.kt` (extend the existing "Section 2: Total
  Distance" combine-collector assertions to also assert `formattedTotalDuration`),
  `TripTotalSummaryTest.kt` (androidTest, new side-by-side / combined-unavailable /
  partial-availability cases), `TripDetailScreenTest.kt` (androidTest, total duration
  displayed / unavailable).
- **No new dependencies, no database/schema changes, no network/API changes.**
- **Reuses `DurationFormatter.formatDuration` as-is** — no new formatter is introduced.
- **No new `AnalyticsEvent` subtype** — a `TODO` marks this as a scoped follow-up.
