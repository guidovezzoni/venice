## Why

The trip detail screen currently only supports a starting point. Users need to set a destination so the app knows where the trip is heading, which is a prerequisite for route calculation in future stories.

## What Changes

- Add a `getDestination(tripId)` query to `StopDao` to retrieve the highest-order stop above 0
- Extend `StopRepository` with `upsertDestination` to insert or update the destination stop
- Create `SetDestinationUseCase` to orchestrate destination persistence with input trimming
- Extend `TripDetailUiState` with `destination` and `isSetDestinationDialogVisible` fields
- Extend `TripDetailUiIntent` with destination-related intents (`OnSetDestinationClicked`, `OnDestinationConfirmed`, `OnDismissDestinationDialog`)
- Update `TripDetailViewModel` to observe the destination from the stops stream and handle destination intents
- Create `DestinationSection` composable (empty state with "Set destination" button, filled state with Place icon)
- Create `SetDestinationDialog` composable (stub dialog with place name, lat, lng fields and validation)
- Update `TripDetailScreen` to render the destination section and dialog
- Add destination-related string resources to `strings.xml`
- Add unit tests for repository, use case, and ViewModel destination logic

## Capabilities

### New Capabilities

- `destination-management`: Domain logic for upserting and observing the destination stop (use case, repository extension, DAO query)
- `destination-ui`: UI composables for destination section, stub dialog, and integration into trip detail screen

### Modified Capabilities

- `stop-persistence`: Adding `getDestination` DAO query to support destination retrieval
- `stop-management`: Extending `StopRepository` with `upsertDestination` and adding `SetDestinationUseCase`
- `trip-detail`: Extending MVI contract (state, intents) and ViewModel to handle destination alongside starting point

## Impact

- **Data layer**: `StopDao` gains one new query; no schema migration needed since the existing `stops` table already supports `order > 0` stops
- **Domain layer**: `StopRepository` interface gains one new method; new `SetDestinationUseCase` class
- **UI layer**: `TripDetailUiState`, `TripDetailUiIntent`, `TripDetailViewModel`, `TripDetailScreen`, and `MainScreen` all modified; two new composable files created
- **Resources**: New string entries in `strings.xml`
- **Dependencies**: No new dependencies required
