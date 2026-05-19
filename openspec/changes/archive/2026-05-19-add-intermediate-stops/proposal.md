## Why

Roadtrips are fundamentally about the stops along the way. Currently, trips only support a starting point and a destination. Users need to plan breaks, visits, and detours by adding intermediate stops between those two fixed points, making this a core interaction for trip planning.

## What Changes

- Users can add intermediate stops between the starting point and destination via the existing `SetStopDialog` pattern.
- Each intermediate stop is inserted at a chosen position in the stop order; existing stops shift to accommodate.
- The destination always remains the last stop.
- A maximum of 23 intermediate stops is enforced (25 total including start and destination — Google Directions API waypoint limit).
- Intermediate stops are displayed in the trip detail screen between starting point and destination, in correct order.
- An "Add Stop" button appears between the last intermediate stop and the destination; it is hidden when the limit is reached.

## Capabilities

### New Capabilities
- `intermediate-stop-management`: Domain use case for validating and adding intermediate stops, including input validation, order enforcement, and stop-count limits.

### Modified Capabilities
- `stop-management`: Add repository method `addIntermediateStop` for shifting orders and inserting a new stop in a transaction.
- `stop-persistence`: Add DAO queries `incrementOrderFrom` (shift orders up) and `getStopCount` (total stops for a trip).
- `trip-detail`: Extend UI state with intermediate stops list, add-stop dialog visibility, and stop-limit flag; add intents for the add-stop flow; render intermediate stops and the "Add Stop" button in the screen.

## Impact

- **Data layer**: `StopDao` gains two new queries; `StopRepositoryImpl` gains a transactional insert-with-shift method.
- **Domain layer**: New `AddIntermediateStopUseCase` class; `StopRepository` interface extended.
- **UI layer**: `TripDetailUiState`, `TripDetailUiIntent`, `TripDetailViewModel`, and `TripDetailScreen` all modified; new strings added to `strings.xml` (with translations).
- **No new dependencies, migrations, or external API calls** — all operations are local Room database operations against the existing `stops` table schema.
