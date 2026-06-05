## Why

Both `TripDetailViewModel` and `TripListViewModel` declare `isLoading` in their UI state, but only `setStop()` and `createTrip()` use it. The remaining five async operations in `TripDetailViewModel` (`editStop`, `removeStop`, `moveStop`, `markStopDeparted`, `undoMarkStopDeparted`) never set the flag, and neither screen passes `isLoading` to dialog composables or action buttons. This means users can trigger duplicate or conflicting operations during processing, and there is no visual feedback that an action is in progress.

## What Changes

- Wrap all five uncovered async operations in `TripDetailViewModel` with `isLoading = true/false`
- Add `isLoading: Boolean` parameter to `SetStopDialog` and `CreateTripDialog` to disable confirm buttons and show a `CircularProgressIndicator` while loading
- Add `isLoading: Boolean` parameter to `StopSection` to disable action buttons (move up/down, delete, mark departed, undo departed) while loading
- Pass `uiState.isLoading` from `TripDetailScreen` to all `SetStopDialog` instances, `StopSection` instances, and the remove-stop confirmation dialog
- Pass `uiState.isLoading` from `TripListScreen` to `CreateTripDialog`
- Show a screen-level `CircularProgressIndicator` on both screens while loading
- Add loading-state previews for dialogs and screens
- Add unit tests for `isLoading` transitions on all five newly-wrapped operations
- Add Compose UI tests for button disabling and spinner visibility

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `destination-ui`: `SetStopDialog` gains `isLoading` parameter that disables the confirm button and shows a spinner
- `trip-detail`: `TripDetailViewModel` wraps all async operations with `isLoading`; `TripDetailScreen` passes loading state to dialogs and action buttons; screen shows spinner while loading
- `trip-list`: `TripListScreen` passes `isLoading` to `CreateTripDialog` and shows spinner while loading
- `trip-creation`: `CreateTripDialog` gains `isLoading` parameter that disables the confirm button and shows a spinner

## Impact

- **UI layer**: `SetStopDialog`, `CreateTripDialog`, `StopSection`, `TripDetailScreen`, `TripListScreen` — parameter additions and wiring
- **ViewModel layer**: `TripDetailViewModel` — five methods gain `isLoading` wrapping
- **Tests**: New unit tests in `TripDetailViewModelTest`; new Compose UI tests in `TripDetailScreenTest` and `TripListScreenTest`
- **No domain/data layer changes**: All operations are already implemented; this is purely UI/ViewModel state wiring
- **No new dependencies**: `CircularProgressIndicator` is already available via Material 3
