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

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`

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

#### Scenario: All intents are representable
- **WHEN** a user action occurs on the trip detail screen
- **THEN** it maps to exactly one `TripDetailUiIntent` subclass

#### Scenario: Search query changed intent carries text
- **WHEN** the user types "Rom" in the place name field
- **THEN** `OnSearchQueryChanged("Rom")` is dispatched

#### Scenario: Suggestion selected intent carries suggestion
- **WHEN** the user taps a suggestion with `placeId = "abc"`, `primaryText = "Rome"`, `secondaryText = "Italy"`
- **THEN** `OnSuggestionSelected(PlaceSuggestion("abc", "Rome", "Italy"))` is dispatched

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
- All async operations SHALL use `withMinimumDuration { ... }` (which defaults to 500 ms) to ensure `isLoading` remains `true` for at least 500 ms, even if the underlying operation completes faster.
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
- On `OnSearchQueryChanged`: cancel any pending search job; if query (trimmed) is blank, clear `placeSuggestions`, `isSearchingPlaces`, and `searchError`; otherwise launch a new coroutine that delays 300 ms, sets `isSearchingPlaces = true`, calls `SearchPlacesUseCase(query)`, and on success updates `placeSuggestions` and clears `isSearchingPlaces` and `searchError`, on failure sets `searchError` and clears `isSearchingPlaces` and `placeSuggestions`.
- On `OnSuggestionSelected`: clear `placeSuggestions`; set `isSearchingPlaces = false`; launch a coroutine that calls `GetPlaceDetailUseCase(suggestion.placeId)`; on success set `selectedPlaceDetail` to the result; on failure emit `ShowError`. After the composable consumes `selectedPlaceDetail`, the ViewModel SHALL clear it (set to `null`).

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

#### Scenario: Suggestion selected but detail fetch fails
- **WHEN** `OnSuggestionSelected` is dispatched and `GetPlaceDetailUseCase` returns failure
- **THEN** a `ShowError` effect is emitted and `selectedPlaceDetail` remains `null`

#### Scenario: Dialog dismiss clears search state
- **WHEN** any dialog dismiss intent is dispatched (e.g. `OnDismissStartingPointDialog`)
- **THEN** `placeSuggestions` is cleared, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, the search job is cancelled, and `PlaceSearchRepository.resetSession()` is called

#### Scenario: Search job cancelled on new query
- **WHEN** `OnSearchQueryChanged("Rome")` is dispatched while a previous search for "Rom" is still debouncing
- **THEN** the previous search job is cancelled and only "Rome" triggers a use case call

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

#### Scenario: Stop dialogs pass search state and callbacks
- **WHEN** any stop dialog is visible
- **THEN** the `SetStopDialog` receives `placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail` from `uiState`, and `onSearchQueryChanged`/`onSuggestionSelected` callbacks that dispatch the corresponding intents

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
