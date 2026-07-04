## 1. Prerequisites

- [x] 1.1 Add `formattedLegDurations: Map<String, String> = emptyMap()` field to `TripDetailUiState.kt`, mirroring the existing `formattedLegDistances` field

## 2. Duration Formatting Utility (BDD)

- [x] 2.1 Write test: GIVEN durationSeconds of 0, 45, and 89 (all below or crossing the one-minute boundary) WHEN formatDuration is called THEN it returns "0 min", "0 min", and "1 min" respectively (truncation, not rounding) in DurationFormatterTest
- [x] 2.2 Write test: GIVEN durationSeconds of 540 (nine minutes, sub-hour) WHEN formatDuration is called THEN it returns "9 min" in DurationFormatterTest
- [x] 2.3 Write test: GIVEN durationSeconds of 3599 (fifty-nine minutes, just below the hour boundary) WHEN formatDuration is called THEN it returns "59 min" in DurationFormatterTest
- [x] 2.4 Write test: GIVEN durationSeconds of 3600 (exactly one hour) WHEN formatDuration is called THEN it returns "1h 0min" in DurationFormatterTest
- [x] 2.5 Write test: GIVEN durationSeconds of 5400 (one hour thirty minutes) WHEN formatDuration is called THEN it returns "1h 30min" in DurationFormatterTest
- [x] 2.6 Implement: create `ui/util/DurationFormatter.kt` with `fun formatDuration(durationSeconds: Int, resources: Resources): String` — truncate `durationSeconds / SECONDS_PER_MINUTE` to whole minutes first, then branch to `trip_detail_leg_duration_hours_minutes` when minutes >= `MINUTES_PER_HOUR` else `trip_detail_leg_duration_minutes`; move the `SECONDS_PER_MINUTE`/`MINUTES_PER_HOUR` constants here as private top-level constants; no `Locale` parameter. Must make all tests in 2.1-2.5 pass

## 3. ViewModel Duration Wiring (BDD)

- [x] 3.1 Add duration string-resource mocks (`trip_detail_leg_duration_minutes`, `trip_detail_leg_duration_hours_minutes`) to `TripDetailViewModelTest.setUp()`, matching the existing distance-resource mock pattern, so subsequent tests emitting non-empty legs do not hit unmocked resources
- [x] 3.2 Write test: GIVEN a trip with legs of varying durationSeconds WHEN observeLegsUseCase emits those legs THEN uiState.formattedLegDurations contains one formatted entry per leg keyed by fromStopId in TripDetailViewModelTest
- [x] 3.3 Implement: add `private fun buildFormattedDurations(legs: List<Leg>): Map<String, String>` to `TripDetailViewModel.kt`, mirroring `buildFormattedDistances` (`legs.associate { leg -> leg.fromStopId to formatDuration(leg.durationSeconds, application.resources) }`), and wire it into the existing `observeLegsUseCase(tripId).onEach { legs -> ... }` block so `formattedLegDurations` is updated alongside `formattedLegDistances`

## 4. LegSummary Composable Duration Display (BDD)

- [x] 4.1 Write test: GIVEN a Leg and a pre-computed formattedDuration string WHEN LegSummary is rendered THEN the formattedDuration text is displayed alongside formattedDistance in LegSummaryTest (androidTest)
- [x] 4.2 Implement: add `formattedDuration: String` parameter to `LegSummary`, render `"$formattedDistance · $formattedDuration"` directly, and delete the private `@Composable formatDuration` function plus the `SECONDS_PER_MINUTE`/`MINUTES_PER_HOUR` constants from `LegSummary.kt` (now redundant with `DurationFormatter.kt`)
- [x] 4.3 Update `LegSummaryTest.setContent(...)` (androidTest) to accept and pass `formattedDuration` explicitly, updating both existing tests (`legSummary_withShortDistance_showsDistanceTextBetweenStops`, `legSummary_exposesNoClickableSemantics`) to supply it

## 5. Integration: Screen Wiring

- [x] 5.1 Update both `LegSummary` call sites in `TripDetailScreen.kt` to pass `formattedDuration = uiState.formattedLegDurations[stop.id] ?: ""`
- [x] 5.2 Update the relevant `TripDetailScreen.kt` preview (e.g. `PreviewTripDetailScreenWithLegs`) to pass a non-empty `formattedLegDurations` map, satisfying UiState preview coverage for the new field

## 6. Integration: LegSummary Preview Coverage

- [x] 6.1 Update all three `LegSummary` previews (`PreviewLegSummaryShortDistance`, `PreviewLegSummaryLongDistance`, `PreviewLegSummaryImperialLocale`) to pass an explicit `formattedDuration` string literal, since the composable no longer performs internal formatting

## 7. Verification

- [x] 7.1 Run `./gradlew test` and confirm `DurationFormatterTest` and `TripDetailViewModelTest` pass
- [x] 7.2 Run `./gradlew connectedDebugAndroidTest` (device/emulator required) and confirm `LegSummaryTest` passes
- [x] 7.3 Run `./gradlew check` and confirm no static-analysis regressions (unused imports, unused constants) after removing internal formatting from `LegSummary.kt`
- [x] 7.4 Re-read the story's acceptance criteria (1-8) against the implemented code and confirm each is satisfied, paying particular attention to AC 5 (truncation) and AC 7 (no Locale parameter)
