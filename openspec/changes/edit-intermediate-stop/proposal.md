## Why

Intermediate stops cannot be edited after creation. Starting point and destination already support tap-to-edit via `SetStopDialog`, but tapping an intermediate stop card does nothing — no intent fires, no dialog opens. Users need to correct or change intermediate stop locations without deleting and re-adding.

## What Changes

- Add a `getStopById` query to `StopDao` for retrieving a stop by primary key
- Add an `updateStop` method to `StopRepository` for updating an existing stop's place name and coordinates
- Create a new `EditStopUseCase` that trims the place name and delegates to the repository
- Extend `TripDetailUiState` with `editingStop` and `isEditStopDialogVisible` fields
- Add edit-specific intents (`OnEditStopClicked`, `OnEditStopConfirmed`, `OnDismissEditStopDialog`) to `TripDetailUiIntent`
- Wire `TripDetailViewModel` to handle edit intents via `EditStopUseCase`
- Wire intermediate stop card taps in `TripDetailScreen` to open a pre-populated `SetStopDialog`
- Add an "Edit stop" dialog title string resource (reuse existing hint/error strings)

## Capabilities

### New Capabilities

_(none — editing is an extension of existing stop management)_

### Modified Capabilities

- `stop-management`: Add `updateStop` repository method, `getStopById` DAO query, and `EditStopUseCase`
- `trip-detail`: Add edit stop intents, state fields, ViewModel handling, and screen wiring for intermediate stop editing

## Impact

- **Data layer**: `StopDao` gains one new query; `StopRepository` and `StopRepositoryImpl` gain one new method
- **Domain layer**: One new use case file (`EditStopUseCase`)
- **UI layer**: State, intent, ViewModel, and screen files all modified; one new string resource added
- **No breaking changes**: All existing behaviour is preserved; starting point and destination editing continues to work via the existing upsert flow
- **No new dependencies**: Uses existing Room, Hilt, and Compose infrastructure
