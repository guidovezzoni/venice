## MODIFIED Requirements

### Requirement: TripDetailUiState represents the screen state
`TripDetailUiState` SHALL be a data class with:
- `tripId: String` (default `""`)
- `startingPoint: Stop?` (default `null`)
- `destination: Stop?` (default `null`)
- `intermediateStops: List<Stop>` (default `emptyList()`)
- `isLoading: Boolean` (default `false`)
- `isSetStartingPointDialogVisible: Boolean` (default `false`)
- `isSetDestinationDialogVisible: Boolean` (default `false`)
- `isAddStopDialogVisible: Boolean` (default `false`)
- `isEditStopDialogVisible: Boolean` (default `false`)
- `editingStop: Stop?` (default `null`)
- `canAddMoreStops: Boolean` (default `false`)
- `isRemoveStopDialogVisible: Boolean` (default `false`)
- `stopToRemove: Stop?` (default `null`)
- `placeSuggestions: List<PlaceSuggestion>` (default `emptyList()`)
- `isSearchingPlaces: Boolean` (default `false`)
- `searchError: String?` (default `null`)
- `selectedPlaceDetail: PlaceDetail?` (default `null`)
- `isResolvingPlace: Boolean` (default `false`)
- `placeDetailError: String?` (default `null`)
- `legs: List<Leg>` (default `emptyList()`)
- `isCalculatingRoute: Boolean` (default `false`)
- `routeError: String?` (default `null`)
- `formattedTotalDistance: String?` (default `null`) — pre-computed, locale-aware formatted
  sum of every leg's `distanceMetres`, using the same `formatDistance` unit/rounding rules
  as individual legs. `null` when the route is not complete (`legs.size != stops.size - 1`,
  or fewer than 2 stops), meaning the total is shown as "unavailable" rather than as zero
  or a stale value.
- `formattedTotalDuration: String?` (default `null`) — pre-computed formatted sum of every
  leg's `durationSeconds`, using the same `formatDuration` rules as individual legs. `null`
  under the same incompleteness conditions as `formattedTotalDistance`, and always
  transitions to/from `null` in lockstep with it, since both are derived from the same
  completeness check on the same emission. Formatting and completeness for both totals are
  computed entirely in the ViewModel; the composable performs no arithmetic, locale
  detection, or string-resource resolution.

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, `legs` is empty, `isCalculatingRoute` is `false`, `routeError` is `null`, `formattedTotalDistance` is `null`, and `formattedTotalDuration` is `null`

#### Scenario: Total distance present when route is complete
- **WHEN** `legs.size == stops.size - 1` and `stops.size >= 2`
- **THEN** `formattedTotalDistance` is a non-null string equal to `formatDistance` applied to the sum of every leg's `distanceMetres`

#### Scenario: Total distance unavailable when route is incomplete or absent
- **WHEN** `legs` is empty, or `legs.size` is neither `0` nor `stops.size - 1`, or `stops.size < 2`
- **THEN** `formattedTotalDistance` is `null`

#### Scenario: Total duration present when route is complete
- **WHEN** `legs.size == stops.size - 1` and `stops.size >= 2`
- **THEN** `formattedTotalDuration` is a non-null string equal to `formatDuration` applied to the sum of every leg's `durationSeconds`, and `formattedTotalDistance` is simultaneously non-null

#### Scenario: Total duration unavailable when route is incomplete or absent
- **WHEN** `legs` is empty, or `legs.size` is neither `0` nor `stops.size - 1`, or `stops.size < 2`
- **THEN** `formattedTotalDuration` is `null`, and `formattedTotalDistance` is simultaneously `null`

### Requirement: TripDetailViewModel drives the screen
`TripDetailViewModel` SHALL:
- Accept `tripId` from `SavedStateHandle`.
- Expose `uiState: StateFlow<TripDetailUiState>` and `uiEffect: SharedFlow<TripDetailUiEffect>`.
- Provide `fun onIntent(intent: TripDetailUiIntent)`.
- On initialisation, collect `ObserveStopsUseCase(tripId)` and update:
  - `startingPoint` with the stop where `order = 0` (or `null` if absent).
  - `destination` with the stop having the highest `order` where `order > 0` (or `null` if absent).
  - `intermediateStops` with all stops where `order > 0` and `order < destination.order`, sorted by `order` ascending.
  - `canAddMoreStops` as `true` when the total stop count is less than 25, `false` otherwise.
- On `OnSetStartingPointClicked`: set `isSetStartingPointDialogVisible = true`.
- On `OnDismissStartingPointDialog`: set `isSetStartingPointDialogVisible = false`, clear search state, cancel search job, call `PlaceSearchRepository.resetSession()`.
- All async save operations SHALL use `withMinimumDuration { ... }` (which defaults to 500 ms) to ensure `isLoading` remains `true` for at least 500 ms, even if the underlying operation completes faster.
- On `OnStartingPointConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.STARTING_POINT`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnSetDestinationClicked`: set `isSetDestinationDialogVisible = true`.
- On `OnDismissDestinationDialog`: set `isSetDestinationDialogVisible = false`, clear search state, cancel search job, call `PlaceSearchRepository.resetSession()`.
- On `OnDestinationConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.DESTINATION`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnAddStopClicked`: set `isAddStopDialogVisible = true`.
- On `OnDismissAddStopDialog`: set `isAddStopDialogVisible = false`, clear search state, cancel search job, call `PlaceSearchRepository.resetSession()`.
- On `OnAddStopConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.INTERMEDIATE`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMoveStopUp`: set `isLoading = true`; call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder - 1)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMoveStopDown`: set `isLoading = true`; call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder + 1)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnEditStopClicked`: set `editingStop` to the provided stop and `isEditStopDialogVisible = true`.
- On `OnDismissEditStopDialog`: set `editingStop = null` and `isEditStopDialogVisible = false`, clear search state, cancel search job, call `PlaceSearchRepository.resetSession()`.
- On `OnEditStopConfirmed`: set `isLoading = true`; call `EditStopUseCase` with the provided `stopId`, `placeName`, `latitude`, `longitude`; on success set `editingStop = null`, `isEditStopDialogVisible = false`, and `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnRemoveStopClicked`: set `stopToRemove` to the provided stop and `isRemoveStopDialogVisible = true`.
- On `OnDismissRemoveStopDialog`: set `stopToRemove = null` and `isRemoveStopDialogVisible = false`.
- On `OnRemoveStopConfirmed`: set `isLoading = true`; call `RemoveStopUseCase(tripId, stopToRemove!!.id)`; on success set `stopToRemove = null`, `isRemoveStopDialogVisible = false`, and `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMarkStopDepartedClicked`: set `isLoading = true`; call `MarkStopDepartedUseCase(tripId, stopId)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnUndoMarkStopDepartedClicked`: set `isLoading = true`; call `UndoMarkStopDepartedUseCase(tripId)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnSearchQueryChanged(query)`: cancel any pending search job; if query is blank, call `clearSearchState()`; otherwise set `isSearchingPlaces = true`, clear `placeDetailError`, debounce 300ms, call `SearchPlacesUseCase`; on success update `placeSuggestions`, clear `searchError`, set `isSearchingPlaces = false`; on failure set `searchError`, clear `placeSuggestions`, set `isSearchingPlaces = false`.
- On `OnSuggestionSelected(suggestion)`: set `isResolvingPlace = true` and clear `placeDetailError`; call `GetPlaceDetailUseCase(suggestion.placeId)`; on success set `selectedPlaceDetail`, clear `placeSuggestions`, set `isResolvingPlace = false`; on failure set `placeDetailError` with the error message, set `isResolvingPlace = false`. SHALL NOT emit `ShowError` effect on Place Details failure.
- On initialisation, collect `ObserveLegsUseCase(tripId)` and update `legs` in the UI state.
- On `OnCalculateRouteClicked`: set `isCalculatingRoute = true` and clear `routeError`; build the ordered list of all stops (starting point + intermediate + destination); call `CalculateRouteUseCase(tripId, stops)` with `withMinimumDuration`; on success set `isCalculatingRoute = false` (legs update via `ObserveLegsUseCase` Flow); on failure set `isCalculatingRoute = false` and set `routeError` to the error message.
- On initialisation, also maintain a single `combine(ObserveStopsUseCase(tripId), ObserveLegsUseCase(tripId))` collector that computes and updates **both** `formattedTotalDistance` and `formattedTotalDuration` on every emission from either source, in one lambda invocation applied via one state update (no separate collector is added for duration):
  - Completeness: `legs.size == stops.size - 1 && stops.size >= 2`.
  - When complete: sum `leg.distanceMetres` across all `legs`, format the sum via the existing `formatDistance(sum, Locale.getDefault(), application.resources)`, and set `formattedTotalDistance` to the formatted string; sum `leg.durationSeconds` across all `legs` (accumulated as `Long` to guard against overflow, then narrowed to `Int`), format the sum via the existing `formatDuration(totalDurationSeconds, application.resources)`, and set `formattedTotalDuration` to the formatted string.
  - When not complete: set both `formattedTotalDistance = null` and `formattedTotalDuration = null`.
  - This collector is independent of, and does not alter, the existing stops-only collector (`startingPoint`, `destination`, `intermediateStops`, `canAddMoreStops`, `formattedStopCoordinates`) or the existing legs-only collector (`legs`, `formattedLegDistances`, `formattedLegDurations`).

#### Scenario: Calculate route — success
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` succeeds
- **THEN** `isCalculatingRoute` becomes `false`, `routeError` is `null`, and `legs` updates via Flow observation

#### Scenario: Calculate route — failure
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` fails with message "Network error"
- **THEN** `isCalculatingRoute` becomes `false` and `routeError` is `"Network error"`

#### Scenario: Total duration updates when route is calculated
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` succeeds, producing one leg per consecutive stop pair
- **THEN** `uiState.formattedTotalDuration` becomes the formatted sum of all leg durations, without any additional user action

#### Scenario: Total duration sums exactly two legs
- **WHEN** legs of `durationSeconds = 600` and `durationSeconds = 900` are observed for a trip with exactly 3 placed stops
- **THEN** `uiState.formattedTotalDuration` equals `formatDuration(1500, application.resources)`

#### Scenario: Total duration unavailable with no legs but a full stop set
- **WHEN** `stops.size >= 2` and no legs exist for the trip (route never calculated)
- **THEN** `uiState.formattedTotalDuration` is `null`

#### Scenario: Total duration unavailable with an incomplete leg set
- **WHEN** `legs.size` is neither `0` nor `stops.size - 1` for the current `stops`
- **THEN** `uiState.formattedTotalDuration` is `null`

#### Scenario: Total duration unavailable with fewer than two stops
- **WHEN** `stops.size < 2`
- **THEN** `uiState.formattedTotalDuration` is `null`, regardless of any legs present

#### Scenario: Total duration becomes unavailable after a route-invalidating stop mutation
- **WHEN** `uiState.formattedTotalDuration` holds a non-null value and a stop mutation (add, remove, move, or edit) invalidates all legs for the trip
- **THEN** `uiState.formattedTotalDuration` transitions to `null` once the legs flow emits the now-empty list, at the same time as `formattedTotalDistance`

### Requirement: TripDetailScreen renders leg summaries between stops
The trip detail screen SHALL render a `LegSummary` composable between each pair of consecutive stops when legs are available. The leg for a given pair is matched by `fromStopId` and `toStopId`. The screen SHALL additionally render a `TripTotalSummary` composable, defined in `ui/screens/tripdetail/TripTotalSummary.kt`, as a `LazyColumn` item placed after the calculate-route button / route-error item block:
- `TripTotalSummary` accepts `modifier: Modifier = Modifier`, `formattedTotalDistance: String?`, and `formattedTotalDuration: String?`.
- When at least one of `formattedTotalDistance` / `formattedTotalDuration` is non-null, it renders both totals side by side in a `Row` of two equal-width halves (distance on the left, duration on the right). Each half independently displays its own label alongside its value when non-null, or its own metric-specific "unavailable" text when only that value is `null`.
- When **both** `formattedTotalDistance` and `formattedTotalDuration` are `null`, it renders a single combined `trip_detail_totals_unavailable` message instead of the side-by-side `Row`.
- `TripTotalSummary` performs no locale detection, unit conversion, string-resource-driven arithmetic, or business-logic branching beyond the availability-based render selection described above — it is purely presentational, mirroring `LegSummary` and `TripProgressSummary`.

#### Scenario: Legs displayed between stops
- **WHEN** 3 stops exist and 2 legs are available
- **THEN** a `LegSummary` is rendered between stop 1 and stop 2, and between stop 2 and stop 3

#### Scenario: No legs available
- **WHEN** 3 stops exist but no legs are available
- **THEN** no `LegSummary` composables are rendered

#### Scenario: Total distance summary displayed with a value
- **WHEN** `uiState.formattedTotalDistance` is `"12.5 km"`
- **THEN** `TripTotalSummary` displays the `trip_detail_total_distance_label` text and the value `"12.5 km"`

#### Scenario: Total distance and duration summary displayed side by side
- **WHEN** `uiState.formattedTotalDistance` is `"12.5 km"` and `uiState.formattedTotalDuration` is `"15 min"`
- **THEN** `TripTotalSummary` displays both the `trip_detail_total_distance_label` text with value `"12.5 km"` and the `trip_detail_total_duration_label` text with value `"15 min"`, side by side

#### Scenario: Total distance and duration summary displayed as unavailable
- **WHEN** `uiState.formattedTotalDistance` is `null` and `uiState.formattedTotalDuration` is `null`
- **THEN** `TripTotalSummary` displays the single combined `trip_detail_totals_unavailable` text and no numeric values

#### Scenario: Total distance summary displayed while duration is unavailable
- **WHEN** `uiState.formattedTotalDistance` is `"12.5 km"` and `uiState.formattedTotalDuration` is `null`
- **THEN** `TripTotalSummary` displays the distance label and value in its half of the `Row`, and the `trip_detail_total_duration_unavailable` text in the other half

#### Scenario: Total duration summary displayed while distance is unavailable
- **WHEN** `uiState.formattedTotalDuration` is `"15 min"` and `uiState.formattedTotalDistance` is `null`
- **THEN** `TripTotalSummary` displays the duration label and value in its half of the `Row`, and the `trip_detail_total_distance_unavailable` text in the other half

### Requirement: TripDetailScreen route preview coverage
The trip detail screen previews SHALL include:
- A preview with legs displayed between stops
- A preview with `isCalculatingRoute = true`
- A preview with `routeError` set
- A preview with a non-null `formattedTotalDistance` and non-null `formattedTotalDuration` (satisfying non-default `UiState`-field preview coverage)
- A preview with `formattedTotalDistance = null` and `formattedTotalDuration = null` alongside a populated stop/leg set, showing the combined unavailable state

`TripTotalSummary` SHALL itself have previews covering all four reachable-or-defensive availability combinations, each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`:
- Both `formattedTotalDistance` and `formattedTotalDuration` available (side by side)
- Both unavailable (combined message)
- `formattedTotalDistance` available, `formattedTotalDuration` unavailable
- `formattedTotalDuration` available, `formattedTotalDistance` unavailable

#### Scenario: Route previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** preview variants for legs, calculating state, error state, both-totals-present state, and both-totals-unavailable state are all visible

#### Scenario: TripTotalSummary previews exist
- **WHEN** `TripTotalSummary` is inspected in Android Studio
- **THEN** four preview variants are visible, covering both-available, both-unavailable, distance-only-available, and duration-only-available
