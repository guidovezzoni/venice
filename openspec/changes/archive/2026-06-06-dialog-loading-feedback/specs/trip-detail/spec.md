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
- All async operations SHALL use `withMinimumDuration { ... }` (which defaults to 500 ms) to ensure `isLoading` remains `true` for at least 500 ms, even if the underlying operation completes faster.
- On `OnSetStartingPointClicked`: set `isSetStartingPointDialogVisible = true`.
- On `OnDismissStartingPointDialog`: set `isSetStartingPointDialogVisible = false`.
- On `OnStartingPointConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.STARTING_POINT`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnSetDestinationClicked`: set `isSetDestinationDialogVisible = true`.
- On `OnDismissDestinationDialog`: set `isSetDestinationDialogVisible = false`.
- On `OnDestinationConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.DESTINATION`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnAddStopClicked`: set `isAddStopDialogVisible = true`.
- On `OnDismissAddStopDialog`: set `isAddStopDialogVisible = false`.
- On `OnAddStopConfirmed`: set `isLoading = true`; call `SetStopUseCase` with `StopType.INTERMEDIATE`; on success dismiss the dialog and set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMoveStopUp`: set `isLoading = true`; call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder - 1)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMoveStopDown`: set `isLoading = true`; call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder + 1)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnEditStopClicked`: set `editingStop` to the provided stop and `isEditStopDialogVisible = true`.
- On `OnDismissEditStopDialog`: set `editingStop = null` and `isEditStopDialogVisible = false`.
- On `OnEditStopConfirmed`: set `isLoading = true`; call `EditStopUseCase` with the provided `stopId`, `placeName`, `latitude`, `longitude`; on success set `editingStop = null`, `isEditStopDialogVisible = false`, and `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnRemoveStopClicked`: set `stopToRemove` to the provided stop and `isRemoveStopDialogVisible = true`.
- On `OnDismissRemoveStopDialog`: set `stopToRemove = null` and `isRemoveStopDialogVisible = false`.
- On `OnRemoveStopConfirmed`: set `isLoading = true`; call `RemoveStopUseCase(tripId, stopToRemove!!.id)`; on success set `stopToRemove = null`, `isRemoveStopDialogVisible = false`, and `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnMarkStopDepartedClicked`: set `isLoading = true`; call `MarkStopDepartedUseCase(tripId, stopId)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.
- On `OnUndoMarkStopDepartedClicked`: set `isLoading = true`; call `UndoMarkStopDepartedUseCase(tripId)`; on success set `isLoading = false`; on failure set `isLoading = false` and emit `ShowError`.

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

### Requirement: TripDetailScreen integrates all components
`TripDetailScreen` SHALL:
- Accept `uiState` and `onIntent` parameters.
- Render `StopSection` for the starting point (with `TripOrigin` icon) below the trip title, passing `isLoading = uiState.isLoading`.
- Render intermediate stops between the starting point and destination sections. Each intermediate stop SHALL be rendered using `StopSection` with `LocationOn` icon, passing `isLoading = uiState.isLoading`. Tapping an intermediate stop card SHALL dispatch `OnEditStopClicked(stop)`.
- Render an "Add Stop" button (e.g., `OutlinedButton` or `IconButton` with `Icons.Default.AddLocation`) between the last intermediate stop and the destination section. The button SHALL be visible only when `canAddMoreStops` is `true`. The button SHALL be disabled when `isLoading` is `true`. Tapping it SHALL dispatch `OnAddStopClicked`.
- Render `StopSection` for the destination (with `Place` icon) below the intermediate stops and add-stop button, passing `isLoading = uiState.isLoading`.
- Show `SetStopDialog` for the starting point when `isSetStartingPointDialogVisible` is `true`, passing `isLoading = uiState.isLoading`. If `uiState.startingPoint` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for the destination when `isSetDestinationDialogVisible` is `true`, passing `isLoading = uiState.isLoading`. If `uiState.destination` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for adding an intermediate stop when `isAddStopDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, with empty initial values.
- Show `SetStopDialog` for editing an intermediate stop when `isEditStopDialogVisible` is `true`, passing `isLoading = uiState.isLoading`, with `editingStop`'s `placeName`, `latitude`, and `longitude` as initial values and the edit stop dialog title string resource. On confirm, dispatch `OnEditStopConfirmed` with the `editingStop`'s `id` and the entered values.
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
