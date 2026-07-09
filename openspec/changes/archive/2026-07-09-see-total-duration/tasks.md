## 1. Prerequisites

- [x] 1.1 Add `formattedTotalDuration: String? = null` field to `TripDetailUiState.kt`, mirroring the existing `formattedTotalDistance` field (docblock noting it transitions to/from `null` in lockstep with `formattedTotalDistance`, since both share the same completeness check)

## 2. ViewModel Total Duration Computation (BDD)

- [x] 2.1 Write test: GIVEN a trip with exactly `stops.size - 1` legs (a complete route, `stops.size >= 2`) WHEN `observeStopsUseCase` and `observeLegsUseCase` both emit THEN `uiState.formattedTotalDuration` equals `formatDuration` applied to the sum of all `leg.durationSeconds`, in the existing "Section 2: Total Distance" block of `TripDetailViewModelTest`
- [x] 2.2 Write test: GIVEN two legs with `durationSeconds = 600` and `durationSeconds = 900` for a trip with exactly 3 placed stops WHEN observed THEN `uiState.formattedTotalDuration` equals `formatDuration(1500, resources)` in `TripDetailViewModelTest`
- [x] 2.3 Write test: GIVEN `stops.size >= 2` and no legs exist for the trip WHEN observed THEN `uiState.formattedTotalDuration` is `null` in `TripDetailViewModelTest`
- [x] 2.4 Write test: GIVEN a leg count that is neither `0` nor `stops.size - 1` (an incomplete/partial leg set) WHEN observed THEN `uiState.formattedTotalDuration` is `null` in `TripDetailViewModelTest`
- [x] 2.5 Write test: GIVEN fewer than two placed stops WHEN observed THEN `uiState.formattedTotalDuration` is `null` regardless of any legs present in `TripDetailViewModelTest`
- [x] 2.6 Write test: GIVEN a viewmodel with a complete route (non-null `formattedTotalDuration`) WHEN a stop mutation invalidates all legs (legs flow re-emits an empty list) THEN `uiState.formattedTotalDuration` transitions to `null` at the same time as `formattedTotalDistance` in `TripDetailViewModelTest`
- [x] 2.7 Write test: GIVEN a complete route WHEN `formattedTotalDistance` is non-null THEN `formattedTotalDuration` is simultaneously non-null in the same `uiState` emission, and vice versa when both are `null`, in `TripDetailViewModelTest`
- [x] 2.8 Implement: in `TripDetailViewModel.init`, extend the existing `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))` collector (added by 3.2.1) — do NOT add a new/fourth collector. Change the lambda to return `Pair<String?, String?>` (`formattedTotalDistance to formattedTotalDuration`): when complete, additionally sum `leg.durationSeconds` as `Long` (`legs.sumOf { it.durationSeconds.toLong() }`), narrow to `Int`, and format via the existing `formatDuration(totalDurationSeconds, application.resources)`; when not complete, both values are `null`. Destructure the pair in `.onEach { (formattedTotalDistance, formattedTotalDuration) -> ... }` and apply both fields in a single `_uiState.update { it.copy(...) }` call. Must make all tests in 2.1-2.7 pass

## 3. TripTotalSummary Composable Restructuring (BDD)

- [x] 3.1 Write test: GIVEN a non-null `formattedTotalDistance` and non-null `formattedTotalDuration` WHEN `TripTotalSummary` is rendered THEN both the `trip_detail_total_distance_label`/value and `trip_detail_total_duration_label`/value are displayed side by side in `TripTotalSummaryTest` (androidTest)
- [x] 3.2 Write test: GIVEN a `null` `formattedTotalDistance` and a `null` `formattedTotalDuration` WHEN `TripTotalSummary` is rendered THEN the single combined `trip_detail_totals_unavailable` text is displayed and no numeric value or per-metric unavailable text is shown in `TripTotalSummaryTest` (androidTest)
- [x] 3.3 Write test: GIVEN a non-null `formattedTotalDistance` and a `null` `formattedTotalDuration` WHEN `TripTotalSummary` is rendered THEN the distance label/value is displayed alongside the `trip_detail_total_duration_unavailable` text (not the combined message) in `TripTotalSummaryTest` (androidTest)
- [x] 3.4 Write test: GIVEN a non-null `formattedTotalDuration` and a `null` `formattedTotalDistance` WHEN `TripTotalSummary` is rendered THEN the duration label/value is displayed alongside the `trip_detail_total_distance_unavailable` text (not the combined message) in `TripTotalSummaryTest` (androidTest)
- [x] 3.5 Implement: restructure `ui/screens/tripdetail/TripTotalSummary.kt` to accept `formattedTotalDuration: String?` in addition to the existing `formattedTotalDistance: String?`. When both are `null`, render the single `trip_detail_totals_unavailable` text. Otherwise render a `Row` with two `Modifier.weight(1f)` halves (distance left, duration right); each half shows its own label + value when non-null, or falls back to its own metric-specific unavailable text when only that value is `null`. No locale/formatting/business logic inside the composable. Must make tests in 3.1-3.4 pass

## 4. Integration: Screen Wiring

- [x] 4.1 Add `trip_detail_total_duration_label` = "Est. driving time", `trip_detail_total_duration_unavailable` = "Est. driving time unavailable", and `trip_detail_totals_unavailable` = "Trip totals unavailable" to `app/src/main/res/values/strings.xml`, grouped with the other `trip_detail_total_distance_*` keys
- [x] 4.2 Mirror all three new string keys, translated, to `app/src/main/res/values-it/strings.xml` and `app/src/main/res/values-es-rES/strings.xml`
- [x] 4.3 Update the `TripTotalSummary(...)` call site in `TripDetailScreen.kt` to also pass `formattedTotalDuration = uiState.formattedTotalDuration`
- [x] 4.4 Write test: GIVEN `uiState.formattedTotalDistance` and `uiState.formattedTotalDuration` are both non-null WHEN `TripDetailScreen` is rendered THEN both the total distance and total duration labels/values are displayed in `TripDetailScreenTest` (androidTest)
- [x] 4.5 Write test: GIVEN `uiState.formattedTotalDistance` and `uiState.formattedTotalDuration` are both `null` WHEN `TripDetailScreen` is rendered THEN the combined `trip_detail_totals_unavailable` text is displayed instead in `TripDetailScreenTest` (androidTest)
- [x] 4.6 Implement (must make 4.4-4.5 pass): no additional screen-level logic is required beyond 4.3 — `TripDetailScreen` passes both `UiState` fields straight through to `TripTotalSummary`, which already renders both requirements per section 3

## 5. Analytics Follow-Up (non-testable)

- [x] 5.1 Add a `// TODO` code comment at the total-duration computation site in `TripDetailViewModel.kt` (the extended combine collector from task 2.8) noting that a dedicated analytics event for total-duration display should be designed and wired in a follow-up story, rather than added as a side effect of this display-only change (per user clarification; no new `AnalyticsEvent` subtype is introduced by this change)

## 6. Integration: Preview Coverage

- [x] 6.1 Replace the two existing `TripTotalSummary` previews with four previews covering: both totals available (`PreviewTripTotalSummaryBothAvailable`), both unavailable (`PreviewTripTotalSummaryBothUnavailable`), distance available/duration unavailable (`PreviewTripTotalSummaryDistanceOnlyAvailable`), and duration available/distance unavailable (`PreviewTripTotalSummaryDurationOnlyAvailable`) — each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`
- [x] 6.2 Update `PreviewTripDetailScreenWithLegs` in `TripDetailScreen.kt` to also pass a non-null `formattedTotalDuration`, satisfying non-default `UiState`-field preview coverage
- [x] 6.3 Update the existing `TripDetailScreen` preview that sets `formattedTotalDistance = null` (e.g. the preview covering the calculating-route or partial-legs state) to also set `formattedTotalDuration = null`, covering the combined unavailable state

## 7. Verification

- [x] 7.1 Run `./gradlew test` and confirm `TripDetailViewModelTest` (including all new total-duration cases) passes
- [x] 7.2 Run `./gradlew connectedDebugAndroidTest` (device/emulator required) and confirm `TripTotalSummaryTest` and `TripDetailScreenTest` pass
- [x] 7.3 Run `./gradlew check` and confirm no static-analysis regressions, and that coverage on new code is ≥95%
- [x] 7.4 On-device verification: confirm the total driving duration appears alongside the total distance, matches the sum of leg durations, updates automatically after recalculation, and both read the combined "Trip totals unavailable" message after a stop mutation invalidates the route (until recalculated)
- [x] 7.5 Re-read the story's key requirements against the implemented code and confirm each is satisfied, paying particular attention to the "driving time only" label wording and the derived-only, no-new-persisted-field/network-call requirement
- [x] 7.6 Update `AGENTS.md` (the real file, not the `CLAUDE.md` symlink) under Epic 3 / Feature 3.2 to record story 3.2.2 as complete
- [x] 7.7 Update `docs/userstories/index.md` to reflect 3.2.2's completed status
