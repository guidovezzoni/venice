## MODIFIED Requirements

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
- On `OnDismissStartingPointDialog`: set `isSetStartingPointDialogVisible = false`.
- On `OnStartingPointConfirmed`: call `SetStopUseCase` with `StopType.STARTING_POINT`; on success dismiss the dialog; on failure emit `ShowError`.
- On `OnSetDestinationClicked`: set `isSetDestinationDialogVisible = true`.
- On `OnDismissDestinationDialog`: set `isSetDestinationDialogVisible = false`.
- On `OnDestinationConfirmed`: call `SetStopUseCase` with `StopType.DESTINATION`; on success dismiss the dialog; on failure emit `ShowError`.
- On `OnAddStopClicked`: set `isAddStopDialogVisible = true`.
- On `OnDismissAddStopDialog`: set `isAddStopDialogVisible = false`.
- On `OnAddStopConfirmed`: call `SetStopUseCase` with `StopType.INTERMEDIATE`; on success dismiss the dialog; on failure emit `ShowError`.

#### Scenario: Opening starting point dialog
- **WHEN** `OnSetStartingPointClicked` is dispatched
- **THEN** `isSetStartingPointDialogVisible` becomes `true`

#### Scenario: Dismissing starting point dialog
- **WHEN** `OnDismissStartingPointDialog` is dispatched while dialog is visible
- **THEN** `isSetStartingPointDialogVisible` becomes `false`

#### Scenario: Confirming starting point — success
- **WHEN** `OnStartingPointConfirmed` is dispatched and `SetStopUseCase` with `StopType.STARTING_POINT` succeeds
- **THEN** `startingPoint` is updated and the dialog is dismissed

#### Scenario: Confirming starting point — failure
- **WHEN** `OnStartingPointConfirmed` is dispatched and `SetStopUseCase` with `StopType.STARTING_POINT` fails
- **THEN** a `ShowError` effect is emitted

#### Scenario: Opening destination dialog
- **WHEN** `OnSetDestinationClicked` is dispatched
- **THEN** `isSetDestinationDialogVisible` becomes `true`

#### Scenario: Dismissing destination dialog
- **WHEN** `OnDismissDestinationDialog` is dispatched while dialog is visible
- **THEN** `isSetDestinationDialogVisible` becomes `false`

#### Scenario: Confirming destination — success
- **WHEN** `OnDestinationConfirmed` is dispatched and `SetStopUseCase` with `StopType.DESTINATION` succeeds
- **THEN** `destination` is updated and the dialog is dismissed

#### Scenario: Confirming destination — failure
- **WHEN** `OnDestinationConfirmed` is dispatched and `SetStopUseCase` with `StopType.DESTINATION` fails
- **THEN** a `ShowError` effect is emitted

#### Scenario: Opening add stop dialog
- **WHEN** `OnAddStopClicked` is dispatched
- **THEN** `isAddStopDialogVisible` becomes `true`

#### Scenario: Dismissing add stop dialog
- **WHEN** `OnDismissAddStopDialog` is dispatched while dialog is visible
- **THEN** `isAddStopDialogVisible` becomes `false`

#### Scenario: Confirming add stop — success
- **WHEN** `OnAddStopConfirmed` is dispatched and `SetStopUseCase` with `StopType.INTERMEDIATE` succeeds
- **THEN** the dialog is dismissed and the new stop appears in `intermediateStops`

#### Scenario: Confirming add stop — failure
- **WHEN** `OnAddStopConfirmed` is dispatched and `SetStopUseCase` with `StopType.INTERMEDIATE` fails
- **THEN** a `ShowError` effect is emitted

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
