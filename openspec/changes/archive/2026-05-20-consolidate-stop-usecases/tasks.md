## 1. Prerequisites

- [x] 1.1 Create `StopType` enum in `app/src/main/java/com/guidovezzoni/venice/domain/model/StopType.kt` with entries `STARTING_POINT`, `DESTINATION`, `INTERMEDIATE`

## 2. SetStopUseCase (BDD)

- [x] 2.1 Write test: `GIVEN a valid starting point WHEN invoke is called THEN repository upsertStartingPoint is called with trimmed name` in `SetStopUseCaseTest`
- [x] 2.2 Write test: `GIVEN a valid destination WHEN invoke is called THEN repository upsertDestination is called with trimmed name` in `SetStopUseCaseTest`
- [x] 2.3 Write test: `GIVEN a valid intermediate stop below limit WHEN invoke is called THEN repository addIntermediateStop is called with trimmed name` in `SetStopUseCaseTest`
- [x] 2.4 Write test: `GIVEN stop count at limit WHEN adding intermediate stop THEN result is failure with IllegalStateException` in `SetStopUseCaseTest`
- [x] 2.5 Write test: `GIVEN repository returns failure for starting point WHEN invoke is called THEN failure is propagated` in `SetStopUseCaseTest`
- [x] 2.6 Write test: `GIVEN repository returns failure for destination WHEN invoke is called THEN failure is propagated` in `SetStopUseCaseTest`
- [x] 2.7 Write test: `GIVEN repository returns failure for intermediate stop WHEN invoke is called THEN failure is propagated` in `SetStopUseCaseTest`
- [x] 2.8 Write test: `GIVEN place name with whitespace WHEN invoke is called for each stop type THEN name is trimmed` in `SetStopUseCaseTest`
- [x] 2.9 Implement `SetStopUseCase` in `app/src/main/java/com/guidovezzoni/venice/domain/usecase/SetStopUseCase.kt` to make all tests pass

## 3. TripDetailViewModel update (BDD)

- [x] 3.1 Update `TripDetailViewModelTest` to replace mocks of `SetStartingPointUseCase`, `SetDestinationUseCase`, `AddIntermediateStopUseCase` with a single mock of `SetStopUseCase`
- [x] 3.2 Update `TripDetailViewModel` to inject `SetStopUseCase` instead of three use cases, and replace three private methods with a single parameterised `setStop` helper

## 4. Cleanup

- [x] 4.1 Delete `SetStartingPointUseCase.kt` and `SetStartingPointUseCaseTest.kt`
- [x] 4.2 Delete `SetDestinationUseCase.kt` and `SetDestinationUseCaseTest.kt`
- [x] 4.3 Delete `AddIntermediateStopUseCase.kt` and `AddIntermediateStopUseCaseTest.kt`

## 5. Verification

- [x] 5.1 Run `./gradlew check` and confirm all tests pass with no lint errors
- [x] 5.2 Grep `app/src/` for any remaining references to `SetStartingPointUseCase`, `SetDestinationUseCase`, or `AddIntermediateStopUseCase` — confirm none exist
