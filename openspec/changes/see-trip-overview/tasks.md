## 1. Prerequisites

- [ ] 1.1 Add `TripDao.observeById(id: String): Flow<TripWithStopCount?>` Room query to `TripDao.kt`, mirroring `observeAll()`'s SQL shape (same correlated `stopCount` subquery) filtered by `WHERE t.id = :id`
- [ ] 1.2 Add `tripName: String? = null` and `isRouteRecalculationPromptVisible: Boolean = false` fields (with docblocks explaining the fallback/lockstep semantics from the design) to `TripDetailUiState.kt`

## 2. Repository — Observe Single Trip (BDD)

- [ ] 2.1 Write test: GIVEN `TripDao.observeById` emits a `TripWithStopCount` for a trip ID WHEN `TripRepositoryImpl.observeTripById(tripId)` is called THEN a mapped domain `Trip` preserving `name`, `createdAt`, `updatedAt`, and `stopCount` is emitted, in `TripRepositoryImplTest`
- [ ] 2.2 Write test: GIVEN `TripDao.observeById` emits `null` for a trip ID that does not exist WHEN `TripRepositoryImpl.observeTripById(tripId)` is called THEN `null` is emitted, in `TripRepositoryImplTest`
- [ ] 2.3 Implement: add `fun observeTripById(tripId: String): Flow<Trip?>` to the `TripRepository` interface; implement in `TripRepositoryImpl` via `tripDao.observeById(tripId).map { it?.toDomain() }`, reusing the existing `TripWithStopCount.toDomain()` mapper. Must make 2.1-2.2 pass

## 3. Use Case — ObserveTripUseCase (BDD)

- [ ] 3.1 Write test: GIVEN a `tripId` WHEN `ObserveTripUseCase.invoke(tripId)` is called THEN it returns `TripRepository.observeTripById(tripId)`'s `Flow` unchanged, in a new `ObserveTripUseCaseTest.kt`
- [ ] 3.2 Implement: create `domain/usecase/ObserveTripUseCase.kt`, a thin `operator fun invoke(tripId: String): Flow<Trip?>` pass-through mirroring `ObserveStopsUseCase`. Must make 3.1 pass

## 4. ViewModel — Trip Name Observation (BDD)

- [ ] 4.1 Write test: GIVEN `ObserveTripUseCase(tripId)` emits a `Trip` with `name = "Summer Roadtrip"` WHEN observed THEN `uiState.tripName` becomes `"Summer Roadtrip"`, in `TripDetailViewModelTest`
- [ ] 4.2 Write test: GIVEN the ViewModel has just initialised and `ObserveTripUseCase(tripId)` has not yet emitted a non-null `Trip` WHEN `uiState` is read THEN `tripName` remains `null`, in `TripDetailViewModelTest`
- [ ] 4.3 Write test: GIVEN `ObserveTripUseCase(tripId)` emits a new `Trip` WHEN observed THEN only `tripName` changes in `uiState`; `startingPoint`, `destination`, `intermediateStops`, `legs`, `formattedTotalDistance`, `formattedTotalDuration`, and `isRouteRecalculationPromptVisible` are unaffected, in `TripDetailViewModelTest`
- [ ] 4.4 Implement: add an `observeTripUseCase: ObserveTripUseCase` constructor parameter to `TripDetailViewModel`; add an independent `.onEach` collector in `init` (separate from the stops-only, legs-only, and combined stops+legs collectors) that updates `tripName` from the emitted `Trip?.name`, leaving `tripName` at its current value when the emission is `null`; extend the `createViewModel(...)` factory in `TripDetailViewModelTest` with an `observeTripUseCase` mock (default emitting `flowOf(null)`) and update the `TripDetailViewModel(...)` constructor call to pass it. Must make 4.1-4.3 pass without breaking any existing test

## 5. ViewModel — Route Recalculation Prompt Visibility (BDD)

- [ ] 5.1 Write test: GIVEN `stops.size >= 2` and no legs exist for the trip (route never calculated) WHEN observed THEN `uiState.isRouteRecalculationPromptVisible` is `true`, in `TripDetailViewModelTest`
- [ ] 5.2 Write test: GIVEN `stops.size >= 2` and `legs.size` is neither `0` nor `stops.size - 1` (a stale/incomplete leg set) WHEN observed THEN `uiState.isRouteRecalculationPromptVisible` is `true`, in `TripDetailViewModelTest`
- [ ] 5.3 Write test: GIVEN `legs.size == stops.size - 1` and `stops.size >= 2` (a complete route) WHEN observed THEN `uiState.isRouteRecalculationPromptVisible` is `false`, in `TripDetailViewModelTest`
- [ ] 5.4 Write test: GIVEN `stops.size < 2` WHEN observed THEN `uiState.isRouteRecalculationPromptVisible` is `false`, regardless of any legs present, in `TripDetailViewModelTest`
- [ ] 5.5 Write test: GIVEN `uiState.isRouteRecalculationPromptVisible` is `false` (route complete) WHEN a stop mutation invalidates all legs for the trip, leaving `stops.size >= 2` THEN `uiState.isRouteRecalculationPromptVisible` transitions to `true` at the same time `formattedTotalDistance` / `formattedTotalDuration` transition to `null`, in `TripDetailViewModelTest`
- [ ] 5.6 Implement: extend the existing `combine(observeStopsUseCase(tripId), observeLegsUseCase(tripId))` collector added by 3.2.1/3.2.2 — replace its `Pair<String?, String?>` return type with a private, file-local data class (e.g. `TripTotalsAndPromptState`) carrying `formattedTotalDistance`, `formattedTotalDuration`, and `isRouteRecalculationPromptVisible`; when the route is complete set the prompt flag `false`; when incomplete set it to `stops.size >= 2`; update the `.onEach` destructuring and the single `_uiState.update { it.copy(...) }` call accordingly. Must make 5.1-5.5 pass without regressing the existing total-distance/total-duration assertions on the same collector

## 6. RouteRecalculationPrompt Composable (BDD)

- [ ] 6.1 Write test: GIVEN `isEnabled = true` WHEN `RouteRecalculationPrompt` is rendered THEN both the `trip_detail_recalculation_prompt_message` and `trip_detail_recalculation_prompt_action` texts are displayed, in a new `RouteRecalculationPromptTest.kt` (androidTest)
- [ ] 6.2 Write test: GIVEN `isEnabled = true` WHEN the user taps `RouteRecalculationPrompt` THEN the `onClick` lambda is invoked, in `RouteRecalculationPromptTest` (androidTest)
- [ ] 6.3 Write test: GIVEN `isEnabled = false` WHEN the user taps `RouteRecalculationPrompt` THEN the `onClick` lambda is NOT invoked, in `RouteRecalculationPromptTest` (androidTest)
- [ ] 6.4 Implement: create `ui/screens/tripdetail/RouteRecalculationPrompt.kt` — a stateless composable accepting `modifier: Modifier = Modifier`, `isEnabled: Boolean`, and `onClick: () -> Unit = {}`; render a tappable container (`Card` or clickable `Row`) showing both string resources, clickable only when `isEnabled` is `true`; no completeness/stop-count logic inside the composable. Must make 6.1-6.3 pass

## 7. TripDetailScreen — Top Bar Trip Name (BDD)

- [ ] 7.1 Write test: GIVEN `uiState.tripName = "Summer Roadtrip"` WHEN `TripDetailScreen` is rendered THEN the `TopAppBar` displays "Summer Roadtrip", in `TripDetailScreenTest` (androidTest)
- [ ] 7.2 Write test: GIVEN `uiState.tripName = null` WHEN `TripDetailScreen` is rendered THEN the `TopAppBar` displays the `trip_detail_title` fallback text ("Trip Detail"), in `TripDetailScreenTest` (androidTest)
- [ ] 7.3 Implement: update the `TopAppBar`'s title in `TripDetailScreen.kt` to `Text(uiState.tripName ?: stringResource(R.string.trip_detail_title))`. Must make 7.1-7.2 pass

## 8. TripDetailScreen — Recalculation Prompt Wiring (BDD)

- [ ] 8.1 Write test: GIVEN `uiState.isRouteRecalculationPromptVisible = true` WHEN `TripDetailScreen` is rendered THEN `RouteRecalculationPrompt` is displayed, in `TripDetailScreenTest` (androidTest)
- [ ] 8.2 Write test: GIVEN `uiState.isRouteRecalculationPromptVisible = false` WHEN `TripDetailScreen` is rendered THEN `RouteRecalculationPrompt` is NOT displayed, in `TripDetailScreenTest` (androidTest)
- [ ] 8.3 Write test: GIVEN `uiState.isRouteRecalculationPromptVisible = true`, `isCalculatingRoute = false`, and `isLoading = false` WHEN the user taps the prompt THEN `TripDetailUiIntent.OnCalculateRouteClicked` is dispatched via `onIntent`, in `TripDetailScreenTest` (androidTest)
- [ ] 8.4 Write test: GIVEN `uiState.isRouteRecalculationPromptVisible = true` and `uiState.isLoading = true` WHEN `TripDetailScreen` is rendered THEN `RouteRecalculationPrompt` is rendered in its disabled state, in `TripDetailScreenTest` (androidTest)
- [ ] 8.5 Implement: add a `RouteRecalculationPrompt` `LazyColumn` item in `TripDetailScreen.kt`, placed immediately before the `TripTotalSummary` item, gated on `uiState.isRouteRecalculationPromptVisible`, passing `isEnabled = !isCalculating && !uiState.isLoading` (reusing the existing "Calculate route" button's enablement rule) and `onClick = { onIntent(TripDetailUiIntent.OnCalculateRouteClicked) }`. Must make 8.1-8.4 pass

## 9. Integration: Strings

- [ ] 9.1 Add `trip_detail_recalculation_prompt_message` = "Route not calculated" and `trip_detail_recalculation_prompt_action` = "Tap to calculate" to `app/src/main/res/values/strings.xml`, grouped with the other route-display keys
- [ ] 9.2 Mirror both new string keys, translated, to `app/src/main/res/values-it/strings.xml` and `app/src/main/res/values-es-rES/strings.xml`

## 10. Integration: Preview Coverage

- [ ] 10.1 Add two previews to `RouteRecalculationPrompt.kt`: `PreviewRouteRecalculationPromptEnabled` and `PreviewRouteRecalculationPromptDisabled`, each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`
- [ ] 10.2 Update `PreviewTripDetailScreenWithLegs` in `TripDetailScreen.kt` to pass a non-null `tripName` (e.g. `"Rome to Barcelona"`), satisfying non-default `UiState`-field preview coverage for `tripName`
- [ ] 10.3 Update `PreviewTripDetailScreenTotalUnavailable` in `TripDetailScreen.kt` to also set `isRouteRecalculationPromptVisible = true`, satisfying non-default `UiState`-field preview coverage for the prompt flag and showing it alongside the unavailable totals it explains

## 11. Verification

- [ ] 11.1 Run `./gradlew test` and confirm `TripRepositoryImplTest`, `ObserveTripUseCaseTest`, and `TripDetailViewModelTest` (including all new trip-name and prompt-visibility cases) pass
- [ ] 11.2 Run `./gradlew connectedDebugAndroidTest` (device/emulator required) and confirm `RouteRecalculationPromptTest` and `TripDetailScreenTest` pass
- [ ] 11.3 Run `./gradlew check` and confirm no static-analysis regressions, and that coverage on new code is ≥95%
- [ ] 11.4 On-device verification: confirm the top bar shows the real trip name (not "Trip Detail") once the trip loads; confirm the recalculation prompt appears for a trip with 2+ stops and no calculated route, disappears once the route is successfully calculated, and reappears after a stop mutation invalidates the route
- [ ] 11.5 Re-read the story's key requirements against the implemented code, paying particular attention to: the "Trip Detail" fallback while `tripName` is loading, the single-generic-prompt requirement (no distinction between never-calculated and stale), the 2+-stops visibility guard matching the "Calculate route" button, and that the prompt reuses `OnCalculateRouteClicked` with no new intent introduced
- [ ] 11.6 Update `AGENTS.md` (the real file, not the `CLAUDE.md` symlink) under Epic 4 / Feature 4.1 to record story 4.1.1 as complete
- [ ] 11.7 Update `docs/userstories/index.md` to reflect 4.1.1's completed status
