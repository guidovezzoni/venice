## Why

Story 3.1.1–3.1.3 added per-leg distance and duration to the Trip Detail screen, but the
user has no single figure for the scale of the whole trip. Story 3.2.1 adds a derived
**total distance** — the sum of all calculated leg distances — so the user can gauge the
overall journey ("this trip is 1,240 km") without adding up the legs themselves. No new
domain/persisted field and no new network call are required; the value is computed from
data already loaded into `TripDetailUiState.legs`.

## What Changes

- Add `TripDetailUiState.formattedTotalDistance: String?` (default `null`) — a
  pre-computed, locale-aware formatted string, following the existing
  `formattedLegDistances` pattern (formatting happens in the ViewModel, never in the
  composable).
- Add a new, independent `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))`
  collector in `TripDetailViewModel.init`, separate from the existing stops-only and
  legs-only `.onEach` collectors (which remain untouched). This collector:
  - Determines route completeness as `legs.size == stops.size - 1 && stops.size >= 2`.
  - When complete: sums `leg.distanceMetres` over all legs and formats the sum via the
    existing `formatDistance(distanceMetres, locale, resources)` (no new formatter).
  - When not complete (no legs, partial legs, or fewer than 2 stops): sets
    `formattedTotalDistance = null`.
- Add a new stateless composable `ui/screens/tripdetail/TripTotalSummary.kt` that renders
  the pre-formatted total, or an "unavailable" label when `formattedTotalDistance` is
  `null`. No locale/formatting/business logic inside the composable.
- Wire `TripTotalSummary` into `TripDetailScreen.kt` as a new `LazyColumn` item placed near
  the calculate-route button / footer area.
- Add two new string resources (`trip_detail_total_distance_label`,
  `trip_detail_total_distance_unavailable`) to the base `values/strings.xml` and mirror
  them to `values-it` and `values-es-rES`.
- Add previews for `TripTotalSummary` (value present / unavailable) and update
  `TripDetailScreen` previews so the new `UiState` field has non-default coverage.

Trip-list total display is explicitly **out of scope** (deferred to a future story) — the
trip list has no access to leg data today.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `trip-detail`: `TripDetailUiState` gains a `formattedTotalDistance: String?` field;
  `TripDetailViewModel` gains a new combined stops+legs collector that derives the total
  distance and its completeness; `TripDetailScreen` renders a new `TripTotalSummary`
  composable showing the total or an "unavailable" label.
- `route-display`: the route calculation string resources requirement gains two new keys
  for the total-distance label and its unavailable state.

## Impact

- **Affected code**: `ui/state/TripDetailUiState.kt`, `ui/viewmodel/TripDetailViewModel.kt`,
  `ui/screens/tripdetail/TripTotalSummary.kt` (new), `ui/screens/tripdetail/TripDetailScreen.kt`,
  `res/values/strings.xml`, `res/values-it/strings.xml`, `res/values-es-rES/strings.xml`.
- **Affected tests**: `TripDetailViewModelTest.kt` (new total-distance assertions across
  the combine collector), `TripDetailScreenTest.kt` (androidTest, total displayed /
  unavailable), new isolated Compose test coverage for `TripTotalSummary` if it carries
  conditional rendering.
- **No new dependencies, no database/schema changes, no network/API changes.**
- **Reuses `DistanceFormatter.formatDistance` as-is** — no new formatter is introduced.
