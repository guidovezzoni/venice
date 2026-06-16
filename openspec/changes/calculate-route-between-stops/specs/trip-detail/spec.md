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

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, `legs` is empty, `isCalculatingRoute` is `false`, `routeError` is `null`

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
- On initialisation, collect `ObserveLegsUseCase(tripId)` and update `legs` in the UI state.
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
- On `OnCalculateRouteClicked`: set `isCalculatingRoute = true` and clear `routeError`; build the ordered list of all stops (starting point + intermediate + destination); call `CalculateRouteUseCase(tripId, stops)` with `withMinimumDuration`; on success set `isCalculatingRoute = false` (legs update via `ObserveLegsUseCase` Flow); on failure set `isCalculatingRoute = false` and set `routeError` to the error message.

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

## ADDED Requirements

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
The trip detail screen SHALL render a `LegSummary` composable between each pair of consecutive stops when legs are available. The leg for a given pair is matched by `fromStopId` and `toStopId`.

#### Scenario: Legs displayed between stops
- **WHEN** 3 stops exist and 2 legs are available
- **THEN** a `LegSummary` is rendered between stop 1 and stop 2, and between stop 2 and stop 3

#### Scenario: No legs available
- **WHEN** 3 stops exist but no legs are available
- **THEN** no `LegSummary` composables are rendered

### Requirement: TripDetailScreen route preview coverage
The trip detail screen previews SHALL include:
- A preview with legs displayed between stops
- A preview with `isCalculatingRoute = true`
- A preview with `routeError` set

#### Scenario: Route previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** preview variants for legs, calculating state, and error state are visible
