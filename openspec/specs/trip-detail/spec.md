# Trip Detail

## Purpose

Defines the requirements for the trip detail screen, covering the MVI contract (state, intents, effects), ViewModel behaviour, UI composables, and navigation wiring.

## Requirements

### Requirement: TripDetailUiState represents the screen state
`TripDetailUiState` SHALL be a data class with:
- `tripId: String` (default `""`)
- `startingPoint: Stop?` (default `null`)
- `isLoading: Boolean` (default `false`)
- `isSetStartingPointDialogVisible: Boolean` (default `false`)

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`

### Requirement: TripDetailUiIntent models user actions
`TripDetailUiIntent` SHALL be a sealed class with:
- `OnSetStartingPointClicked` — user taps the set/change starting point button
- `OnStartingPointConfirmed(placeName: String, latitude: Double, longitude: Double)` — user confirms the stub dialog
- `OnDismissStartingPointDialog` — user dismisses the dialog

#### Scenario: All intents are representable
- **WHEN** a user action occurs on the trip detail screen
- **THEN** it maps to exactly one `TripDetailUiIntent` subclass

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
- On initialisation, collect `ObserveStopsUseCase(tripId)` and update `startingPoint` with the stop where `order = 0` (or `null` if absent).
- On `OnSetStartingPointClicked`: set `isSetStartingPointDialogVisible = true`.
- On `OnDismissStartingPointDialog`: set `isSetStartingPointDialogVisible = false`.
- On `OnStartingPointConfirmed`: call `SetStartingPointUseCase`; on success dismiss the dialog; on failure emit `ShowError`.

#### Scenario: Opening dialog
- **WHEN** `OnSetStartingPointClicked` is dispatched
- **THEN** `isSetStartingPointDialogVisible` becomes `true`

#### Scenario: Dismissing dialog
- **WHEN** `OnDismissStartingPointDialog` is dispatched while dialog is visible
- **THEN** `isSetStartingPointDialogVisible` becomes `false`

#### Scenario: Confirming starting point — success
- **WHEN** `OnStartingPointConfirmed` is dispatched and the use case succeeds
- **THEN** `startingPoint` is updated and the dialog is dismissed

#### Scenario: Confirming starting point — failure
- **WHEN** `OnStartingPointConfirmed` is dispatched and the use case fails
- **THEN** a `ShowError` effect is emitted

#### Scenario: Initialisation with existing starting point
- **WHEN** the ViewModel initialises and a stop with `order = 0` exists
- **THEN** `startingPoint` reflects that stop

#### Scenario: Initialisation with no stops
- **WHEN** the ViewModel initialises and no stops exist
- **THEN** `startingPoint` is `null`

### Requirement: StartingPointSection composable
`StartingPointSection` SHALL display:
- When `startingPoint` is `null`: a placeholder card with a "Set starting point" button.
- When `startingPoint` is non-null: a card showing the place name, coordinates, and a `TripOrigin` icon with "Start" label. Tapping it triggers `OnSetStartingPointClicked`.

The section SHALL include a `contentDescription` on the button for accessibility.

#### Scenario: Empty state shown
- **WHEN** `startingPoint` is `null`
- **THEN** a placeholder with "Set starting point" button is displayed

#### Scenario: Filled state shown
- **WHEN** `startingPoint` has `placeName = "Rome"` and coordinates
- **THEN** a card with "Rome", coordinates, and a "Start" label is displayed

#### Scenario: Tapping filled state triggers change
- **WHEN** the user taps the starting point card
- **THEN** `OnSetStartingPointClicked` is triggered

### Requirement: SetStartingPointDialog composable
`SetStartingPointDialog` SHALL display an `AlertDialog` with:
- Title: "Set starting point"
- Three `OutlinedTextField` inputs with labels: "Place name", "Latitude", "Longitude"
- Confirm and dismiss buttons
- Input validation: place name must be non-blank; latitude in `[-90, 90]`; longitude in `[-180, 180]`. Inline error messages shown on invalid input.

#### Scenario: Valid input enables confirmation
- **WHEN** all fields contain valid values
- **THEN** the confirm button triggers `OnStartingPointConfirmed` with the entered values

#### Scenario: Invalid latitude shows error
- **WHEN** the user enters a latitude outside `[-90, 90]`
- **THEN** an inline error is shown on the latitude field

#### Scenario: Invalid longitude shows error
- **WHEN** the user enters a longitude outside `[-180, 180]`
- **THEN** an inline error is shown on the longitude field

#### Scenario: Blank place name shows error
- **WHEN** the user leaves the place name field blank
- **THEN** an inline error is shown on the place name field

### Requirement: TripDetailScreen integrates all components
`TripDetailScreen` SHALL:
- Accept `uiState` and `onIntent` parameters.
- Render `StartingPointSection` below the trip title.
- Show `SetStartingPointDialog` when `isSetStartingPointDialogVisible` is `true`.
- Consume `uiEffect` to show a snackbar on `ShowError`.

#### Scenario: Screen renders starting point section
- **WHEN** the trip detail screen is displayed
- **THEN** the starting point section is visible below the trip title

#### Scenario: Snackbar shown on error
- **WHEN** a `ShowError` effect is emitted
- **THEN** a snackbar with the error message is displayed

### Requirement: TripDetailScreen wired in navigation
`MainScreen` SHALL wire `TripDetailViewModel` via `hiltViewModel()` in the `ROUTE_TRIP_DETAIL` composable destination, passing `uiState` and `onIntent` to `TripDetailScreen`.

#### Scenario: Navigation to trip detail creates ViewModel
- **WHEN** the user navigates to a trip's detail screen
- **THEN** `TripDetailViewModel` is created with the trip's ID from the route arguments
