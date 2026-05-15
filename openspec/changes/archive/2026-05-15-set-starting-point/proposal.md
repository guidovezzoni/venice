## Why

The app can create and list road trips, but a trip currently has no route data. Before route calculation or navigation can work, the user needs to designate a departure location. Adding a starting point (the first stop at `order = 0`) is the foundational step for the stop management feature and unblocks all future route-related stories.

## What Changes

- Introduce a `Stop` domain model and `StopStatus` enum to represent points along a trip's route.
- Add a `stops` Room table with a foreign key to `trips`, requiring a database migration (v1 → v2).
- Provide a `StopRepository` with upsert semantics for the starting point and a reactive stream of stops per trip.
- Add `SetStartingPointUseCase` and `ObserveStopsUseCase` in the domain layer.
- Build a `TripDetailScreen` driven by a new `TripDetailViewModel` (MVI) that shows a starting-point section with empty-state placeholder and a stub input dialog (place name + lat + lng).
- Wire DI (Hilt) for the new DAO, repository, and ViewModel.
- Add input validation for coordinates (lat ∈ [-90, 90], lng ∈ [-180, 180]) and non-blank place name.

## Capabilities

### New Capabilities
- `stop-persistence`: Room entity, DAO, mapper, and database migration for the `stops` table.
- `stop-management`: Domain model (`Stop`, `StopStatus`), repository interface/impl, and use cases for upserting and observing stops.
- `trip-detail`: MVI screen (state/intent/effect, ViewModel, composables) for viewing a trip's detail and managing its starting point via a stub dialog.

### Modified Capabilities
- `trip-persistence`: Database version bumped from 1 to 2; `AppDatabase` gains `StopEntity`, `StopDao`, and `MIGRATION_1_2`.

## Impact

- **Database**: Breaking schema change handled by `MIGRATION_1_2` — existing users' data is preserved.
- **DI modules**: `DatabaseModule` and `RepositoryModule` gain new provider/binding entries.
- **Navigation**: `MainScreen` wires a new `TripDetailViewModel` into the existing `ROUTE_TRIP_DETAIL` composable.
- **Dependencies**: No new external dependencies required; uses existing Room, Hilt, and Compose libraries.
- **Testing**: New unit test suites for repository, use case, and ViewModel.
