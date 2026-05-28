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

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`

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
- On `OnMoveStopUp`: call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder - 1)`; on failure emit `ShowError`.
- On `OnMoveStopDown`: call `MoveStopUseCase` with `(tripId, currentOrder, currentOrder + 1)`; on failure emit `ShowError`.
- On `OnEditStopClicked`: set `editingStop` to the provided stop and `isEditStopDialogVisible = true`.
- On `OnDismissEditStopDialog`: set `editingStop = null` and `isEditStopDialogVisible = false`.
- On `OnEditStopConfirmed`: call `EditStopUseCase` with the provided `stopId`, `placeName`, `latitude`, `longitude`; on success set `editingStop = null` and `isEditStopDialogVisible = false`; on failure emit `ShowError`.

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

#### Scenario: Move stop up — success
- **WHEN** `OnMoveStopUp` is dispatched with `currentOrder = 2` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `MoveStopUseCase` is called with `(tripId, 2, 1)` and the stop list updates via the existing Flow observation

#### Scenario: Move stop down — success
- **WHEN** `OnMoveStopDown` is dispatched with `currentOrder = 1` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `MoveStopUseCase` is called with `(tripId, 1, 2)` and the stop list updates via the existing Flow observation

#### Scenario: Move stop — failure
- **WHEN** `OnMoveStopUp` or `OnMoveStopDown` is dispatched and `MoveStopUseCase` returns failure
- **THEN** a `ShowError` effect is emitted

#### Scenario: Opening edit stop dialog
- **WHEN** `OnEditStopClicked` is dispatched with a stop
- **THEN** `editingStop` is set to the provided stop and `isEditStopDialogVisible` becomes `true`

#### Scenario: Dismissing edit stop dialog
- **WHEN** `OnDismissEditStopDialog` is dispatched
- **THEN** `editingStop` is set to `null` and `isEditStopDialogVisible` becomes `false`

#### Scenario: Confirming edit stop — success
- **WHEN** `OnEditStopConfirmed` is dispatched and `EditStopUseCase` succeeds
- **THEN** `editingStop` is set to `null`, `isEditStopDialogVisible` becomes `false`, and the updated stop is reflected via Flow observation

#### Scenario: Confirming edit stop — failure
- **WHEN** `OnEditStopConfirmed` is dispatched and `EditStopUseCase` fails
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
- Render `StopSection` for the starting point (with `TripOrigin` icon) below the trip title.
- Render intermediate stops between the starting point and destination sections. Each intermediate stop SHALL be rendered using `StopSection` with `LocationOn` icon. Tapping an intermediate stop card SHALL dispatch `OnEditStopClicked(stop)`.
- Render an "Add Stop" button (e.g., `OutlinedButton` or `IconButton` with `Icons.Default.AddLocation`) between the last intermediate stop and the destination section. The button SHALL be visible only when `canAddMoreStops` is `true`. Tapping it SHALL dispatch `OnAddStopClicked`.
- Render `StopSection` for the destination (with `Place` icon) below the intermediate stops and add-stop button.
- Show `SetStopDialog` for the starting point when `isSetStartingPointDialogVisible` is `true`. If `uiState.startingPoint` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for the destination when `isSetDestinationDialogVisible` is `true`. If `uiState.destination` is non-null, pass its `placeName`, `latitude`, and `longitude` as initial values to the dialog.
- Show `SetStopDialog` for adding an intermediate stop when `isAddStopDialogVisible` is `true`, with empty initial values.
- Show `SetStopDialog` for editing an intermediate stop when `isEditStopDialogVisible` is `true`, with `editingStop`'s `placeName`, `latitude`, and `longitude` as initial values and the edit stop dialog title string resource. On confirm, dispatch `OnEditStopConfirmed` with the `editingStop`'s `id` and the entered values.
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

### Requirement: TripDetailScreen wired in navigation
`MainScreen` SHALL wire `TripDetailViewModel` via `hiltViewModel()` in the `ROUTE_TRIP_DETAIL` composable destination, passing `uiState` and `onIntent` to `TripDetailScreen`.

#### Scenario: Navigation to trip detail creates ViewModel
- **WHEN** the user navigates to a trip's detail screen
- **THEN** `TripDetailViewModel` is created with the trip's ID from the route arguments
