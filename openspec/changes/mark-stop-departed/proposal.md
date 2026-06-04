## Why

Users need to track their progress along a road trip by marking stops as departed. Without this, the trip detail screen is static and doesn't reflect where the user currently is on their journey. This is the foundational feature for the "Stop Progress" feature group (1.3.x).

## What Changes

- Add `updateStopStatus(stopId, status)` to `StopDao`, `StopRepository`, and `StopRepositoryImpl`
- Create `MarkStopDepartedUseCase` — marks the current stop (first PENDING by order) as VISITED; rejects if target is not the current stop
- Create `UndoMarkStopDepartedUseCase` — reverts the last departed stop (highest-order VISITED) back to PENDING
- Add `OnMarkStopDepartedClicked` and `OnUndoMarkStopDepartedClicked` intents to `TripDetailUiIntent`
- Handle new intents in `TripDetailViewModel`
- Visually differentiate departed/current/upcoming stops in `StopSection` (icon tint, card styling)
- Show "Mark as departed" button only on the current stop
- Show "Undo" button only on the last departed stop
- Add EN and IT string resources for new labels

## Capabilities

### New Capabilities
- `stop-progress`: Domain logic for marking stops as departed and undoing that action, including current-stop derivation and validation

### Modified Capabilities
- `stop-persistence`: Add `updateStopStatus` DAO query for targeted status updates
- `trip-detail`: Add status-based visual differentiation and mark/undo action buttons to the trip detail UI

## Impact

- **Data layer**: `StopDao` gets one new query; `StopRepository` interface and impl get one new method
- **Domain layer**: Two new use cases added
- **UI layer**: `TripDetailUiIntent`, `TripDetailViewModel`, `StopSection`, and `TripDetailScreen` modified; new string resources
- **No schema migration**: `StopStatus.PENDING`/`VISITED` already exist in the DB
- **No breaking changes**: Existing stop operations (edit, reorder, remove) remain unrestricted regardless of status
