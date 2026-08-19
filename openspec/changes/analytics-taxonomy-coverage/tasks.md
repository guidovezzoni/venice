## 1. Prerequisites: Flat Vocabulary Types

No test pairs — these are plain enums with no mapping/classification logic (per
`guidelines-analytics.md`, "a flat vocabulary with no logic needs no such tests"). Each is exercised
indirectly by the `AnalyticsEvent` construction tests in section 6.

- [x] 1.1 Create `core/analytics/AnalyticsScreen.kt`: enum with `TRIP_LIST("trip_list")`,
  `TRIP_DETAIL("trip_detail")`
- [x] 1.2 Create `core/analytics/StopTypeParam.kt`: enum with `STARTING_POINT("starting_point")`,
  `DESTINATION("destination")`, `INTERMEDIATE("intermediate")`
- [x] 1.3 Create `core/analytics/AnalyticsErrorType.kt`: enum with `NETWORK("network")`,
  `TIMEOUT("timeout")`, `NOT_FOUND("not_found")`, `PERMISSION_DENIED("permission_denied")`,
  `QUOTA_EXCEEDED("quota_exceeded")`, `PERSISTENCE("persistence")`, `UNKNOWN("unknown")` — with a code
  comment noting `NOT_FOUND`/`PERMISSION_DENIED`/`QUOTA_EXCEEDED` are reserved vocabulary, unreachable
  from `AnalyticsErrorClassifier` until finer-grained classification is implemented in a follow-up
- [x] 1.4 Extend `core/analytics/AnalyticsOperation.kt`: add `UNDO_MARK_DEPARTED("undo_mark_departed")`,
  `SEARCH_PLACE("search_place")`, `RESOLVE_PLACE("resolve_place")` to the existing 7 values

## 2. Distance Banding (BDD)

- [x] 2.1 Write test: GIVEN a distance of 0m, 49,999m, 50,000m, 199,999m, 200,000m, 999,999m,
  1,000,000m, and 2,000,000m WHEN the mapping function classifies each THEN it returns
  `UNDER_50KM`, `UNDER_50KM`, `RANGE_50_200KM`, `RANGE_50_200KM`, `RANGE_200_500KM`,
  `RANGE_500_1000KM`, `OVER_1000KM`, `OVER_1000KM` respectively (lower-bound-inclusive boundaries at
  50/200/500/1000km), in `DistanceBandTest`
- [x] 2.2 Implement: `core/analytics/DistanceBand.kt` — enum with 5 constants each carrying
  `value: String`, plus a mapping function `fun DistanceBand.Companion.fromMetres(metres: Int):
  DistanceBand` (or top-level function) per design decision 1

## 3. Duration Banding (BDD)

- [x] 3.1 Write test: GIVEN a duration of 0s, 3,599s, 3,600s, 10,799s, 10,800s, 21,599s, 21,600s,
  43,199s, 43,200s, and 100,000s WHEN the mapping function classifies each THEN it returns the
  correct band at every boundary (lower-bound-inclusive boundaries at 1/3/6/12 hours), in
  `DurationBandTest`
- [x] 3.2 Implement: `core/analytics/DurationBand.kt` — enum with 5 constants each carrying
  `value: String`, plus a mapping function classifying seconds into a band per design decision 1

## 4. Trip Count Banding (BDD)

- [x] 4.1 Write test: GIVEN a trip count of 0, 1, 2, 5, and 6 WHEN the mapping function classifies
  each THEN it returns `ZERO`, `ONE`, `RANGE_2_5`, `RANGE_2_5`, `SIX_PLUS` respectively, in
  `CountBandTest`
- [x] 4.2 Implement: `core/analytics/CountBand.kt` — enum with 4 constants each carrying
  `value: String`, plus a mapping function classifying a non-negative count into a band

## 5. Error Classification (BDD)

- [x] 5.1 Write test: GIVEN a `SocketTimeoutException`, a plain `IOException`, a
  `java.net.UnknownHostException` (an `IOException` subtype other than the timeout case), an
  `android.database.sqlite.SQLiteException`, and an `IllegalStateException` (the type
  `CalculateRouteUseCase`/`MarkStopDepartedUseCase` actually throw for precondition failures) WHEN
  `AnalyticsErrorClassifier` classifies each THEN it returns `TIMEOUT`, `NETWORK`, `NETWORK`,
  `PERSISTENCE`, `UNKNOWN` respectively, in `AnalyticsErrorClassifierTest`
- [x] 5.2 Implement: `core/analytics/AnalyticsErrorClassifier.kt` — top-level function
  `(Throwable) -> AnalyticsErrorType`, checking `SocketTimeoutException` before the general
  `IOException` branch, per design decision 2

## 6. Event Taxonomy Rewrite (BDD)

- [x] 6.1 Write test: GIVEN each of the 14 `AnalyticsEvent` subtypes (`TripCreated`, `TripOpened`,
  `StopAdded`, `RouteCalculated`, `NavigationLaunched`, `StopEdited`, `StopRemoved`,
  `StopReordered`, `StopDeparted`, `StopDepartureUndone`, `PlaceSearchPerformed`,
  `PlaceSuggestionSelected`, `OperationFailed`, `ScreenViewed`) constructed with representative
  parameter values WHEN `name` and `properties` are inspected THEN they match
  `docs/analytics/tracking-plan.md`'s event dictionary exactly, and every enum-typed parameter's
  value in `properties` equals `.value` and never `.name` (assert no `SCREAMING_SNAKE_CASE` string
  appears in any properties map), rewriting `AnalyticsEventTest.kt` in full
- [x] 6.2 Implement: rewrite `core/analytics/AnalyticsEvent.kt` — replace all 10 legacy subtypes with
  the 14 target subtypes per the `analytics-taxonomy` spec, each built from the new enum types from
  sections 1–5 and emitting `.value` for every enum-typed parameter

## 7. User Properties (BDD)

- [x] 7.1 Write test: GIVEN `AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)` and
  `AnalyticsUserProperty.DistanceUnit(unit = "imperial")` constructed WHEN inspected THEN each holds
  its typed/bounded value and no parameter name collides with any `AnalyticsEvent` parameter name, in
  `AnalyticsUserPropertyTest.kt`
- [x] 7.2 Implement: rewrite `core/analytics/AnalyticsUserProperty.kt` — remove the `ForTesting`
  placeholder, add `TripCountBand(band: CountBand)` and `DistanceUnit(unit: String)`

## 8. Debug Log Formatter Regression Guard (BDD)

- [x] 8.1 Write test: GIVEN one of the new `AnalyticsEvent` subtypes and one of the new
  `AnalyticsUserProperty` subtypes WHEN `formatEventLog`/`formatUserPropertyLog` format them THEN
  each produces a distinguishable log line containing the event/property's data, added as new cases
  in `AnalyticsLogFormatterTest.kt`
- [x] 8.2 Implement: confirm `AnalyticsLogFormatter.kt` needs no code change (it operates generically
  on `event.name`/`event.properties`/`toString()`, per design decision 7); adjust only if the test
  reveals a gap

## 9. `CalculateRouteUseCase` Returns Calculated Legs (BDD)

- [x] 9.1 Write test: GIVEN `directionsApiService.fetchRoute` succeeds and legs are persisted WHEN
  `RouteRepositoryImpl.calculateRoute` is called THEN it returns `Result.success` wrapping the same
  `List<Leg>` that was persisted (update `RouteRepositoryImplTest.kt`); AND GIVEN
  `routeRepository.calculateRoute` returns `Result.success(legs)` WHEN `CalculateRouteUseCase` is
  invoked with 2+ stops THEN it returns `Result.success(legs)` unchanged (update
  `CalculateRouteUseCaseTest.kt`)
- [x] 9.2 Implement: change `RouteRepository.calculateRoute` and `RouteRepositoryImpl.calculateRoute`
  return type to `Result<List<Leg>>` (returning the in-memory `legs` list already built before
  persistence — no extra DB read); change `CalculateRouteUseCase.invoke` return type to
  `Result<List<Leg>>`, forwarding the repository's result, per design decision 3

## 10. `MarkStopDepartedUseCase` Returns the Departed Stop (BDD)

- [x] 10.1 Write test: GIVEN the current pending stop at `order = 1` WHEN
  `MarkStopDepartedUseCase` succeeds THEN it returns `Result.success` wrapping that `Stop` with
  `status = VISITED` and `order = 1` unchanged, in `MarkStopDepartedUseCaseTest.kt`
- [x] 10.2 Implement: change `MarkStopDepartedUseCase.invoke` return type to `Result<Stop>`; on
  success, return `currentStop.copy(status = StopStatus.VISITED)` — no repository read-back — per
  design decision 3

## 11. `UndoMarkStopDepartedUseCase` Returns the Reverted Stop (BDD)

- [x] 11.1 Write test: GIVEN the most recently departed stop at `order = 1` WHEN
  `UndoMarkStopDepartedUseCase` succeeds THEN it returns `Result.success` wrapping that `Stop` with
  `status = PENDING` and `order = 1` unchanged, in `UndoMarkStopDepartedUseCaseTest.kt`
- [x] 11.2 Implement: change `UndoMarkStopDepartedUseCase.invoke` return type to `Result<Stop>`; on
  success, return `lastDeparted.copy(status = StopStatus.PENDING)` — no repository read-back — per
  design decision 3

## 12. TripListViewModel — Screen View and Trip Count Band on Load (BDD)

- [x] 12.1 Write test: GIVEN `TripListViewModel` is constructed THEN `ScreenViewed(AnalyticsScreen
  .TRIP_LIST)` is logged; AND GIVEN the observed trip list first emits 3 trips THEN
  `setUserProperty(TripCountBand(band = CountBand.RANGE_2_5))` is called, in `TripListViewModelTest.kt`
- [x] 12.2 Implement: update `TripListViewModel.init` to log `ScreenViewed(AnalyticsScreen
  .TRIP_LIST)` and to call `setUserProperty(TripCountBand(...))` from the trip-list observer using the
  `CountBand` mapping function

## 13. TripListViewModel — Trip Created and User Property Update (BDD)

- [x] 13.1 Write test: GIVEN the user had 1 trip before creating a new one WHEN `createTrip()`
  succeeds THEN `TripCreated(isFirstTrip = false)` is logged and `setUserProperty(TripCountBand(band =
  CountBand.RANGE_2_5))` is called; AND GIVEN the user had 0 trips before creating THEN
  `TripCreated(isFirstTrip = true)` is logged, in `TripListViewModelTest.kt`
- [x] 13.2 Implement: update `createTrip()`'s success branch to compute `isFirstTrip` from the
  pre-creation trip count, log `TripCreated(isFirstTrip)`, and call `setUserProperty(TripCountBand
  (...))` with the post-creation count

## 14. TripListViewModel — Trip Creation Failure Dual-Channel (BDD)

- [x] 14.1 Write test: GIVEN `createTripUseCase` fails with an `IOException` WHEN the failure branch
  runs THEN `OperationFailed(operation = CREATE_TRIP, errorType = NETWORK)` is logged and
  `trackException(throwable, CREATE_TRIP)` is called, in `TripListViewModelTest.kt`
- [x] 14.2 Implement: update `createTrip()`'s failure branch to classify the error via
  `AnalyticsErrorClassifier`, log `OperationFailed(CREATE_TRIP, errorType)`, and call
  `trackException(error, AnalyticsOperation.CREATE_TRIP)`

## 15. TripListViewModel — Trip Opened Removal (BDD)

- [x] 15.1 Write test: GIVEN `onIntent(OnTripClicked(tripId))` is processed WHEN analytics calls are
  inspected THEN no `TripOpened` event is logged by `TripListViewModel`, in `TripListViewModelTest.kt`
- [x] 15.2 Implement: remove the `analyticsClient.logEvent(AnalyticsEvent.TripOpened(...))` call from
  `onIntent(OnTripClicked)`

## 16. TripDetailViewModel — Screen View and Trip Opened on Init (BDD)

- [x] 16.1 Write test: GIVEN `TripDetailViewModel` is constructed THEN `ScreenViewed(AnalyticsScreen
  .TRIP_DETAIL)` is logged; AND GIVEN the stops/legs flows emit an initial complete route (2 stops, 1
  leg) and then emit again after a later mutation THEN `TripOpened(stopCount = 2, routeState =
  "complete")` is logged exactly once, on the first combined emission, in
  `TripDetailViewModelTest.kt`
- [x] 16.2 Implement: update `TripDetailViewModel.init` to log `ScreenViewed(AnalyticsScreen
  .TRIP_DETAIL)`, and add a separate `combine(observeStopsUseCase(tripId),
  observeLegsUseCase(tripId))` piped through `.take(1)` that logs `TripOpened(stopCount, routeState)`
  once, per design decision 4 — kept distinct from the existing continuously-updating totals `combine`
  block

## 17. TripDetailViewModel — Stop Added (BDD)

- [x] 17.1 Write test: GIVEN `uiState` reflects 2 stops before a starting point is set WHEN
  `setStop(..., StopType.STARTING_POINT)` succeeds THEN `StopAdded(stopType =
  StopTypeParam.STARTING_POINT, stopCount = 3)` is logged, in `TripDetailViewModelTest.kt`
- [x] 17.2 Implement: update `setStop()`'s success branch to log `StopAdded(stopType.toParam(),
  currentStopCount() + 1)` using a `StopType -> StopTypeParam` mapping and the pre-mutation stop-count
  helper from design decision 3

## 18. TripDetailViewModel — Stop Added Failure (BDD)

- [x] 18.1 Write test: GIVEN `setStopUseCase` fails with a persistence error WHEN the failure branch
  runs THEN `OperationFailed(operation = SET_STOP, errorType = PERSISTENCE)` is logged and
  `trackException(throwable, SET_STOP)` is called, in `TripDetailViewModelTest.kt`
- [x] 18.2 Implement: update `setStop()`'s failure branch to classify via `AnalyticsErrorClassifier`
  and call both `logEvent(OperationFailed(...))` and `trackException(...)`

## 19. TripDetailViewModel — Stop Edited (BDD)

- [x] 19.1 Write test: GIVEN an intermediate stop is edited WHEN `editStop(...)` succeeds THEN
  `StopEdited(stopType = StopTypeParam.INTERMEDIATE)` is logged, in `TripDetailViewModelTest.kt`
- [x] 19.2 Implement: update `editStop()`'s success branch to log `StopEdited(stopType.toParam())` —
  sourcing the edited stop's type from the stop being edited

## 20. TripDetailViewModel — Stop Edited Failure (BDD)

- [x] 20.1 Write test: GIVEN `editStopUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = EDIT_STOP, errorType)` is logged and `trackException(throwable,
  EDIT_STOP)` is called, in `TripDetailViewModelTest.kt`
- [x] 20.2 Implement: update `editStop()`'s failure branch for dual-channel emission

## 21. TripDetailViewModel — Stop Removed (BDD)

- [x] 21.1 Write test: GIVEN `uiState` reflects 3 stops including the one being removed WHEN
  `removeStop()` succeeds THEN `StopRemoved(stopType, stopCount = 2)` is logged, in
  `TripDetailViewModelTest.kt`
- [x] 21.2 Implement: update `removeStop()`'s success branch to log `StopRemoved(stopType.toParam(),
  currentStopCount() - 1)` per design decision 3

## 22. TripDetailViewModel — Stop Removed Failure (BDD)

- [x] 22.1 Write test: GIVEN `removeStopUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = REMOVE_STOP, errorType)` is logged and `trackException(throwable,
  REMOVE_STOP)` is called, in `TripDetailViewModelTest.kt`
- [x] 22.2 Implement: update `removeStop()`'s failure branch for dual-channel emission

## 23. TripDetailViewModel — Stop Reordered (BDD)

- [x] 23.1 Write test: GIVEN `moveStop(currentOrder, currentOrder - 1)` (an up-move) succeeds THEN
  `StopReordered(direction = "up", stopCount)` is logged; AND GIVEN a down-move succeeds THEN
  `direction = "down"`, in `TripDetailViewModelTest.kt`
- [x] 23.2 Implement: update `moveStop()`'s success branch to log `StopReordered(direction,
  currentStopCount())`, deriving `direction` from whether `toOrder < fromOrder`

## 24. TripDetailViewModel — Stop Reordered Failure (BDD)

- [x] 24.1 Write test: GIVEN `moveStopUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = MOVE_STOP, errorType)` is logged and `trackException(throwable,
  MOVE_STOP)` is called, in `TripDetailViewModelTest.kt`
- [x] 24.2 Implement: update `moveStop()`'s failure branch for dual-channel emission

## 25. TripDetailViewModel — Stop Departed (BDD)

- [x] 25.1 Write test: GIVEN `markStopDepartedUseCase` succeeds and returns a `Stop` with `order = 1`
  WHEN `markStopDeparted(stopId)` completes THEN `StopDeparted(stopPosition = 1, stopCount)` is
  logged, in `TripDetailViewModelTest.kt`
- [x] 25.2 Implement: update `markStopDeparted()`'s success branch to source `stopPosition` from the
  returned `Stop.order` (per section 10's new return type) and log `StopDeparted(stopPosition,
  currentStopCount())`

## 26. TripDetailViewModel — Stop Departed Failure (BDD)

- [x] 26.1 Write test: GIVEN `markStopDepartedUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = MARK_DEPARTED, errorType)` is logged and `trackException(throwable,
  MARK_DEPARTED)` is called, in `TripDetailViewModelTest.kt`
- [x] 26.2 Implement: update `markStopDeparted()`'s failure branch for dual-channel emission (already
  logs `OperationFailed`; add the missing `trackException` call)

## 27. TripDetailViewModel — Stop Departure Undone (BDD)

- [x] 27.1 Write test: GIVEN `undoMarkStopDepartedUseCase` succeeds and returns a `Stop` with `order =
  1` WHEN `undoMarkStopDeparted()` completes THEN `StopDepartureUndone(stopPosition = 1)` is logged
  — this action currently logs nothing, in `TripDetailViewModelTest.kt`
- [x] 27.2 Implement: update `undoMarkStopDeparted()`'s success branch (currently absent) to log
  `StopDepartureUndone(stopPosition)` sourced from the returned `Stop.order`

## 28. TripDetailViewModel — Stop Departure Undone Failure (BDD)

- [x] 28.1 Write test: GIVEN `undoMarkStopDepartedUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = UNDO_MARK_DEPARTED, errorType)` is logged and
  `trackException(throwable, UNDO_MARK_DEPARTED)` is called — this failure path currently tracks
  nothing, in `TripDetailViewModelTest.kt`
- [x] 28.2 Implement: update `undoMarkStopDeparted()`'s failure branch (currently absent) for
  dual-channel emission using `AnalyticsOperation.UNDO_MARK_DEPARTED`

## 29. TripDetailViewModel — Place Search Performed (BDD)

- [x] 29.1 Write test: GIVEN `searchPlacesUseCase` returns 5 suggestions WHEN the search success
  branch runs THEN `PlaceSearchPerformed(suggestionCount = 5)` is logged — this path currently tracks
  nothing, in `TripDetailViewModelTest.kt`
- [x] 29.2 Implement: update the `OnSearchQueryChanged` success branch (line ~278) to log
  `PlaceSearchPerformed(suggestions.size)`

## 30. TripDetailViewModel — Place Search Failure (BDD)

- [x] 30.1 Write test: GIVEN `searchPlacesUseCase` fails WHEN the search failure branch runs THEN
  `OperationFailed(operation = SEARCH_PLACE, errorType)` is logged and `trackException(throwable,
  SEARCH_PLACE)` is called — this path currently tracks nothing, in `TripDetailViewModelTest.kt`
- [x] 30.2 Implement: update the `OnSearchQueryChanged` failure branch (line ~287) for dual-channel
  emission using the new `AnalyticsOperation.SEARCH_PLACE`

## 31. TripDetailViewModel — Place Suggestion Selected (BDD)

- [x] 31.1 Write test: GIVEN a suggestion at index 0 is selected WHEN `OnSuggestionSelected` is
  processed THEN `PlaceSuggestionSelected(suggestionPosition = 0)` is logged — this path currently
  tracks nothing, in `TripDetailViewModelTest.kt`
- [x] 31.2 Implement: update `OnSuggestionSelected` handling (line ~302) to log
  `PlaceSuggestionSelected(suggestionPosition)` sourced from the suggestion's index in the current
  suggestions list

## 32. TripDetailViewModel — Place Detail Resolution Failure (BDD)

- [x] 32.1 Write test: GIVEN `getPlaceDetailUseCase` fails WHEN the resolution failure branch runs
  THEN `OperationFailed(operation = RESOLVE_PLACE, errorType)` is logged and
  `trackException(throwable, RESOLVE_PLACE)` is called — this path currently tracks nothing, in
  `TripDetailViewModelTest.kt`
- [x] 32.2 Implement: update the suggestion-resolution failure branch (line ~323) for dual-channel
  emission using the new `AnalyticsOperation.RESOLVE_PLACE`

## 33. TripDetailViewModel — Route Calculated (BDD)

- [x] 33.1 Write test: GIVEN `calculateRouteUseCase` succeeds and returns legs summing to 120,000
  metres and 5,400 seconds across 2 legs WHEN `calculateRoute()` completes THEN
  `RouteCalculated(stopCount, legCount = 2, distanceBand = RANGE_50_200KM, durationBand = RANGE_1_3H)`
  is logged, in `TripDetailViewModelTest.kt`
- [x] 33.2 Implement: update `calculateRoute()`'s success branch to band the returned `List<Leg>`'s
  summed distance/duration directly (per section 9's new return type) and log `RouteCalculated(...)`
  — this also resolves the deferred TODO at line 148, since `duration_band` now covers what it
  deferred

## 34. TripDetailViewModel — Route Calculation Failure (BDD)

- [x] 34.1 Write test: GIVEN `calculateRouteUseCase` fails WHEN the failure branch runs THEN
  `OperationFailed(operation = CALCULATE_ROUTE, errorType)` is logged and `trackException(throwable,
  CALCULATE_ROUTE)` is called, in `TripDetailViewModelTest.kt`
- [x] 34.2 Implement: update `calculateRoute()`'s failure branch for dual-channel emission (already
  logs `OperationFailed`; add the missing `trackException` call)

## 35. TripDetailViewModel — Navigation Launched (BDD)

- [x] 35.1 Write test: GIVEN `navigateToStop(stopId)` is called for the destination at position 2
  WHEN `TripDetailUiEffect.LaunchNavigation` is emitted THEN `NavigationLaunched(stopType =
  StopTypeParam.DESTINATION, stopPosition = 2)` is logged — this path currently tracks nothing, in
  `TripDetailViewModelTest.kt`
- [x] 35.2 Implement: update `navigateToStop()` (line ~491) to log `NavigationLaunched(stopType,
  stopPosition)`, deriving `stopType` from whether the stop is the starting point, destination, or an
  intermediate stop, and `stopPosition` from its index in the combined stop list

## 36. Integration: Application Startup Wiring

No test pairs — `VeniceApplication` is excluded from Kover coverage (it requires an Android
`Context` and is not unit-testable without Robolectric, which this project does not use) and is
verified on-device instead.

- [x] 36.1 Inject `AnalyticsClient` into `VeniceApplication` via Hilt member injection
  (`@Inject lateinit var`)
- [x] 36.2 In `onCreate()`, immediately after `super.onCreate()` and before
  `Places.initializeWithNewPlacesApiEnabled(...)`, call `analyticsClient.setUserProperty
  (AnalyticsUserProperty.DistanceUnit(unit))` with `unit` derived from
  `DistanceFormatter.isImperialLocale(...)`, per design decision 6

## 37. Final Verification

- [x] 37.1 Run `./gradlew detektDebug` and resolve any findings
- [x] 37.2 Run `./gradlew testDebugUnitTest` and confirm all tests pass, including every test added
  in sections 2–35
- [x] 37.3 Run `./gradlew koverVerify` (or the project's equivalent coverage task) and confirm the
  95%+ bound is maintained
- [x] 37.4 Cross-check every `AnalyticsEvent`/`AnalyticsOperation`/`AnalyticsErrorType`/
  `AnalyticsScreen`/`StopTypeParam`/`DistanceBand`/`DurationBand`/`CountBand` value in code against
  `docs/analytics/tracking-plan.md`'s Event Dictionary, Parameter Reference, and User Properties
  tables — confirm zero drift in either direction
- [x] 37.5 On-device: exercise trip creation, stop add/edit/remove/reorder, mark-departed/undo,
  place search and selection, route calculation, and navigation launch; confirm via
  `adb logcat -s Analytics` that each of the 14 events fires exactly once per action with the
  expected name and parameters, and that no identifier, free text, place name, or coordinate appears
  in any payload
- [x] 37.6 On-device: confirm `distance_unit` appears in Logcat before any other analytics log line
  on a cold app start, and confirm a release build emits no debug analytics logs (existing
  `DebugAnalyticsProvider` debug-only gating, unchanged by this story)
