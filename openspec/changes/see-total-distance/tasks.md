## 1. Prerequisites

- [ ] 1.1 Add `formattedTotalDistance: String? = null` field to `TripDetailUiState.kt`, mirroring the existing `formattedLegDistances` / `formattedLegDurations` fields (docblock noting `null` means "route not complete / unavailable")

## 2. ViewModel Total Distance Computation (BDD)

- [ ] 2.1 Write test: GIVEN a trip with exactly `stops.size - 1` legs (a complete route, `stops.size >= 2`) WHEN `observeStopsUseCase` and `observeLegsUseCase` both emit THEN `uiState.formattedTotalDistance` equals `formatDistance` applied to the sum of all `leg.distanceMetres` in `TripDetailViewModelTest`
- [ ] 2.2 Write test: GIVEN two legs with `distanceMetres = 10000` and `distanceMetres = 2500` for a trip with exactly 3 placed stops WHEN observed THEN `uiState.formattedTotalDistance` equals the formatted value for `12500` metres in the active locale in `TripDetailViewModelTest`
- [ ] 2.3 Write test: GIVEN `stops.size >= 2` and no legs exist for the trip WHEN observed THEN `uiState.formattedTotalDistance` is `null` in `TripDetailViewModelTest`
- [ ] 2.4 Write test: GIVEN a leg count that is neither `0` nor `stops.size - 1` (an incomplete/partial leg set) WHEN observed THEN `uiState.formattedTotalDistance` is `null` in `TripDetailViewModelTest`
- [ ] 2.5 Write test: GIVEN fewer than two placed stops WHEN observed THEN `uiState.formattedTotalDistance` is `null` regardless of any legs present in `TripDetailViewModelTest`
- [ ] 2.6 Write test: GIVEN a viewmodel with a complete route (non-null `formattedTotalDistance`) WHEN a stop mutation invalidates all legs (legs flow re-emits an empty list) THEN `uiState.formattedTotalDistance` transitions to `null` in `TripDetailViewModelTest`
- [ ] 2.7 Write test: GIVEN a complete route under an imperial-unit locale (e.g. `en-US`) WHEN observed THEN `uiState.formattedTotalDistance` is expressed in miles, consistent with `formattedLegDistances` for the same legs in `TripDetailViewModelTest`
- [ ] 2.8 Implement: in `TripDetailViewModel.init`, add a new, independent `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId)) { stops, legs -> ... }.onEach { ... }.launchIn(viewModelScope)` collector (alongside, not replacing, the existing stops-only and legs-only collectors) that computes completeness as `legs.size == stops.size - 1 && stops.size >= 2`; when complete, sums `leg.distanceMetres` across `legs` and sets `formattedTotalDistance = formatDistance(sum, Locale.getDefault(), application.resources)`; otherwise sets `formattedTotalDistance = null`. Must make all tests in 2.1-2.7 pass

## 3. TripTotalSummary Composable (BDD)

- [ ] 3.1 Write test: GIVEN a non-null `formattedTotalDistance` WHEN `TripTotalSummary` is rendered THEN the `trip_detail_total_distance_label` text and the formatted value are both displayed in `TripTotalSummaryTest` (androidTest)
- [ ] 3.2 Write test: GIVEN a `null` `formattedTotalDistance` WHEN `TripTotalSummary` is rendered THEN the `trip_detail_total_distance_unavailable` text is displayed and no numeric value is shown in `TripTotalSummaryTest` (androidTest)
- [ ] 3.3 Implement: create `ui/screens/tripdetail/TripTotalSummary.kt` — a stateless `@Composable fun TripTotalSummary(modifier: Modifier = Modifier, formattedTotalDistance: String?)` with private `Dp` layout constants at the file top, rendering the label + value when non-null or the unavailable string when `null`; no locale/formatting/business logic inside the composable. Must make tests in 3.1-3.2 pass

## 4. Integration: Screen Wiring

- [ ] 4.1 Add `trip_detail_total_distance_label` = "Total distance" and `trip_detail_total_distance_unavailable` = "Total distance unavailable" to `app/src/main/res/values/strings.xml`, grouped with the other `trip_detail_leg_*` / route calculation keys
- [ ] 4.2 Mirror both new string keys, translated, to `app/src/main/res/values-it/strings.xml` and `app/src/main/res/values-es-rES/strings.xml`
- [ ] 4.3 Wire `TripTotalSummary(formattedTotalDistance = uiState.formattedTotalDistance)` into `TripDetailScreen.kt` as a new `LazyColumn` item placed after the calculate-route button / route-error item block
- [ ] 4.4 Write test: GIVEN `uiState.formattedTotalDistance` is a non-null value WHEN `TripDetailScreen` is rendered THEN the total distance label and value are displayed in `TripDetailScreenTest` (androidTest)
- [ ] 4.5 Write test: GIVEN `uiState.formattedTotalDistance` is `null` WHEN `TripDetailScreen` is rendered THEN the unavailable text is displayed instead in `TripDetailScreenTest` (androidTest)

## 5. Integration: Preview Coverage

- [ ] 5.1 Add two `TripTotalSummary` previews (`PreviewTripTotalSummaryWithValue`, `PreviewTripTotalSummaryUnavailable`) — value-present and value-absent states — each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`
- [ ] 5.2 Update `PreviewTripDetailScreenWithLegs` in `TripDetailScreen.kt` to pass a non-null `formattedTotalDistance`, satisfying non-default `UiState`-field preview coverage
- [ ] 5.3 Add a new `TripDetailScreen` preview (e.g. `PreviewTripDetailScreenTotalUnavailable`) showing a populated stop/leg set with `formattedTotalDistance = null`, covering the unavailable state

## 6. Verification

- [ ] 6.1 Run `./gradlew test` and confirm `TripDetailViewModelTest` (including all new total-distance cases) passes
- [ ] 6.2 Run `./gradlew connectedDebugAndroidTest` (device/emulator required) and confirm `TripTotalSummaryTest` and `TripDetailScreenTest` pass
- [ ] 6.3 Run `./gradlew check` and confirm no static-analysis regressions, and that coverage on new code is ≥95%
- [ ] 6.4 On-device verification: confirm the total distance appears, matches the sum of leg distances, updates automatically after recalculation, and reads "unavailable" after a stop mutation invalidates the route (until recalculated)
- [ ] 6.5 Re-read the story's acceptance criteria (1-10) against the implemented code and confirm each is satisfied, paying particular attention to AC 4 (unavailable, not zero) and AC 5 (derived only, no new persisted field/network call)
- [ ] 6.6 Update `AGENTS.md` (the real file, not the `CLAUDE.md` symlink) under Epic 3 / Feature 3.2 to record story 3.2.1 as complete
- [ ] 6.7 Update `docs/userstories/index.md` to reflect 3.2.1's completed status
