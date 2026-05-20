## Why

The domain layer currently has three separate use cases (`SetStartingPointUseCase`, `SetDestinationUseCase`, `AddIntermediateStopUseCase`) that share the same signature and all trim the place name before delegating to the repository. This duplication makes the codebase harder to maintain, duplicates the `MAX_STOP_COUNT` constant across files, and forces the ViewModel to inject three dependencies for what is logically a single operation with a type discriminator.

## What Changes

- Introduce a `StopType` enum (`STARTING_POINT`, `DESTINATION`, `INTERMEDIATE`) in the domain model
- Create a single `SetStopUseCase` that accepts a `StopType` parameter, trims place names, enforces the 25-stop limit for intermediate stops only, and delegates to the correct repository method
- Update `TripDetailViewModel` to inject and use only `SetStopUseCase` instead of three separate use cases
- Delete the three old use case classes and their test files
- Create a single `SetStopUseCaseTest` covering all stop types
- Update `TripDetailViewModelTest` to mock the consolidated use case

## Capabilities

### New Capabilities

_(none — the consolidated use case is a replacement, not a new capability)_

### Modified Capabilities

- `stop-management`: Replace `SetStartingPointUseCase` and `SetDestinationUseCase` requirements with a single `SetStopUseCase` requirement; add `StopType` enum requirement
- `intermediate-stop-management`: Replace `AddIntermediateStopUseCase` requirement with delegation to `SetStopUseCase` with `StopType.INTERMEDIATE`
- `destination-management`: Replace `SetDestinationUseCase` requirement with delegation to `SetStopUseCase` with `StopType.DESTINATION`
- `trip-detail`: Update `TripDetailViewModel` requirement to inject `SetStopUseCase` instead of three separate use cases

## Impact

- **Domain layer:** 3 use case files deleted, 2 new files created (`StopType.kt`, `SetStopUseCase.kt`)
- **UI layer:** `TripDetailViewModel.kt` modified (constructor and private methods); no screen/composable changes
- **Test layer:** 3 test files deleted, 1 new test file created; `TripDetailViewModelTest` updated
- **DI:** No module changes — Hilt auto-discovers `SetStopUseCase` via `@Inject constructor`
- **Repository / Data layer:** No changes — the three distinct repository methods remain as-is
- **UI / UX:** No user-facing changes
