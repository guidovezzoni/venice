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

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`

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
- On `OnEditStopClicked(stop)`: set `editingStop = stop` and `isEditStopDialogVisible = true`.
- On `OnDismissEditStopDialog`: set `editingStop = null` and `isEditStopDialogVisible = false`, clear search state, cancel search job, call `PlaceSearchRepository.resetSession()`.
- On `OnEditStopConfirmed`: set `isLoading = true`; call `EditStopUseCase`; on success clear edit state, dismiss dialog, set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnRemoveStopClicked(stop)`: set `stopToRemove = stop` and `isRemoveStopDialogVisible = true`.
- On `OnDismissRemoveStopDialog`: set `stopToRemove = null` and `isRemoveStopDialogVisible = false`.
- On `OnRemoveStopConfirmed`: set `isLoading = true`; call `RemoveStopUseCase`; on success clear remove state, dismiss dialog, set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMoveStopUp/Down`: set `isLoading = true`; call `MoveStopUseCase`; on failure emit `ShowError`; set `isLoading = false`.
- On `OnMarkStopDepartedClicked(stopId)`: set `isLoading = true`; call `MarkStopDepartedUseCase(tripId, stopId)`; on failure emit `ShowError`; set `isLoading = false`.
- On `OnUndoMarkStopDepartedClicked`: set `isLoading = true`; call `UndoMarkStopDepartedUseCase(tripId)`; on failure emit `ShowError`; set `isLoading = false`.
- On `OnSearchQueryChanged(query)`: cancel any pending search job; if query is blank, call `clearSearchState()`; otherwise set `isSearchingPlaces = true`, clear `placeDetailError`, debounce 300ms, call `SearchPlacesUseCase`; on success update `placeSuggestions`, clear `searchError`, set `isSearchingPlaces = false`; on failure set `searchError`, clear `placeSuggestions`, set `isSearchingPlaces = false`.
- On `OnSuggestionSelected(suggestion)`: set `isResolvingPlace = true` and clear `placeDetailError`; call `GetPlaceDetailUseCase(suggestion.placeId)`; on success set `selectedPlaceDetail`, clear `placeSuggestions`, set `isResolvingPlace = false`; on failure set `placeDetailError` with the error message, set `isResolvingPlace = false`. SHALL NOT emit `ShowError` effect on Place Details failure.
- `clearSearchState()` SHALL reset `placeSuggestions` to empty, `isSearchingPlaces` to `false`, `searchError` to `null`, `selectedPlaceDetail` to `null`, `isResolvingPlace` to `false`, and `placeDetailError` to `null`.

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

#### Scenario: Dialog dismiss clears resolving state
- **WHEN** any dismiss dialog intent is dispatched
- **THEN** `isResolvingPlace` is `false` and `placeDetailError` is `null`

### Requirement: SetStopDialog composable
`SetStopDialog` SHALL accept the following parameters:
- `modifier: Modifier` (default `Modifier`)
- `isLoading: Boolean` (default `false`)
- `isResolvingPlace: Boolean` (default `false`)
- `@StringRes dialogTitleRes: Int`
- `@StringRes placeNameHintRes: Int`
- `@StringRes placeNameErrorRes: Int`
- `@StringRes latitudeHintRes: Int`
- `@StringRes latitudeErrorRes: Int`
- `@StringRes longitudeHintRes: Int`
- `@StringRes longitudeErrorRes: Int`
- `initialPlaceName: String` (default `""`)
- `initialLatitude: String` (default `""`)
- `initialLongitude: String` (default `""`)
- `suggestions: List<PlaceSuggestion>` (default `emptyList()`)
- `isSearchingPlaces: Boolean` (default `false`)
- `searchError: String?` (default `null`)
- `placeDetailError: String?` (default `null`)
- `selectedPlaceDetail: PlaceDetail?` (default `null`)
- `onSearchQueryChanged: (String) -> Unit` (default `{}`)
- `onSuggestionSelected: (PlaceSuggestion) -> Unit` (default `{}`)
- `onConfirm: (placeName: String, latitude: Double, longitude: Double) -> Unit`
- `onDismiss: () -> Unit`

The confirm button SHALL be disabled when `isLoading` is `true` OR `isResolvingPlace` is `true`.

When `isResolvingPlace` is `true`, a `CircularProgressIndicator` SHALL be displayed inside the form body with a semantics content description of "Resolving place".

When `placeDetailError` is not null, the error text SHALL be displayed inline in the form body using `MaterialTheme.colorScheme.error` styling, following the same pattern as `searchError`.

#### Scenario: Resolving place shows indicator and disables confirm
- **WHEN** `isResolvingPlace` is `true`
- **THEN** a progress indicator with content description "Resolving place" is visible and the confirm button is disabled

#### Scenario: Place detail error shown inline
- **WHEN** `placeDetailError` is set to a non-null value
- **THEN** the error message is displayed inline in the dialog

#### Scenario: Default state shows no indicator or error
- **WHEN** `isResolvingPlace` is `false` and `placeDetailError` is `null`
- **THEN** no resolving indicator or place detail error message is shown

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
