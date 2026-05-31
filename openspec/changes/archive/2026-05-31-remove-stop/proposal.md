## Why

Users need to remove stops they no longer want on their trip — whether to simplify the journey or correct a mistake. Without this, the only option is to edit a stop's details, which doesn't help when the stop itself is unwanted. This completes the CRUD lifecycle for stops (create, read, update, and now delete).

## What Changes

- Add a delete button to all filled stop cards (starting point, intermediates, destination)
- Show a confirmation dialog before deletion, displaying the stop's place name
- After deletion, recalculate remaining stop orders to be contiguous (no gaps)
- When the starting point is removed, the next stop is promoted to order 0
- When the destination is removed, the previous stop becomes the new destination
- Allow a trip to have zero stops (removing the last stop leaves an empty trip)
- Add a transactional DAO method to delete and reorder atomically
- Add a new `RemoveStopUseCase` with a TODO for leg invalidation (deferred to Epic 3)

## Capabilities

### New Capabilities

- `stop-removal`: Domain logic, repository contract, DAO operations, and use case for deleting a stop and reordering remaining stops within a transaction

### Modified Capabilities

- `stop-management`: Add `deleteStop` to the `StopRepository` interface and `StopRepositoryImpl`
- `trip-detail`: Add remove stop intents, dialog state, delete button on stop cards, and confirmation dialog to the trip detail MVI contract and UI

## Impact

- **Data layer**: `StopDao` gains three new methods (`deleteById`, `decrementOrderAbove`, `deleteAndReorder`); `StopRepositoryImpl` gains `deleteStop`
- **Domain layer**: `StopRepository` interface adds `deleteStop`; new `RemoveStopUseCase`
- **UI layer**: `TripDetailUiIntent`, `TripDetailUiState`, `TripDetailViewModel`, `StopSection`, `TripDetailScreen` all modified; new confirmation `AlertDialog`
- **Resources**: New string resources in EN, IT, ES
- **Tests**: New `RemoveStopUseCaseTest`; additions to `StopRepositoryImplTest` and `TripDetailViewModelTest`
