## Why

`LegSummary` currently computes its duration text internally via a private `@Composable formatDuration` function that reads string resources directly. This violates the project's "composables are purely presentational" guideline (already applied to distance formatting via `DistanceFormatter.kt` in story 3.1.2) and leaves duration formatting untestable as a pure unit. User story 3.1.3 requires the duration to be pre-computed and testable, matching the distance pattern exactly.

## What Changes

- Add `ui/util/DurationFormatter.kt` with a top-level `formatDuration(durationSeconds: Int, resources: Resources): String` function — no `Locale` parameter, since time units are universal.
- Remove the private `@Composable formatDuration` function and the `SECONDS_PER_MINUTE` / `MINUTES_PER_HOUR` constants from `LegSummary.kt`.
- Change `LegSummary` to accept a new `formattedDuration: String` parameter, rendered as-is (no formatting logic inside the composable).
- Add `TripDetailUiState.formattedLegDurations: Map<String, String>` (keyed by `fromStopId`), mirroring `formattedLegDistances`.
- Add `TripDetailViewModel.buildFormattedDurations(legs: List<Leg>): Map<String, String>`, wired into the existing `observeLegsUseCase(tripId).onEach { ... }` pipeline alongside `buildFormattedDistances`.
- Update both `LegSummary` call sites in `TripDetailScreen.kt` to pass `formattedDuration = uiState.formattedLegDurations[stop.id] ?: ""`, and update the screen preview.
- Update `LegSummary` previews and the `LegSummaryTest` (androidTest) to pass `formattedDuration` explicitly instead of relying on internal formatting.

No user-visible behavioural change: durations are truncated to whole minutes (not rounded), matching the existing composable's arithmetic — only the code's location and testability change.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `route-display`: `LegSummary` no longer performs duration formatting internally; duration formatting moves to a new testable `ui/util/DurationFormatter.kt` utility, computed in `TripDetailViewModel` and passed into the composable as a pre-computed string, mirroring the existing distance-formatting pattern. The spec's illustrative scenarios describing duration formatting as an implicit composable behaviour are replaced with scenarios describing `formatDuration` as a standalone, unit-testable function.

## Impact

- **Affected code**: `ui/util/DurationFormatter.kt` (new), `ui/screens/tripdetail/LegSummary.kt`, `ui/screens/tripdetail/TripDetailScreen.kt`, `ui/state/TripDetailUiState.kt`, `ui/viewmodel/TripDetailViewModel.kt`.
- **Affected tests**: new `ui/util/DurationFormatterTest.kt`; `TripDetailViewModelTest.kt` (add duration string-resource mocks and formatted-duration assertions); `LegSummaryTest.kt` (androidTest, update call sites).
- **No new dependencies, no database/schema changes, no API changes.**
- **String resources**: none new — `trip_detail_leg_duration_minutes` and `trip_detail_leg_duration_hours_minutes` already exist in all three locales (`values`, `values-it`, `values-es-rES`).
