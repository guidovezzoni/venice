## MODIFIED Requirements

### Requirement: TripDetailUiState represents the screen state
`TripDetailUiState` SHALL be a data class with:
- `tripId: String` (default `""`)
- `tripName: String?` (default `null`) — the trip's persisted name, observed live via `ObserveTripUseCase`. `null` means the name has not arrived yet (e.g. first composition, or the trip is not yet found); the composable falls back to the `trip_detail_title` string resource while `tripName` is `null`. No loading indicator is associated with this field.
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
- `isRouteRecalculationPromptVisible: Boolean` (default `false`) — pre-computed in the
  ViewModel from the same `stops`/`legs` inputs and the same completeness check as
  `formattedTotalDistance` / `formattedTotalDuration`, plus the same 2+-stops guard already
  used by the "Calculate route" button: `stops.size >= 2 && legs.size != stops.size - 1`.
  `true` means the route has never been calculated, or has gone stale after a stop mutation
  (both cases are indistinguishable and handled by a single generic prompt, since
  `InvalidateRouteUseCase` always deletes all legs at once). The composable performs no
  completeness or stop-count arithmetic of its own to decide the prompt's visibility.

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `tripName` is `null`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, `legs` is empty, `isCalculatingRoute` is `false`, `routeError` is `null`, `formattedTotalDistance` is `null`, `formattedTotalDuration` is `null`, and `isRouteRecalculationPromptVisible` is `false`

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

#### Scenario: Trip name reflects the observed trip
- **WHEN** `ObserveTripUseCase(tripId)` emits a `Trip` with `name = "Summer Roadtrip"`
- **THEN** `uiState.tripName` becomes `"Summer Roadtrip"`

#### Scenario: Trip name stays null while the trip has not been observed yet
- **WHEN** the ViewModel has just initialised and `ObserveTripUseCase(tripId)` has not yet emitted
- **THEN** `uiState.tripName` remains `null`

#### Scenario: Recalculation prompt visible when route incomplete with 2+ stops
- **WHEN** `stops.size >= 2` and `legs.size != stops.size - 1` (including the no-legs-at-all case)
- **THEN** `isRouteRecalculationPromptVisible` is `true`

#### Scenario: Recalculation prompt hidden when route is complete
- **WHEN** `legs.size == stops.size - 1` and `stops.size >= 2`
- **THEN** `isRouteRecalculationPromptVisible` is `false`

#### Scenario: Recalculation prompt hidden with fewer than two stops
- **WHEN** `stops.size < 2`
- **THEN** `isRouteRecalculationPromptVisible` is `false`, regardless of any legs present

#### Scenario: Recalculation prompt becomes visible after a route-invalidating stop mutation
- **WHEN** `isRouteRecalculationPromptVisible` is `false` (route complete) and a stop mutation (add, remove, move, or edit) invalidates all legs for the trip, leaving `stops.size >= 2`
- **THEN** `isRouteRecalculationPromptVisible` transitions to `true` once the legs flow emits the now-empty list, at the same time `formattedTotalDistance` / `formattedTotalDuration` transition to `null`

### Requirement: TripDetailViewModel drives the screen
`TripDetailViewModel` SHALL:
- Accept `tripId` from `SavedStateHandle`.
- Expose `uiState: StateFlow<TripDetailUiState>` and `uiEffect: SharedFlow<TripDetailUiEffect>`.
- Provide `fun onIntent(intent: TripDetailUiIntent)`.
- On initialisation, collect `ObserveTripUseCase(tripId)` and update `tripName` with the emitted `Trip?`'s `name` (or leave `tripName` at its current value if the emission is `null`, i.e. the trip is not found — the "Trip Detail" fallback title is a composable-level default and is never written back into `tripName` by the ViewModel). This collector is independent of the stops-only, legs-only, and combined stops+legs collectors described below.
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
- On initialisation, also maintain a single `combine(ObserveStopsUseCase(tripId), ObserveLegsUseCase(tripId))` collector that computes and updates `formattedTotalDistance`, `formattedTotalDuration`, **and** `isRouteRecalculationPromptVisible` on every emission from either source, in one lambda invocation applied via one state update (no separate collector is added for any of the three):
  - Completeness: `legs.size == stops.size - 1 && stops.size >= 2`.
  - When complete: sum `leg.distanceMetres` across all `legs`, format the sum via the existing `formatDistance(sum, Locale.getDefault(), application.resources)`, and set `formattedTotalDistance` to the formatted string; sum `leg.durationSeconds` across all `legs` (accumulated as `Long` to guard against overflow, then narrowed to `Int`), format the sum via the existing `formatDuration(totalDurationSeconds, application.resources)`, and set `formattedTotalDuration` to the formatted string; set `isRouteRecalculationPromptVisible = false`.
  - When not complete: set both `formattedTotalDistance = null` and `formattedTotalDuration = null`; set `isRouteRecalculationPromptVisible = (stops.size >= 2)` (i.e. the prompt is shown only when there are enough stops to calculate a route in the first place, matching the same guard used by the "Calculate route" button).
  - This collector is independent of, and does not alter, the stops-only collector (`startingPoint`, `destination`, `intermediateStops`, `canAddMoreStops`, `formattedStopCoordinates`), the legs-only collector (`legs`, `formattedLegDistances`, `formattedLegDurations`), or the new trip-name collector (`tripName`).

#### Scenario: Calculate route — success
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` succeeds
- **THEN** `isCalculatingRoute` becomes `false`, `routeError` is `null`, and `legs` updates via Flow observation

#### Scenario: Calculate route — failure
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` fails with message "Network error"
- **THEN** `isCalculatingRoute` becomes `false` and `routeError` is "Network error"

#### Scenario: Calculate route — loading state
- **WHEN** `OnCalculateRouteClicked` is dispatched and the API call is in-flight
- **THEN** `isCalculatingRoute` is `true` and `routeError` is `null`

#### Scenario: Calculate route — minimum loading duration
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` completes in less than 500 ms
- **THEN** `isCalculatingRoute` remains `true` until at least 500 ms have elapsed

#### Scenario: Legs updated via observation
- **WHEN** the ViewModel initialises and legs exist in the database for the trip
- **THEN** `uiState.legs` contains the persisted legs

#### Scenario: Legs cleared after stop mutation
- **WHEN** a stop is added, removed, reordered, or edited (triggering leg invalidation via the use case)
- **THEN** `uiState.legs` becomes empty via Flow observation

#### Scenario: Total updates when route is calculated
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` succeeds, producing one leg per consecutive stop pair
- **THEN** `uiState.formattedTotalDistance` becomes the formatted sum of all leg distances, without any additional user action

#### Scenario: Total sums exactly two legs
- **WHEN** legs of `distanceMetres = 10000` and `distanceMetres = 2500` are observed for a trip with exactly 3 placed stops
- **THEN** `uiState.formattedTotalDistance` equals `formatDistance(12500, Locale.getDefault(), resources)`

#### Scenario: Total unavailable with no legs but a full stop set
- **WHEN** `stops.size >= 2` and no legs exist for the trip (route never calculated)
- **THEN** `uiState.formattedTotalDistance` is `null`

#### Scenario: Total unavailable with an incomplete leg set
- **WHEN** `legs.size` is neither `0` nor `stops.size - 1` for the current `stops`
- **THEN** `uiState.formattedTotalDistance` is `null`

#### Scenario: Total unavailable with fewer than two stops
- **WHEN** `stops.size < 2`
- **THEN** `uiState.formattedTotalDistance` is `null`, regardless of any legs present

#### Scenario: Total becomes unavailable after a route-invalidating stop mutation
- **WHEN** `uiState.formattedTotalDistance` holds a non-null value and a stop mutation (add, remove, move, or edit) invalidates all legs for the trip
- **THEN** `uiState.formattedTotalDistance` transitions to `null` once the legs flow emits the now-empty list

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

#### Scenario: Total respects the active locale's unit system
- **WHEN** the device locale is an imperial-unit locale (e.g. `en-US`)
- **THEN** `uiState.formattedTotalDistance` is expressed in miles, consistent with `formattedLegDistances` for the same legs

#### Scenario: Search query debounced at 300 ms
- **WHEN** `OnSearchQueryChanged("R")` is dispatched, then `OnSearchQueryChanged("Ro")` 100 ms later, then `OnSearchQueryChanged("Rom")` 100 ms later
- **THEN** only one `SearchPlacesUseCase` call is made (for "Rom") after 300 ms from the last change

#### Scenario: Search results update state
- **WHEN** `OnSearchQueryChanged("Rome")` is dispatched and `SearchPlacesUseCase` returns 3 suggestions
- **THEN** `placeSuggestions` contains 3 items, `isSearchingPlaces` is `false`, `searchError` is `null`

#### Scenario: Search failure shows error
- **WHEN** `OnSearchQueryChanged("Rome")` is dispatched and `SearchPlacesUseCase` returns failure
- **THEN** `searchError` is set to an error message, `placeSuggestions` is empty, `isSearchingPlaces` is `false`

#### Scenario: Blank query clears search state
- **WHEN** `OnSearchQueryChanged("")` is dispatched
- **THEN** `placeSuggestions` is cleared, `isSearchingPlaces` is `false`, `searchError` is `null`, and no use case call is made

#### Scenario: Suggestion selected fetches place details
- **WHEN** `OnSuggestionSelected` is dispatched with a suggestion with `placeId = "abc"` and `GetPlaceDetailUseCase` returns `PlaceDetail("Colosseum", 41.8902, 12.4922)`
- **THEN** `selectedPlaceDetail` is set to `PlaceDetail("Colosseum", 41.8902, 12.4922)` and `placeSuggestions` is cleared

#### Scenario: Suggestion selected sets resolving flag
- **WHEN** `OnSuggestionSelected` is dispatched and `GetPlaceDetailUseCase` is in-flight
- **THEN** `isResolvingPlace` is `true`

#### Scenario: Suggestion selected succeeds clears resolving flag
- **WHEN** `OnSuggestionSelected` is dispatched and `GetPlaceDetailUseCase` succeeds
- **THEN** `isResolvingPlace` is `false` and `selectedPlaceDetail` is populated

#### Scenario: Suggestion selected fails sets inline error
- **WHEN** `OnSuggestionSelected` is dispatched and `GetPlaceDetailUseCase` fails
- **THEN** `isResolvingPlace` is `false` and `placeDetailError` contains the error message

#### Scenario: New search query clears place detail error
- **WHEN** `placeDetailError` is set and `OnSearchQueryChanged` is dispatched with a non-blank query
- **THEN** `placeDetailError` is `null`

#### Scenario: New suggestion selected clears place detail error
- **WHEN** `placeDetailError` is set and `OnSuggestionSelected` is dispatched
- **THEN** `placeDetailError` is cleared before the new API call begins

#### Scenario: Dialog dismiss clears search state
- **WHEN** any dialog dismiss intent is dispatched (e.g. `OnDismissStartingPointDialog`)
- **THEN** `placeSuggestions` is cleared, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, the search job is cancelled, and `PlaceSearchRepository.resetSession()` is called

#### Scenario: Search job cancelled on new query
- **WHEN** `OnSearchQueryChanged("Rome")` is dispatched while a previous search for "Rom" is still debouncing
- **THEN** the previous search job is cancelled and only "Rome" triggers a use case call

#### Scenario: Opening starting point dialog
- **WHEN** `OnSetStartingPointClicked` is dispatched
- **THEN** `isSetStartingPointDialogVisible` becomes `true`

#### Scenario: Dismissing starting point dialog
- **WHEN** `OnDismissStartingPointDialog` is dispatched while dialog is visible
- **THEN** `isSetStartingPointDialogVisible` becomes `false`

#### Scenario: Confirming starting point — success
- **WHEN** `OnStartingPointConfirmed` is dispatched and `SetStopUseCase` with `StopType.STARTING_POINT` succeeds
- **THEN** `startingPoint` is updated, the dialog is dismissed, and `isLoading` is `false`

#### Scenario: Confirming starting point — failure
- **WHEN** `OnStartingPointConfirmed` is dispatched and `SetStopUseCase` with `StopType.STARTING_POINT` fails
- **THEN** a `ShowError` effect is emitted and `isLoading` is `false`

#### Scenario: Opening destination dialog
- **WHEN** `OnSetDestinationClicked` is dispatched
- **THEN** `isSetDestinationDialogVisible` becomes `true`

#### Scenario: Dismissing destination dialog
- **WHEN** `OnDismissDestinationDialog` is dispatched while dialog is visible
- **THEN** `isSetDestinationDialogVisible` becomes `false`

#### Scenario: Confirming destination — success
- **WHEN** `OnDestinationConfirmed` is dispatched and `SetStopUseCase` with `StopType.DESTINATION` succeeds
- **THEN** `destination` is updated, the dialog is dismissed, and `isLoading` is `false`

#### Scenario: Confirming destination — failure
- **WHEN** `OnDestinationConfirmed` is dispatched and `SetStopUseCase` with `StopType.DESTINATION` fails
- **THEN** a `ShowError` effect is emitted and `isLoading` is `false`

#### Scenario: Opening add stop dialog
- **WHEN** `OnAddStopClicked` is dispatched
- **THEN** `isAddStopDialogVisible` becomes `true`

#### Scenario: Dismissing add stop dialog
- **WHEN** `OnDismissAddStopDialog` is dispatched while dialog is visible
- **THEN** `isAddStopDialogVisible` becomes `false`

#### Scenario: Confirming add stop — success
- **WHEN** `OnAddStopConfirmed` is dispatched and `SetStopUseCase` with `StopType.INTERMEDIATE` succeeds
- **THEN** the dialog is dismissed, the new stop appears in `intermediateStops`, and `isLoading` is `false`

#### Scenario: Confirming add stop — failure
- **WHEN** `OnAddStopConfirmed` is dispatched and `SetStopUseCase` with `StopType.INTERMEDIATE` fails
- **THEN** a `ShowError` effect is emitted and `isLoading` is `false`

#### Scenario: Move stop up — success
- **WHEN** `OnMoveStopUp` is dispatched with `currentOrder = 2` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `MoveStopUseCase` is called with `(tripId, 2, 1)`, the stop list updates via the existing Flow observation, and `isLoading` is `false`

#### Scenario: Move stop down — success
- **WHEN** `OnMoveStopDown` is dispatched with `currentOrder = 1` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `MoveStopUseCase` is called with `(tripId, 1, 2)`, the stop list updates via the existing Flow observation, and `isLoading` is `false`

#### Scenario: Move stop — failure
- **WHEN** `OnMoveStopUp` or `OnMoveStopDown` is dispatched and `MoveStopUseCase` returns failure
- **THEN** a `ShowError` effect is emitted and `isLoading` is `false`

#### Scenario: Opening edit stop dialog
- **WHEN** `OnEditStopClicked` is dispatched with a stop
- **THEN** `editingStop` is set to the provided stop and `isEditStopDialogVisible` becomes `true`

#### Scenario: Dismissing edit stop dialog
- **WHEN** `OnDismissEditStopDialog` is dispatched
- **THEN** `editingStop` is set to `null` and `isEditStopDialogVisible` becomes `false`

#### Scenario: Confirming edit stop — success
- **WHEN** `OnEditStopConfirmed` is dispatched and `EditStopUseCase` succeeds
- **THEN** `editingStop` is set to `null`, `isEditStopDialogVisible` becomes `false`, the updated stop is reflected via Flow observation, and `isLoading` is `false`

#### Scenario: Confirming edit stop — failure
- **WHEN** `OnEditStopConfirmed` is dispatched and `EditStopUseCase` fails
- **THEN** a `ShowError` effect is emitted and `isLoading` is `false`

#### Scenario: Opening remove stop dialog
- **WHEN** `OnRemoveStopClicked` is dispatched with a stop
- **THEN** `stopToRemove` is set to the provided stop and `isRemoveStopDialogVisible` becomes `true`

#### Scenario: Dismissing remove stop dialog
- **WHEN** `OnDismissRemoveStopDialog` is dispatched
- **THEN** `stopToRemove` is set to `null` and `isRemoveStopDialogVisible` becomes `false`

#### Scenario: Confirming remove stop — success
- **WHEN** `OnRemoveStopConfirmed` is dispatched and `RemoveStopUseCase` succeeds
- **THEN** `stopToRemove` is set to `null`, `isRemoveStopDialogVisible` becomes `false`, the stop is removed from the list via Flow observation, and `isLoading` is `false`

#### Scenario: Confirming remove stop — failure
- **WHEN** `OnRemoveStopConfirmed` is dispatched and `RemoveStopUseCase` fails
- **THEN** a `ShowError` effect is emitted, the dialog remains visible, and `isLoading` is `false`

#### Scenario: Initialisation with existing starting point and destination
- **WHEN** the ViewModel initialises and stops with `order = 0` and `order = 1` exist
- **THEN** `startingPoint` reflects the `order = 0` stop, `destination` reflects the `order = 1` stop, `intermediateStops` is empty, and `canAddMoreStops` is `true`

#### Scenario: Initialisation with no stops
- **WHEN** the ViewModel initialises and no stops exist
- **THEN** `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, and `canAddMoreStops` is `false`

#### Scenario: Initialisation with only starting point
- **WHEN** the ViewModel initialises and only a stop with `order = 0` exists
- **THEN** `startingPoint` reflects that stop, `destination` is `null`, `intermediateStops` is empty, and `canAddMoreStops` is `true`

#### Scenario: Initialisation with intermediate stops
- **WHEN** the ViewModel initialises and stops with orders `[0, 1, 2, 3]` exist
- **THEN** `startingPoint` is the stop with `order = 0`, `destination` is the stop with `order = 3`, and `intermediateStops` contains stops with `order = 1` and `order = 2` in ascending order

#### Scenario: canAddMoreStops is false at limit
- **WHEN** the ViewModel observes 25 stops for the trip
- **THEN** `canAddMoreStops` is `false`

#### Scenario: Mark stop departed — success
- **WHEN** `OnMarkStopDepartedClicked` is dispatched and `MarkStopDepartedUseCase` succeeds
- **THEN** the stop list updates via Flow observation (the departed stop now has `status = VISITED`) and `isLoading` is `false`

#### Scenario: Mark stop departed — failure
- **WHEN** `OnMarkStopDepartedClicked` is dispatched and `MarkStopDepartedUseCase` fails
- **THEN** a `ShowError` effect is emitted with the error message and `isLoading` is `false`

#### Scenario: Undo mark departed — success
- **WHEN** `OnUndoMarkStopDepartedClicked` is dispatched and `UndoMarkStopDepartedUseCase` succeeds
- **THEN** the stop list updates via Flow observation (the reverted stop now has `status = PENDING`) and `isLoading` is `false`

#### Scenario: Undo mark departed — failure
- **WHEN** `OnUndoMarkStopDepartedClicked` is dispatched and `UndoMarkStopDepartedUseCase` fails
- **THEN** a `ShowError` effect is emitted with the error message and `isLoading` is `false`

#### Scenario: Minimum loading duration enforced
- **WHEN** any async operation completes in less than 500 ms
- **THEN** `isLoading` remains `true` until at least 500 ms have elapsed since it was set to `true`

#### Scenario: Long operation not artificially delayed
- **WHEN** any async operation takes longer than 500 ms
- **THEN** `isLoading` remains `true` for the full duration of the operation and is set to `false` immediately upon completion

#### Scenario: Trip name collector is independent of stops/legs collectors
- **WHEN** `ObserveTripUseCase(tripId)` emits a new `Trip` while `ObserveStopsUseCase` and `ObserveLegsUseCase` have not re-emitted
- **THEN** only `tripName` changes in `uiState`; `startingPoint`, `destination`, `intermediateStops`, `legs`, `formattedTotalDistance`, `formattedTotalDuration`, and `isRouteRecalculationPromptVisible` are unaffected

### Requirement: TripDetailScreen route preview coverage
The trip detail screen previews SHALL include:
- A preview with legs displayed between stops
- A preview with `isCalculatingRoute = true`
- A preview with `routeError` set
- A preview with a non-null `formattedTotalDistance` and non-null `formattedTotalDuration` (satisfying non-default `UiState`-field preview coverage)
- A preview with `formattedTotalDistance = null` and `formattedTotalDuration = null` alongside a populated stop/leg set, showing the combined unavailable state
- A preview with a non-null `tripName` (satisfying non-default `UiState`-field preview coverage), showing the trip's real name in the `TopAppBar`
- A preview with `isRouteRecalculationPromptVisible = true` alongside a 2+-stop, route-incomplete state, showing the `RouteRecalculationPrompt`

`TripTotalSummary` SHALL itself have previews covering all four reachable-or-defensive availability combinations, each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`:
- Both `formattedTotalDistance` and `formattedTotalDuration` available (side by side)
- Both unavailable (combined message)
- `formattedTotalDistance` available, `formattedTotalDuration` unavailable
- `formattedTotalDuration` available, `formattedTotalDistance` unavailable

`RouteRecalculationPrompt` SHALL itself have previews, each private, wrapped in `HeadingToVeniceTheme`, with `showBackground = true`, covering:
- Enabled (tappable) state
- Disabled state (while `isCalculatingRoute` or `isLoading` is `true`)

#### Scenario: Route previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** preview variants for legs, calculating state, error state, both-totals-present state, both-totals-unavailable state, non-null trip name, and the recalculation prompt are all visible

#### Scenario: TripTotalSummary previews exist
- **WHEN** `TripTotalSummary` is inspected in Android Studio
- **THEN** four preview variants are visible, covering both-available, both-unavailable, distance-only-available, and duration-only-available

#### Scenario: RouteRecalculationPrompt previews exist
- **WHEN** `RouteRecalculationPrompt` is inspected in Android Studio
- **THEN** two preview variants are visible, covering the enabled and disabled states

## ADDED Requirements

### Requirement: TripDetailScreen top bar shows the trip name
`TripDetailScreen`'s `TopAppBar` SHALL display `uiState.tripName` as its title when non-null. When `uiState.tripName` is `null`, it SHALL display the existing `trip_detail_title` string resource ("Trip Detail") instead. No loading spinner or additional visual state is shown for the fallback case — it is a plain string substitution performed with a null-coalescing expression (`uiState.tripName ?: stringResource(R.string.trip_detail_title)`).

#### Scenario: Top bar shows the real trip name
- **WHEN** `uiState.tripName` is `"Summer Roadtrip"`
- **THEN** the `TopAppBar` title displays "Summer Roadtrip"

#### Scenario: Top bar falls back to the placeholder title
- **WHEN** `uiState.tripName` is `null`
- **THEN** the `TopAppBar` title displays the `trip_detail_title` string resource ("Trip Detail")

### Requirement: TripDetailScreen renders route recalculation prompt
`TripDetailScreen` SHALL render a `RouteRecalculationPrompt` composable, defined in `ui/screens/tripdetail/RouteRecalculationPrompt.kt`, as a `LazyColumn` item placed immediately before the `TripTotalSummary` item, visible only when `uiState.isRouteRecalculationPromptVisible` is `true`.

`RouteRecalculationPrompt` accepts `modifier: Modifier = Modifier`, `isEnabled: Boolean`, and `onClick: () -> Unit = {}`. It renders a tappable container (e.g. `Card` or `Row` with a clickable modifier) showing the `trip_detail_recalculation_prompt_message` text and the `trip_detail_recalculation_prompt_action` call-to-action text. It performs no completeness or stop-count logic of its own — visibility and enablement are both supplied pre-computed by the caller.

`TripDetailScreen` SHALL pass `isEnabled = !isCalculatingRoute && !uiState.isLoading` (the same enablement rule already used by the "Calculate route" button) and `onClick = { onIntent(TripDetailUiIntent.OnCalculateRouteClicked) }` — no new `TripDetailUiIntent` subclass is introduced; the prompt reuses the existing `OnCalculateRouteClicked` intent.

#### Scenario: Prompt visible when route incomplete with 2+ stops
- **WHEN** `uiState.isRouteRecalculationPromptVisible` is `true`
- **THEN** `RouteRecalculationPrompt` is rendered above `TripTotalSummary`, displaying the recalculation message and call-to-action

#### Scenario: Prompt hidden when route is complete or fewer than 2 stops
- **WHEN** `uiState.isRouteRecalculationPromptVisible` is `false`
- **THEN** `RouteRecalculationPrompt` is not rendered

#### Scenario: Tapping the prompt dispatches the existing calculate-route intent
- **WHEN** the user taps `RouteRecalculationPrompt` while it is enabled
- **THEN** `OnCalculateRouteClicked` is dispatched (the same intent dispatched by the "Calculate route" button)

#### Scenario: Prompt disabled while calculating or loading
- **WHEN** `uiState.isCalculatingRoute` is `true`, or `uiState.isLoading` is `true`
- **THEN** `RouteRecalculationPrompt` is rendered with `isEnabled = false` and tapping it does not dispatch `OnCalculateRouteClicked`
