## MODIFIED Requirements

### Requirement: TripDetailUiState represents the screen state
`TripDetailUiState` SHALL be a data class with:
- `tripId: String` (default `""`)
- `startingPoint: Stop?` (default `null`)
- `destination: Stop?` (default `null`)
- `isLoading: Boolean` (default `false`)
- `isSetStartingPointDialogVisible: Boolean` (default `false`)
- `isSetDestinationDialogVisible: Boolean` (default `false`)

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`

### Requirement: TripDetailUiIntent models user actions
`TripDetailUiIntent` SHALL be a sealed class with:
- `OnSetStartingPointClicked` — user taps the set/change starting point button
- `OnStartingPointConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the starting point stub dialog
- `OnDismissStartingPointDialog` — user dismisses the starting point dialog
- `OnSetDestinationClicked` — user taps the set/change destination button
- `OnDestinationConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the destination stub dialog
- `OnDismissDestinationDialog` — user dismisses the destination dialog

#### Scenario: All intents are representable
- **WHEN** a user action occurs on the trip detail screen
- **THEN** it maps to exactly one `TripDetailUiIntent` subclass

### Requirement: TripDetailViewModel drives the screen
`TripDetailViewModel` SHALL:
- Accept `tripId` from `SavedStateHandle`.
- Expose `uiState: StateFlow<TripDetailUiState>` and `uiEffect: SharedFlow<TripDetailUiEffect>`.
- Provide `fun onIntent(intent: TripDetailUiIntent)`.
- On initialisation, collect `ObserveStopsUseCase(tripId)` and update `startingPoint` with the stop where `order = 0` (or `null` if absent) and `destination` with the stop where `order > 0` having the highest `order` (or `null` if absent).
- On `OnSetStartingPointClicked`: set `isSetStartingPointDialogVisible = true`.
- On `OnDismissStartingPointDialog`: set `isSetStartingPointDialogVisible = false`.
- On `OnStartingPointConfirmed`: call `SetStartingPointUseCase`; on success dismiss the dialog; on failure emit `ShowError`.
- On `OnSetDestinationClicked`: set `isSetDestinationDialogVisible = true`.
- On `OnDismissDestinationDialog`: set `isSetDestinationDialogVisible = false`.
- On `OnDestinationConfirmed`: call `SetDestinationUseCase`; on success dismiss the dialog; on failure emit `ShowError`.

#### Scenario: Opening starting point dialog
- **WHEN** `OnSetStartingPointClicked` is dispatched
- **THEN** `isSetStartingPointDialogVisible` becomes `true`

#### Scenario: Dismissing starting point dialog
- **WHEN** `OnDismissStartingPointDialog` is dispatched while dialog is visible
- **THEN** `isSetStartingPointDialogVisible` becomes `false`

#### Scenario: Confirming starting point — success
- **WHEN** `OnStartingPointConfirmed` is dispatched and the use case succeeds
- **THEN** `startingPoint` is updated and the dialog is dismissed

#### Scenario: Confirming starting point — failure
- **WHEN** `OnStartingPointConfirmed` is dispatched and the use case fails
- **THEN** a `ShowError` effect is emitted

#### Scenario: Opening destination dialog
- **WHEN** `OnSetDestinationClicked` is dispatched
- **THEN** `isSetDestinationDialogVisible` becomes `true`

#### Scenario: Dismissing destination dialog
- **WHEN** `OnDismissDestinationDialog` is dispatched while dialog is visible
- **THEN** `isSetDestinationDialogVisible` becomes `false`

#### Scenario: Confirming destination — success
- **WHEN** `OnDestinationConfirmed` is dispatched and the use case succeeds
- **THEN** `destination` is updated and the dialog is dismissed

#### Scenario: Confirming destination — failure
- **WHEN** `OnDestinationConfirmed` is dispatched and the use case fails
- **THEN** a `ShowError` effect is emitted

#### Scenario: Initialisation with existing starting point and destination
- **WHEN** the ViewModel initialises and stops with `order = 0` and `order = 1` exist
- **THEN** `startingPoint` reflects the `order = 0` stop and `destination` reflects the `order = 1` stop

#### Scenario: Initialisation with no stops
- **WHEN** the ViewModel initialises and no stops exist
- **THEN** `startingPoint` is `null` and `destination` is `null`

#### Scenario: Initialisation with only starting point
- **WHEN** the ViewModel initialises and only a stop with `order = 0` exists
- **THEN** `startingPoint` reflects that stop and `destination` is `null`

### Requirement: TripDetailScreen integrates all components
`TripDetailScreen` SHALL:
- Accept `uiState` and `onIntent` parameters.
- Render `StopSection` for the starting point (with `TripOrigin` icon) below the trip title.
- Render `StopSection` for the destination (with `Place` icon) below the starting point section.
- Show `SetStopDialog` for the starting point when `isSetStartingPointDialogVisible` is `true`. If `uiState.startingPoint` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for the destination when `isSetDestinationDialogVisible` is `true`. If `uiState.destination` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Consume `uiEffect` to show a snackbar on `ShowError`.

#### Scenario: Screen renders starting point and destination sections
- **WHEN** the trip detail screen is displayed
- **THEN** the starting point section is visible below the trip title and the destination section is visible below the starting point

#### Scenario: Starting point dialog pre-populated when editing
- **WHEN** the user taps an existing starting point and the dialog opens
- **THEN** the dialog fields are pre-populated with the starting point's place name, latitude, and longitude

#### Scenario: Destination dialog pre-populated when editing
- **WHEN** the user taps an existing destination and the dialog opens
- **THEN** the dialog fields are pre-populated with the destination's place name, latitude, and longitude

#### Scenario: Snackbar shown on error
- **WHEN** a `ShowError` effect is emitted
- **THEN** a snackbar with the error message is displayed
