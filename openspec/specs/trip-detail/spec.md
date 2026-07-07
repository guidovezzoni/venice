# Trip Detail

## Purpose

Defines the requirements for the trip detail screen, covering the MVI contract (state, intents, effects), ViewModel behaviour, UI composables, and navigation wiring.

## Requirements

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
  or a stale value. Formatting and completeness are computed entirely in the ViewModel; the
  composable performs no arithmetic, locale detection, or string-resource resolution.

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, `legs` is empty, `isCalculatingRoute` is `false`, `routeError` is `null`, and `formattedTotalDistance` is `null`

#### Scenario: Total distance present when route is complete
- **WHEN** `legs.size == stops.size - 1` and `stops.size >= 2`
- **THEN** `formattedTotalDistance` is a non-null string equal to `formatDistance` applied to the sum of every leg's `distanceMetres`

#### Scenario: Total distance unavailable when route is incomplete or absent
- **WHEN** `legs` is empty, or `legs.size` is neither `0` nor `stops.size - 1`, or `stops.size < 2`
- **THEN** `formattedTotalDistance` is `null`

### Requirement: TripDetailUiIntent models user actions
`TripDetailUiIntent` SHALL be a sealed class with:
- `OnSetStartingPointClicked` — user taps the set/change starting point button
- `OnStartingPointConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the starting point stub dialog
- `OnDismissStartingPointDialog` — user dismisses the starting point dialog
- `OnSetDestinationClicked` — user taps the set/change destination button
- `OnDestinationConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the destination stub dialog
- `OnDismissDestinationDialog` — user dismisses the destination dialog
- `OnAddStopClicked` — user taps the "Add Stop" button
- `OnAddStopConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the add stop dialog
- `OnDismissAddStopDialog` — user dismisses the add stop dialog
- `OnMoveStopUp(stopId: String, currentOrder: Int)` — user taps the move-up button on an intermediate stop
- `OnMoveStopDown(stopId: String, currentOrder: Int)` — user taps the move-down button on an intermediate stop
- `OnEditStopClicked(stop: Stop)` — user taps an intermediate stop card to edit it
- `OnEditStopConfirmed(stopId: String, placeName: String, latitude: Double, longitude: Double)` — user confirms the edit stop dialog
- `OnDismissEditStopDialog` — user dismisses the edit stop dialog
- `OnRemoveStopClicked(stop: Stop)` — user taps the delete button on any stop card
- `OnRemoveStopConfirmed` — user confirms the removal dialog
- `OnDismissRemoveStopDialog` — user dismisses the removal dialog
- `OnMarkStopDepartedClicked(stopId: String)` — user taps "Mark as departed" on the current stop
- `OnUndoMarkStopDepartedClicked(stopId: String)` — user taps "Undo" on the last departed stop
- `OnSearchQueryChanged(query: String)` — user types in the place name field during a stop dialog
- `OnSuggestionSelected(suggestion: PlaceSuggestion)` — user taps an autocomplete suggestion
- `OnCalculateRouteClicked` — user taps the "Calculate route" button

#### Scenario: All intents are representable
- **WHEN** a user action occurs on the trip detail screen
- **THEN** it maps to exactly one `TripDetailUiIntent` subclass

#### Scenario: Mark departed intent carries stop ID
- **WHEN** the user taps "Mark as departed" on a stop with `id = "s1"`
- **THEN** `OnMarkStopDepartedClicked("s1")` is dispatched

#### Scenario: Undo departed intent carries stop ID
- **WHEN** the user taps "Undo" on a stop with `id = "s2"`
- **THEN** `OnUndoMarkStopDepartedClicked("s2")` is dispatched

#### Scenario: Search query changed intent carries text
- **WHEN** the user types "Rom" in the place name field
- **THEN** `OnSearchQueryChanged("Rom")` is dispatched

#### Scenario: Suggestion selected intent carries suggestion
- **WHEN** the user taps a suggestion with `placeId = "abc"`, `primaryText = "Rome"`, `secondaryText = "Italy"`
- **THEN** `OnSuggestionSelected(PlaceSuggestion("abc", "Rome", "Italy"))` is dispatched

#### Scenario: Calculate route intent
- **WHEN** the user taps the "Calculate route" button
- **THEN** `OnCalculateRouteClicked` is dispatched

### Requirement: TripDetailUiEffect models one-shot side effects
`TripDetailUiEffect` SHALL be a sealed class with:
- `ShowError(message: String)` — displayed as a snackbar

#### Scenario: Error effect carries message
- **WHEN** `ShowError("Failed")` is created
- **THEN** its `message` property is `"Failed"`

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
- On initialisation, also maintain an independent `combine(ObserveStopsUseCase(tripId), ObserveLegsUseCase(tripId))` collector that computes and updates `formattedTotalDistance` on every emission from either source:
  - Completeness: `legs.size == stops.size - 1 && stops.size >= 2`.
  - When complete: sum `leg.distanceMetres` across all `legs`, format the sum via the existing `formatDistance(sum, Locale.getDefault(), application.resources)`, and set `formattedTotalDistance` to the formatted string.
  - When not complete: set `formattedTotalDistance = null`.
  - This collector is independent of, and does not alter, the existing stops-only collector (`startingPoint`, `destination`, `intermediateStops`, `canAddMoreStops`, `formattedStopCoordinates`) or the existing legs-only collector (`legs`, `formattedLegDistances`, `formattedLegDurations`).

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

### Requirement: Stop sections use consolidated StopSection composable
The trip detail screen SHALL use `StopSection` (defined in destination-ui spec) for both the starting point and destination sections, parameterised with the appropriate icon, labels, and string resources for each.

#### Scenario: Starting point section uses StopSection
- **WHEN** the starting point section is rendered
- **THEN** it uses `StopSection` with `TripOrigin` icon and starting point string resources

#### Scenario: Destination section uses StopSection
- **WHEN** the destination section is rendered
- **THEN** it uses `StopSection` with `Place` icon and destination string resources

### Requirement: Stop dialogs use consolidated SetStopDialog composable
The trip detail screen SHALL use `SetStopDialog` (defined in destination-ui spec) for both the starting point and destination dialogs, parameterised with the appropriate title, hint, and error string resources for each.

### Requirement: TripDetailScreen integrates all components
`TripDetailScreen` SHALL:
- Accept `uiState` and `onIntent` parameters.
- Render `StopSection` for the starting point (with `TripOrigin` icon) below the trip title, passing `isLoading = uiState.isLoading`.
- Render intermediate stops between the starting point and destination sections. Each intermediate stop SHALL be rendered using `StopSection` with `LocationOn` icon, passing `isLoading = uiState.isLoading`. Tapping an intermediate stop card SHALL dispatch `OnEditStopClicked(stop)`.
- Render an "Add Stop" button (e.g., `OutlinedButton` or `IconButton` with `Icons.Default.AddLocation`) between the last intermediate stop and the destination section. The button SHALL be visible only when `canAddMoreStops` is `true`. The button SHALL be disabled when `isLoading` is `true`. Tapping it SHALL dispatch `OnAddStopClicked`.
- Render `StopSection` for the destination (with `Place` icon) below the intermediate stops and add-stop button, passing `isLoading = uiState.isLoading`.
- Show `SetStopDialog` for the starting point when `isSetStartingPointDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, search-related state (`placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail`), and search callbacks (`onSearchQueryChanged`, `onSuggestionSelected`). If `uiState.startingPoint` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for the destination when `isSetDestinationDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, search-related state and callbacks. If `uiState.destination` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for adding an intermediate stop when `isAddStopDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, search-related state and callbacks, with empty initial values.
- Show `SetStopDialog` for editing an intermediate stop when `isEditStopDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, search-related state and callbacks, with `editingStop`'s `placeName`, `latitude`, and `longitude` as initial values and the edit stop dialog title string resource. On confirm, dispatch `OnEditStopConfirmed` with the `editingStop`'s `id` and the entered values.
- Show the remove-stop confirmation dialog when `isRemoveStopDialogVisible` is `true`. The confirm button SHALL be disabled when `isLoading` is `true`.
- Consume `uiEffect` to show a snackbar on `ShowError`.
- The "Add Stop" button SHALL have a `contentDescription` for accessibility.

#### Scenario: Screen renders starting point, intermediate stops, and destination sections
- **WHEN** the trip detail screen is displayed with a starting point, two intermediate stops, and a destination
- **THEN** the starting point section is visible, followed by the two intermediate stops in order, followed by the "Add Stop" button, followed by the destination section

#### Scenario: Tapping intermediate stop opens edit dialog
- **WHEN** the user taps an existing intermediate stop card
- **THEN** the edit stop dialog opens pre-populated with the stop's place name, latitude, and longitude

#### Scenario: Edit dialog uses edit-specific title
- **WHEN** the edit stop dialog is visible
- **THEN** the dialog title is "Edit stop" (from `trip_detail_edit_stop_dialog_title` string resource)

#### Scenario: Edit dialog confirm dispatches OnEditStopConfirmed
- **WHEN** the user confirms the edit stop dialog with valid values
- **THEN** `OnEditStopConfirmed` is dispatched with the editing stop's ID and the entered place name, latitude, and longitude

#### Scenario: Add stop button visible when under limit
- **WHEN** the trip has fewer than 25 stops
- **THEN** the "Add Stop" button is visible between the intermediate stops and the destination

#### Scenario: Add stop button hidden at limit
- **WHEN** the trip has 25 stops
- **THEN** the "Add Stop" button is not visible

#### Scenario: Add stop button disabled while loading
- **WHEN** `isLoading` is `true` and `canAddMoreStops` is `true`
- **THEN** the "Add Stop" button is visible but disabled

#### Scenario: Starting point dialog pre-populated when editing
- **WHEN** the user taps an existing starting point and the dialog opens
- **THEN** the dialog fields are pre-populated with the starting point's place name, latitude, and longitude

#### Scenario: Destination dialog pre-populated when editing
- **WHEN** the user taps an existing destination and the dialog opens
- **THEN** the dialog fields are pre-populated with the destination's place name, latitude, and longitude

#### Scenario: Add stop dialog opens with empty fields
- **WHEN** the user taps the "Add Stop" button
- **THEN** a `SetStopDialog` opens with empty place name, latitude, and longitude fields

#### Scenario: Snackbar shown on error
- **WHEN** a `ShowError` effect is emitted
- **THEN** a snackbar with the error message is displayed

#### Scenario: Remove stop confirm button disabled while loading
- **WHEN** `isRemoveStopDialogVisible` is `true` and `isLoading` is `true`
- **THEN** the remove-stop confirmation dialog's confirm button is disabled

#### Scenario: StopSection action buttons disabled while loading
- **WHEN** the trip detail screen is rendered with `isLoading = true`
- **THEN** all action buttons in all `StopSection` instances (move up, move down, delete, mark departed, undo departed) are disabled

#### Scenario: Stop dialogs pass search state and callbacks
- **WHEN** any stop dialog is visible
- **THEN** the `SetStopDialog` receives `placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail` from `uiState`, and `onSearchQueryChanged`/`onSuggestionSelected` callbacks that dispatch the corresponding intents

### Requirement: Stop sections render reorder buttons for intermediate stops
`StopSection` SHALL accept optional `onMoveUp: (() -> Unit)?` and `onMoveDown: (() -> Unit)?` lambda parameters (defaulting to `null`). When `onMoveUp` is non-null, an `IconButton` with `Icons.Filled.KeyboardArrowUp` SHALL be rendered with content description `trip_detail_move_stop_up`. When `onMoveDown` is non-null, an `IconButton` with `Icons.Filled.KeyboardArrowDown` SHALL be rendered with content description `trip_detail_move_stop_down`.

#### Scenario: Move-up button shown when lambda provided
- **WHEN** `StopSection` is rendered with a non-null `onMoveUp` lambda
- **THEN** an up-arrow `IconButton` is visible with content description "Move up"

#### Scenario: Move-down button shown when lambda provided
- **WHEN** `StopSection` is rendered with a non-null `onMoveDown` lambda
- **THEN** a down-arrow `IconButton` is visible with content description "Move down"

#### Scenario: No buttons when lambdas are null
- **WHEN** `StopSection` is rendered with `onMoveUp = null` and `onMoveDown = null`
- **THEN** no reorder buttons are visible

### Requirement: TripDetailScreen passes reorder lambdas for intermediate stops
`TripDetailScreen` SHALL pass `onMoveUp` and `onMoveDown` lambdas to `StopSection` for each intermediate stop based on its position:
- The first intermediate stop (lowest order among intermediates) SHALL receive `onMoveUp = null` and `onMoveDown` dispatching `OnMoveStopDown`.
- The last intermediate stop (highest order among intermediates) SHALL receive `onMoveUp` dispatching `OnMoveStopUp` and `onMoveDown = null`.
- Middle intermediate stops SHALL receive both `onMoveUp` and `onMoveDown` lambdas.
- When there are fewer than 2 intermediate stops, no reorder lambdas SHALL be passed.
- Starting point and destination sections SHALL never receive reorder lambdas.

#### Scenario: Single intermediate stop — no reorder buttons
- **WHEN** there is exactly 1 intermediate stop
- **THEN** `StopSection` for that stop receives `onMoveUp = null` and `onMoveDown = null`

#### Scenario: Two intermediate stops — limited buttons
- **WHEN** there are 2 intermediate stops
- **THEN** the first intermediate receives only `onMoveDown` and the second receives only `onMoveUp`

#### Scenario: Three intermediate stops — full buttons for middle
- **WHEN** there are 3 intermediate stops
- **THEN** the first receives only `onMoveDown`, the middle receives both `onMoveUp` and `onMoveDown`, and the last receives only `onMoveUp`

#### Scenario: Starting point never has reorder buttons
- **WHEN** the trip detail screen is rendered
- **THEN** the starting point `StopSection` receives `onMoveUp = null` and `onMoveDown = null`

#### Scenario: Destination never has reorder buttons
- **WHEN** the trip detail screen is rendered
- **THEN** the destination `StopSection` receives `onMoveUp = null` and `onMoveDown = null`

### Requirement: Stop sections render delete button for filled stops
`StopSection` SHALL accept an optional `onDelete: (() -> Unit)?` lambda parameter (defaulting to `null`). When `onDelete` is non-null and the stop is filled (non-null), an `IconButton` with `Icons.Outlined.Delete` SHALL be rendered with content description from string resource `trip_detail_remove_stop`.

#### Scenario: Delete button shown when lambda provided and stop is filled
- **WHEN** `StopSection` is rendered with a non-null `onDelete` lambda and a non-null stop
- **THEN** a delete `IconButton` is visible with content description "Remove stop"

#### Scenario: No delete button when lambda is null
- **WHEN** `StopSection` is rendered with `onDelete = null`
- **THEN** no delete button is visible

#### Scenario: No delete button when stop is empty
- **WHEN** `StopSection` is rendered with `onDelete` non-null but `stop = null`
- **THEN** no delete button is visible (the empty-state button is shown instead)

### Requirement: TripDetailScreen passes delete lambdas and shows confirmation dialog
`TripDetailScreen` SHALL:
- Pass `onDelete` lambda to all `StopSection` instances (starting point, intermediates, destination) that dispatches `OnRemoveStopClicked(stop)` for the corresponding stop.
- Show a confirmation `AlertDialog` when `isRemoveStopDialogVisible` is `true`, displaying:
  - Title from string resource `trip_detail_remove_stop_dialog_title`
  - Message from string resource `trip_detail_remove_stop_dialog_message` formatted with `stopToRemove?.placeName`
  - Confirm button with text from string resource `global_remove` that dispatches `OnRemoveStopConfirmed`
  - Dismiss button with text from string resource `global_cancel` that dispatches `OnDismissRemoveStopDialog`

#### Scenario: Delete button on starting point dispatches OnRemoveStopClicked
- **WHEN** the user taps the delete button on the starting point stop card
- **THEN** `OnRemoveStopClicked` is dispatched with the starting point stop

#### Scenario: Delete button on intermediate stop dispatches OnRemoveStopClicked
- **WHEN** the user taps the delete button on an intermediate stop card
- **THEN** `OnRemoveStopClicked` is dispatched with that intermediate stop

#### Scenario: Delete button on destination dispatches OnRemoveStopClicked
- **WHEN** the user taps the delete button on the destination stop card
- **THEN** `OnRemoveStopClicked` is dispatched with the destination stop

#### Scenario: Confirmation dialog displays stop name
- **WHEN** `isRemoveStopDialogVisible` is `true` and `stopToRemove` has `placeName = "Florence, Italy"`
- **THEN** the confirmation dialog message includes "Florence, Italy"

#### Scenario: Confirming deletion dismisses dialog
- **WHEN** the user taps the confirm button on the removal dialog
- **THEN** `OnRemoveStopConfirmed` is dispatched

#### Scenario: Cancelling deletion dismisses dialog
- **WHEN** the user taps the cancel button on the removal dialog
- **THEN** `OnDismissRemoveStopDialog` is dispatched

### Requirement: Remove stop string resources
The app SHALL include the following string resources in EN, IT, and ES:
- `trip_detail_remove_stop`: "Remove stop" / "Rimuovi tappa" / "Eliminar parada"
- `trip_detail_remove_stop_dialog_title`: "Remove stop?" / "Rimuovere la tappa?" / "Eliminar parada?"
- `trip_detail_remove_stop_dialog_message`: "Are you sure you want to remove \"%s\"?" / "Vuoi davvero rimuovere \"%s\"?" / "Estas seguro de que quieres eliminar \"%s\"?"
- `trip_detail_remove_stop_error`: "Failed to remove stop. Please try again." / "Impossibile rimuovere la tappa. Riprova." / "No se pudo eliminar la parada. Intentalo de nuevo."
- `global_remove`: "Remove" / "Rimuovi" / "Eliminar"

#### Scenario: English strings present
- **WHEN** the app locale is English
- **THEN** all five remove-related string resources resolve to their English values

#### Scenario: Italian strings present
- **WHEN** the app locale is Italian
- **THEN** all five remove-related string resources resolve to their Italian values

#### Scenario: Spanish strings present
- **WHEN** the app locale is Spanish
- **THEN** all five remove-related string resources resolve to their Spanish values

### Requirement: TripDetailScreen wired in navigation
`MainScreen` SHALL wire `TripDetailViewModel` via `hiltViewModel()` in the `ROUTE_TRIP_DETAIL` composable destination, passing `uiState` and `onIntent` to `TripDetailScreen`.

#### Scenario: Navigation to trip detail creates ViewModel
- **WHEN** the user navigates to a trip's detail screen
- **THEN** `TripDetailViewModel` is created with the trip's ID from the route arguments

### Requirement: StopSection visually differentiates stop status
`StopSection` SHALL accept an optional `stopDisplayState` parameter indicating `DEPARTED`, `CURRENT`, or `UPCOMING` (defaulting to `UPCOMING`). Visual styling SHALL vary:
- **DEPARTED**: Icon uses `MaterialTheme.colorScheme.onSurfaceVariant` tint. Card has reduced emphasis (alpha `0.6f` on the icon and label).
- **CURRENT**: Icon uses `MaterialTheme.colorScheme.primary` tint (existing style). Card is styled with default emphasis.
- **UPCOMING**: Icon uses `MaterialTheme.colorScheme.onSurfaceVariant` tint. Card is styled with default emphasis.

#### Scenario: Departed stop shows muted styling
- **WHEN** `StopSection` is rendered with `stopDisplayState = DEPARTED`
- **THEN** the icon tint is `onSurfaceVariant` and icon/label have alpha `0.6f`

#### Scenario: Current stop shows primary styling
- **WHEN** `StopSection` is rendered with `stopDisplayState = CURRENT`
- **THEN** the icon tint is `primary` and no alpha reduction is applied

#### Scenario: Upcoming stop shows default styling
- **WHEN** `StopSection` is rendered with `stopDisplayState = UPCOMING`
- **THEN** the icon tint is `onSurfaceVariant` and no alpha reduction is applied

### Requirement: StopSection renders mark departed action button
`StopSection` SHALL accept an optional `onMarkDeparted: (() -> Unit)?` lambda (defaulting to `null`). When non-null, a button labelled with string resource `trip_detail_mark_departed` SHALL be rendered.

#### Scenario: Mark departed button shown when lambda provided
- **WHEN** `StopSection` is rendered with a non-null `onMarkDeparted` lambda
- **THEN** a "Mark as departed" button is visible

#### Scenario: Mark departed button hidden when lambda is null
- **WHEN** `StopSection` is rendered with `onMarkDeparted = null`
- **THEN** no "Mark as departed" button is visible

#### Scenario: Tapping mark departed fires callback
- **WHEN** the user taps the "Mark as departed" button
- **THEN** the `onMarkDeparted` lambda is invoked

### Requirement: StopSection renders undo departed action button
`StopSection` SHALL accept an optional `onUndoDeparted: (() -> Unit)?` lambda (defaulting to `null`). When non-null, a button labelled with string resource `trip_detail_undo_departed` SHALL be rendered.

#### Scenario: Undo button shown when lambda provided
- **WHEN** `StopSection` is rendered with a non-null `onUndoDeparted` lambda
- **THEN** an "Undo departure" button is visible

#### Scenario: Undo button hidden when lambda is null
- **WHEN** `StopSection` is rendered with `onUndoDeparted = null`
- **THEN** no "Undo departure" button is visible

#### Scenario: Tapping undo fires callback
- **WHEN** the user taps the "Undo departure" button
- **THEN** the `onUndoDeparted` lambda is invoked

### Requirement: TripDetailScreen passes mark/undo lambdas based on stop status
`TripDetailScreen` SHALL derive the display state and action lambdas for each stop:
- The **current stop** (first PENDING by order across all stops) SHALL receive `onMarkDeparted` dispatching `OnMarkStopDepartedClicked(stop.id)` and `stopDisplayState = CURRENT`.
- The **last departed stop** (highest-order VISITED stop) SHALL receive `onUndoDeparted` dispatching `OnUndoMarkStopDepartedClicked(stop.id)` and `stopDisplayState = DEPARTED`.
- All other VISITED stops SHALL receive `stopDisplayState = DEPARTED` with no action buttons for mark/undo.
- All other PENDING stops (after the current one) SHALL receive `stopDisplayState = UPCOMING` with no action buttons for mark/undo.

#### Scenario: Current stop gets mark departed button
- **WHEN** the trip has stops [VISITED(order=0), PENDING(order=1), PENDING(order=2)]
- **THEN** the stop at order 1 is rendered with `stopDisplayState = CURRENT` and `onMarkDeparted` set

#### Scenario: Last departed stop gets undo button
- **WHEN** the trip has stops [VISITED(order=0), VISITED(order=1), PENDING(order=2)]
- **THEN** the stop at order 1 is rendered with `stopDisplayState = DEPARTED` and `onUndoDeparted` set

#### Scenario: First stop as current when none departed
- **WHEN** all stops are PENDING
- **THEN** the stop at order 0 (starting point) is rendered with `stopDisplayState = CURRENT` and `onMarkDeparted` set

#### Scenario: All stops departed — no action buttons
- **WHEN** all stops have `status = VISITED`
- **THEN** all stops are rendered with `stopDisplayState = DEPARTED`, no `onMarkDeparted`, and only the last one gets `onUndoDeparted`

### Requirement: Stop progress string resources
The app SHALL include the following string resources in EN and IT:
- `trip_detail_mark_departed`: "Mark as departed" / "Segna come partito"
- `trip_detail_undo_departed`: "Undo departure" / "Annulla partenza"

#### Scenario: English strings present
- **WHEN** the app locale is English
- **THEN** `trip_detail_mark_departed` resolves to "Mark as departed" and `trip_detail_undo_departed` resolves to "Undo departure"

#### Scenario: Italian strings present
- **WHEN** the app locale is Italian
- **THEN** `trip_detail_mark_departed` resolves to "Segna come partito" and `trip_detail_undo_departed` resolves to "Annulla partenza"

### Requirement: TripDetailScreen passes resolving state to dialogs
`TripDetailScreen` SHALL pass `isResolvingPlace` and `placeDetailError` from `TripDetailUiState` to all `SetStopDialog` invocations (starting point, destination, add stop, edit stop).

#### Scenario: All dialog call sites receive resolving state
- **WHEN** `TripDetailScreen` renders any `SetStopDialog`
- **THEN** it passes `uiState.isResolvingPlace` as `isResolvingPlace` and `uiState.placeDetailError` as `placeDetailError`

### Requirement: Place detail error string resource
A string resource SHALL be defined for the place detail error message at `R.string.trip_detail_place_detail_error` with the value "Could not resolve place. Tap a suggestion to try again."

#### Scenario: Error string is available
- **WHEN** the app references `R.string.trip_detail_place_detail_error`
- **THEN** the string "Could not resolve place. Tap a suggestion to try again." is returned

### Requirement: Place search string resources
The app SHALL include the following string resources in EN, IT, and ES:
- `trip_detail_search_no_results`: "No results found" / "Nessun risultato trovato" / "No se encontraron resultados"
- `trip_detail_search_unavailable`: "Search unavailable" / "Ricerca non disponibile" / "Busqueda no disponible"
- `trip_detail_search_loading`: "Searching…" / "Ricerca in corso…" / "Buscando…"

#### Scenario: English strings present
- **WHEN** the app locale is English
- **THEN** all search-related string resources resolve to their English values

#### Scenario: Italian strings present
- **WHEN** the app locale is Italian
- **THEN** all search-related string resources resolve to their Italian values

#### Scenario: Spanish strings present
- **WHEN** the app locale is Spanish
- **THEN** all search-related string resources resolve to their Spanish values

### Requirement: TripDetailScreen renders calculate route button
The trip detail screen SHALL display a "Calculate route" button when at least 2 stops exist (starting point and destination both non-null). The button SHALL:
- Be disabled when `isCalculatingRoute` is `true` or `isLoading` is `true`.
- Show a `CircularProgressIndicator` when `isCalculatingRoute` is `true`.
- Dispatch `OnCalculateRouteClicked` when tapped.
- Be placed after the destination section and before any other content.

#### Scenario: Button visible with 2+ stops
- **WHEN** the screen has a starting point and a destination
- **THEN** the "Calculate route" button is visible

#### Scenario: Button hidden with fewer than 2 stops
- **WHEN** the screen has only a starting point (no destination)
- **THEN** the "Calculate route" button is not visible

#### Scenario: Button disabled while calculating
- **WHEN** `isCalculatingRoute` is `true`
- **THEN** the button is disabled and shows a loading indicator

#### Scenario: Button tap dispatches intent
- **WHEN** the user taps the "Calculate route" button
- **THEN** `OnCalculateRouteClicked` is dispatched

### Requirement: TripDetailScreen renders route error
The trip detail screen SHALL display an inline error message when `routeError` is not null. The error SHALL be displayed near the calculate route button.

#### Scenario: Error displayed
- **WHEN** `routeError` is "Network error"
- **THEN** the text "Route calculation failed: Network error" is displayed

#### Scenario: No error
- **WHEN** `routeError` is `null`
- **THEN** no error message is displayed

### Requirement: TripDetailScreen renders leg summaries between stops
The trip detail screen SHALL render a `LegSummary` composable between each pair of consecutive stops when legs are available. The leg for a given pair is matched by `fromStopId` and `toStopId`. The screen SHALL additionally render a `TripTotalSummary` composable, defined in `ui/screens/tripdetail/TripTotalSummary.kt`, as a `LazyColumn` item placed after the calculate-route button / route-error item block:
- `TripTotalSummary` accepts `modifier: Modifier = Modifier` and `formattedTotalDistance: String?`.
- When `formattedTotalDistance` is non-null, it displays the `trip_detail_total_distance_label` text alongside the value.
- When `formattedTotalDistance` is `null`, it displays the `trip_detail_total_distance_unavailable` text instead.
- `TripTotalSummary` performs no locale detection, unit conversion, string-resource-driven arithmetic, or business-logic branching beyond the null-fallback render — it is purely presentational, mirroring `LegSummary` and `TripProgressSummary`.

#### Scenario: Legs displayed between stops
- **WHEN** 3 stops exist and 2 legs are available
- **THEN** a `LegSummary` is rendered between stop 1 and stop 2, and between stop 2 and stop 3

#### Scenario: No legs available
- **WHEN** 3 stops exist but no legs are available
- **THEN** no `LegSummary` composables are rendered

#### Scenario: Total distance summary displayed with a value
- **WHEN** `uiState.formattedTotalDistance` is `"12.5 km"`
- **THEN** `TripTotalSummary` displays the `trip_detail_total_distance_label` text and the value `"12.5 km"`

#### Scenario: Total distance summary displayed as unavailable
- **WHEN** `uiState.formattedTotalDistance` is `null`
- **THEN** `TripTotalSummary` displays the `trip_detail_total_distance_unavailable` text and no numeric value

### Requirement: TripDetailScreen route preview coverage
The trip detail screen previews SHALL include:
- A preview with legs displayed between stops
- A preview with `isCalculatingRoute = true`
- A preview with `routeError` set
- A preview with a non-null `formattedTotalDistance` (satisfying non-default `UiState`-field preview coverage)
- A preview with `formattedTotalDistance = null` alongside a populated stop/leg set, showing the unavailable state

`TripTotalSummary` SHALL itself have previews covering both the value-present and value-absent (unavailable) states, each wrapped in `HeadingToVeniceTheme` with `showBackground = true`.

#### Scenario: Route previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** preview variants for legs, calculating state, error state, total-present state, and total-unavailable state are all visible

#### Scenario: TripTotalSummary previews exist
- **WHEN** `TripTotalSummary` is inspected in Android Studio
- **THEN** at least two preview variants are visible: one with a non-null `formattedTotalDistance` and one with `null`
