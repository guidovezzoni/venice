## ADDED Requirements

### Requirement: StopType enum
The domain layer SHALL define a `StopType` enum in `com.guidovezzoni.venice.domain.model` with values `STARTING_POINT`, `DESTINATION`, `INTERMEDIATE`.

#### Scenario: StopType values
- **WHEN** `StopType.values()` is called
- **THEN** it returns exactly `[STARTING_POINT, DESTINATION, INTERMEDIATE]`

### Requirement: SetStopUseCase validates and delegates by StopType
`SetStopUseCase` SHALL accept `tripId: String`, `placeName: String`, `latitude: Double`, `longitude: Double`, `stopType: StopType` and:
1. Trim the place name.
2. When `stopType` is `INTERMEDIATE`: query `StopRepository.getStopCount(tripId)` and return `Result.failure(IllegalStateException("Maximum of 25 stops reached"))` if count >= 25.
3. Delegate to the correct repository method based on `stopType`:
   - `STARTING_POINT` → `StopRepository.upsertStartingPoint(tripId, trimmedName, latitude, longitude)`
   - `DESTINATION` → `StopRepository.upsertDestination(tripId, trimmedName, latitude, longitude)`
   - `INTERMEDIATE` → `StopRepository.addIntermediateStop(tripId, trimmedName, latitude, longitude)`
4. Return the repository result.

#### Scenario: Starting point — successful invocation
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and valid parameters
- **THEN** `StopRepository.upsertStartingPoint` is called with the trimmed place name and `Result.success(Stop)` is returned

#### Scenario: Destination — successful invocation
- **WHEN** `invoke` is called with `stopType = DESTINATION` and valid parameters
- **THEN** `StopRepository.upsertDestination` is called with the trimmed place name and `Result.success(Stop)` is returned

#### Scenario: Intermediate — successful invocation below limit
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and the trip has fewer than 25 stops
- **THEN** `StopRepository.addIntermediateStop` is called with the trimmed place name and `Result.success(Stop)` is returned

#### Scenario: Intermediate — stop count at limit rejected
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and `StopRepository.getStopCount(tripId)` returns 25
- **THEN** `Result.failure` is returned with an `IllegalStateException` and `addIntermediateStop` is not called

#### Scenario: Starting point — place name is trimmed
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and `placeName = "  Rome  "`
- **THEN** the repository receives `placeName = "Rome"`

#### Scenario: Destination — place name is trimmed
- **WHEN** `invoke` is called with `stopType = DESTINATION` and `placeName = "  Barcelona  "`
- **THEN** the repository receives `placeName = "Barcelona"`

#### Scenario: Intermediate — place name is trimmed
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and `placeName = "  Florence  "`
- **THEN** the repository receives `placeName = "Florence"`

#### Scenario: Starting point — repository failure propagated
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

#### Scenario: Destination — repository failure propagated
- **WHEN** `invoke` is called with `stopType = DESTINATION` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

#### Scenario: Intermediate — repository failure propagated
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

## REMOVED Requirements

### Requirement: SetStartingPointUseCase validates and delegates
**Reason**: Replaced by `SetStopUseCase` with `stopType = STARTING_POINT`
**Migration**: All callers should use `SetStopUseCase(tripId, placeName, latitude, longitude, StopType.STARTING_POINT)` instead

### Requirement: SetDestinationUseCase validates and delegates
**Reason**: Replaced by `SetStopUseCase` with `stopType = DESTINATION`
**Migration**: All callers should use `SetStopUseCase(tripId, placeName, latitude, longitude, StopType.DESTINATION)` instead
