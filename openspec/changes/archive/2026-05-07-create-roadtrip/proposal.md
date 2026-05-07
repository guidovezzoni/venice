## Why

Users need the ability to create a new roadtrip to begin planning their journey. This is the foundational feature of the Venice app — without trip creation, no other trip management features can function. It establishes the core data model, persistence layer, and the first user-facing screen.

## What Changes

- Add a Trip List screen as the app's main screen, displaying all saved trips.
- Add a Floating Action Button (FAB) that opens a dialog/bottom sheet for creating a new trip.
- Implement trip name input with validation (required, trimmed, max 100 characters).
- Persist trips locally using Room database.
- Navigate to the Trip Detail screen after successful creation.
- Introduce the foundational clean architecture layers: data (Room entity, DAO, repository), domain (model, repository interface, use case), and UI (screen, dialog, ViewModel with MVI pattern).

## Capabilities

### New Capabilities
- `trip-persistence`: Room database setup with TripEntity, TripDao, and database configuration for local trip storage.
- `trip-creation`: End-to-end trip creation flow including validation, repository, use case, and UI (dialog, FAB, ViewModel).
- `trip-list`: Trip List screen displaying all saved trips with navigation to trip detail after creation.

### Modified Capabilities
<!-- No existing capabilities to modify — this is a greenfield project. -->

## Impact

- **New dependencies**: Room (runtime, compiler, KTX), Hilt (Dagger), Navigation Compose, MockK and coroutines-test for testing.
- **New database**: Creates the app's Room database with a `trips` table.
- **New screens**: Trip List screen (main entry point) and scaffolding for Trip Detail screen (navigation target).
- **Architecture foundation**: Establishes the data/domain/UI layer separation and MVI pattern that all future features will follow.
- **Sync-ready IDs**: Uses UUID-based String primary keys from the start, avoiding a breaking migration when remote sync and trip sharing are introduced later.
- **Package structure**: Creates `data/`, `domain/`, `ui/screens/`, `ui/state/`, `ui/intent/`, `ui/effect/`, `ui/viewmodel/`, and `di/` packages under `com.guidovezzoni.venice`.
