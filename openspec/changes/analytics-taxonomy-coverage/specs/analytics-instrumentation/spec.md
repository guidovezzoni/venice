## ADDED Requirements

### Requirement: Every failure path emits both a bounded event and a diagnostic exception
Every `onFailure` branch in `TripListViewModel` and `TripDetailViewModel` SHALL call both
`analyticsClient.logEvent(AnalyticsEvent.OperationFailed(...))` and
`analyticsClient.trackException(throwable, operation)` on the same path, using the same
`AnalyticsOperation` value for both calls. No failure path SHALL call only one of the two.

#### Scenario: A failed operation fires both channels
- **GIVEN** any tracked operation (`create_trip`, `set_stop`, `edit_stop`, `remove_stop`,
  `move_stop`, `mark_departed`, `undo_mark_departed`, `calculate_route`, `search_place`,
  `resolve_place`) fails
- **WHEN** the corresponding ViewModel's failure branch runs
- **THEN** `analyticsClient.logEvent(OperationFailed(operation, errorType))` is called and
  `analyticsClient.trackException(throwable, operation)` is called, both with the same `operation`
  value

#### Scenario: The error type is classified once and reused for both calls
- **WHEN** a failure branch runs
- **THEN** `AnalyticsErrorClassifier` is invoked exactly once for that failure, and its result is used
  to build the `OperationFailed` event

### Requirement: Five previously-silent user actions now emit analytics events
`TripDetailViewModel` SHALL emit an event for each of the following actions, none of which emit
anything before this change: a place search returning results (`PlaceSearchPerformed`), a place
suggestion being selected (`PlaceSuggestionSelected`), a departure being undone successfully
(`StopDepartureUndone`), navigation being launched for a stop (`NavigationLaunched`), and the two
previously-untracked failure paths for place search and place-detail resolution
(`OperationFailed` with `operation` = `search_place` / `resolve_place` respectively, each paired with
`trackException` per the dual-channel requirement above).

#### Scenario: A successful place search logs the result count
- **WHEN** `searchPlacesUseCase` returns a successful list of suggestions
- **THEN** `analyticsClient.logEvent(PlaceSearchPerformed(suggestionCount = <list size>))` is called

#### Scenario: Selecting a suggestion logs its position
- **WHEN** a user selects a suggestion from the place-search results
- **THEN** `analyticsClient.logEvent(PlaceSuggestionSelected(suggestionPosition = <its index>))` is
  called

#### Scenario: Undoing a departure logs the reverted stop's position
- **WHEN** `undoMarkStopDepartedUseCase` succeeds
- **THEN** `analyticsClient.logEvent(StopDepartureUndone(stopPosition = <returned Stop.order>))` is
  called

#### Scenario: Launching navigation logs the stop's type and position
- **WHEN** `navigateToStop` emits `TripDetailUiEffect.LaunchNavigation` for a stop
- **THEN** `analyticsClient.logEvent(NavigationLaunched(stopType, stopPosition))` is called with that
  stop's type and 0-based position

#### Scenario: A place search failure is tracked under its own operation
- **WHEN** `searchPlacesUseCase` fails
- **THEN** `OperationFailed(operation = AnalyticsOperation.SEARCH_PLACE, errorType)` is logged and
  `trackException(throwable, AnalyticsOperation.SEARCH_PLACE)` is called

#### Scenario: A place-detail resolution failure is tracked under its own operation
- **WHEN** `getPlaceDetailUseCase` fails
- **THEN** `OperationFailed(operation = AnalyticsOperation.RESOLVE_PLACE, errorType)` is logged and
  `trackException(throwable, AnalyticsOperation.RESOLVE_PLACE)` is called

### Requirement: `trip_opened` fires once from trip-detail entry, not from the trip list
`TripDetailViewModel` SHALL log `AnalyticsEvent.TripOpened(stopCount, routeState)` exactly once per
ViewModel instance, from `init`, sourced from the first combined emission of its observed stops and
legs. `TripListViewModel.onIntent` SHALL NOT log a `TripOpened` event when a trip is clicked.

#### Scenario: Opening a trip logs TripOpened exactly once
- **GIVEN** a `TripDetailViewModel` is constructed for a trip with observed stops and legs
- **WHEN** the stops and legs flows emit their initial values, and then emit again after a
  subsequent mutation
- **THEN** `TripOpened` is logged exactly once, on the first combined emission

#### Scenario: Clicking a trip in the list does not log TripOpened
- **WHEN** `TripListViewModel.onIntent(OnTripClicked(tripId))` is processed
- **THEN** no `TripOpened` event is logged by `TripListViewModel`

#### Scenario: route_state reflects route completeness
- **GIVEN** a trip's stops and legs satisfy `legs.size == stops.size - 1 && stops.size >= 2`
- **WHEN** `TripOpened` is logged
- **THEN** its `route_state` is `"complete"`; otherwise it is `"none"`

### Requirement: `stop_count` reflects the post-mutation count without waiting for the stops flow
For `StopAdded`, `StopRemoved`, `StopReordered`, and `StopDeparted`, `TripDetailViewModel` SHALL
compute `stop_count` from the stop lists already held in its own `uiState` at the moment the mutation's
success callback runs, adjusted by a deterministic delta for add (+1) and remove (-1) operations,
rather than reading a value that depends on `observeStopsUseCase`'s `Flow` having re-emitted the
post-mutation list.

#### Scenario: Adding a stop counts the stop that is not yet reflected in observed state
- **GIVEN** `uiState` currently reflects 2 stops before a new stop is added
- **WHEN** `setStop` succeeds
- **THEN** the logged `StopAdded.stop_count` is 3, regardless of whether the `observeStopsUseCase`
  flow has re-emitted yet

#### Scenario: Removing a stop counts the stop that is still reflected in observed state
- **GIVEN** `uiState` currently reflects 3 stops including the one being removed
- **WHEN** `removeStop` succeeds
- **THEN** the logged `StopRemoved.stop_count` is 2

### Requirement: `stop_position` is sourced from the domain layer, not from UI state
`TripDetailViewModel` SHALL source `stop_position` for `StopDeparted` and `StopDepartureUndone` from
the `Stop.order` field of the `Stop` returned by `MarkStopDepartedUseCase` and
`UndoMarkStopDepartedUseCase` respectively, on their success path — not from a lookup against
`uiState`'s stop lists.

#### Scenario: StopDeparted's position comes from the returned Stop
- **WHEN** `markStopDepartedUseCase` succeeds and returns a `Stop` with `order = 2`
- **THEN** the logged `StopDeparted.stop_position` is 2

#### Scenario: StopDepartureUndone's position comes from the returned Stop
- **WHEN** `undoMarkStopDepartedUseCase` succeeds and returns a `Stop` with `order = 2`
- **THEN** the logged `StopDepartureUndone.stop_position` is 2

### Requirement: `CalculateRouteUseCase` returns the calculated legs on success
`CalculateRouteUseCase` SHALL return `Result<List<Leg>>` (previously `Result<Unit>`), forwarding the
legs `RouteRepository.calculateRoute` persisted. `TripDetailViewModel.calculateRoute` SHALL band the
returned legs' total distance and total duration directly, without a separate read of
`observeLegsUseCase`, when logging `RouteCalculated`.

#### Scenario: A successful calculation logs bands derived from the returned legs
- **GIVEN** `calculateRouteUseCase` succeeds and returns a list of legs summing to 120,000 metres and
  5,400 seconds
- **WHEN** `RouteCalculated` is logged
- **THEN** its `distance_band` is `DistanceBand.RANGE_50_200KM` and its `duration_band` is
  `DurationBand.RANGE_1_3H`, and its `leg_count` equals the returned list's size

### Requirement: `MarkStopDepartedUseCase` and `UndoMarkStopDepartedUseCase` return the affected stop
`MarkStopDepartedUseCase` SHALL return `Result<Stop>` (previously `Result<Unit>`), the stop that was
marked departed, with its status updated to `VISITED`. `UndoMarkStopDepartedUseCase` SHALL return
`Result<Stop>` (previously `Result<Unit>`), the stop whose departure was undone, with its status
reverted to `PENDING`. Neither use case SHALL perform a database read-back to construct the returned
value; both SHALL derive it from the `Stop` already loaded during the use case's existing precondition
checks.

#### Scenario: MarkStopDepartedUseCase returns the departed stop with updated status
- **WHEN** `MarkStopDepartedUseCase` succeeds for a stop with `order = 1`
- **THEN** it returns `Result.success` wrapping a `Stop` with `order = 1` and `status = VISITED`

#### Scenario: UndoMarkStopDepartedUseCase returns the reverted stop with updated status
- **WHEN** `UndoMarkStopDepartedUseCase` succeeds for the most recently departed stop, at
  `order = 1`
- **THEN** it returns `Result.success` wrapping a `Stop` with `order = 1` and `status = PENDING`

### Requirement: `distance_unit` is set at application start, before the first event
`VeniceApplication` SHALL inject `AnalyticsClient` and call
`analyticsClient.setUserProperty(AnalyticsUserProperty.DistanceUnit(unit))` in `onCreate()`, with
`unit` derived from `DistanceFormatter.isImperialLocale(...)`, before any other initialisation in
`onCreate()` that could lead to an analytics event being logged.

#### Scenario: distance_unit is set before Places SDK initialisation
- **WHEN** `VeniceApplication.onCreate()` runs
- **THEN** `setUserProperty(DistanceUnit(...))` is called before
  `Places.initializeWithNewPlacesApiEnabled(...)`

#### Scenario: An imperial locale sets the imperial unit
- **GIVEN** `DistanceFormatter.isImperialLocale(...)` returns `true`
- **WHEN** `VeniceApplication.onCreate()` runs
- **THEN** `DistanceUnit(unit = "imperial")` is set

### Requirement: `trip_count_band` is set when the trip list loads and when a trip is created or removed
`TripListViewModel` SHALL call `analyticsClient.setUserProperty(AnalyticsUserProperty.TripCountBand
(band))` when the observed trip list first loads, and again whenever a trip is successfully created
or removed, with `band` computed from the current trip count via the `CountBand` mapping function.

#### Scenario: The trip list loading sets the band
- **WHEN** `TripListViewModel`'s observed trip list first emits with 3 trips
- **THEN** `setUserProperty(TripCountBand(band = CountBand.RANGE_2_5))` is called

#### Scenario: Creating a trip updates the band
- **WHEN** `createTrip()` succeeds, taking the trip count from 1 to 2
- **THEN** `setUserProperty(TripCountBand(band = CountBand.RANGE_2_5))` is called
