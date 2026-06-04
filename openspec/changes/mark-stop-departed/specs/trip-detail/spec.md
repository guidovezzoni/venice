# Trip Detail

## MODIFIED Requirements

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

#### Scenario: All intents are representable
- **WHEN** a user action occurs on the trip detail screen
- **THEN** it maps to exactly one `TripDetailUiIntent` subclass

#### Scenario: Mark departed intent carries stop ID
- **WHEN** the user taps "Mark as departed" on a stop with `id = "s1"`
- **THEN** `OnMarkStopDepartedClicked("s1")` is dispatched

#### Scenario: Undo departed intent carries stop ID
- **WHEN** the user taps "Undo" on a stop with `id = "s2"`
- **THEN** `OnUndoMarkStopDepartedClicked("s2")` is dispatched

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
- Handle all existing intents as before (starting point, destination, add stop, move, edit, remove).
- On `OnMarkStopDepartedClicked`: call `MarkStopDepartedUseCase(tripId, stopId)`; on failure emit `ShowError`.
- On `OnUndoMarkStopDepartedClicked`: call `UndoMarkStopDepartedUseCase(tripId)`; on failure emit `ShowError`.

#### Scenario: Mark stop departed — success
- **WHEN** `OnMarkStopDepartedClicked` is dispatched and `MarkStopDepartedUseCase` succeeds
- **THEN** the stop list updates via Flow observation (the departed stop now has `status = VISITED`)

#### Scenario: Mark stop departed — failure
- **WHEN** `OnMarkStopDepartedClicked` is dispatched and `MarkStopDepartedUseCase` fails
- **THEN** a `ShowError` effect is emitted with the error message

#### Scenario: Undo mark departed — success
- **WHEN** `OnUndoMarkStopDepartedClicked` is dispatched and `UndoMarkStopDepartedUseCase` succeeds
- **THEN** the stop list updates via Flow observation (the reverted stop now has `status = PENDING`)

#### Scenario: Undo mark departed — failure
- **WHEN** `OnUndoMarkStopDepartedClicked` is dispatched and `UndoMarkStopDepartedUseCase` fails
- **THEN** a `ShowError` effect is emitted with the error message

## ADDED Requirements

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
