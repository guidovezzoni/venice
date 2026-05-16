## MODIFIED Requirements

### Requirement: StopRepository interface
The domain layer SHALL define a `StopRepository` interface with:
- `suspend fun upsertStartingPoint(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun upsertDestination(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `fun observeStopsForTrip(tripId: String): Flow<List<Stop>>`

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `StopRepository`
- **THEN** it depends only on the domain layer, not the data layer

### Requirement: StopRepositoryImpl upserts stops via internal helper
`StopRepositoryImpl` SHALL implement both `upsertStartingPoint` and `upsertDestination` by delegating to a private `upsertStop` helper that captures the shared upsert pattern:
1. Query for an existing stop via a provided finder function.
2. If found, update the existing stop's `placeName`, `latitude`, and `longitude` (preserving the same `id` and `order`).
3. If not found, insert a new stop with a fresh UUID, the specified `order`, and `status = PENDING`.
4. Return the resulting `Stop` wrapped in `Result.success`.
5. If any DAO operation throws, return `Result.failure` with the exception.

For `upsertStartingPoint`: finder is `StopDao.getStartingPoint(tripId)`, order is `0`.
For `upsertDestination`: finder is `StopDao.getDestination(tripId)`, order is `1`.

#### Scenario: No existing destination — insert
- **WHEN** `upsertDestination` is called for a trip with no `order > 0` stop
- **THEN** a new stop is inserted with `order = 1`, `status = PENDING`, and a UUID `id`, and `Result.success(Stop)` is returned

#### Scenario: Existing destination — update
- **WHEN** `upsertDestination` is called for a trip that already has an `order > 0` stop
- **THEN** the existing stop's `placeName`, `latitude`, and `longitude` are updated, its `id` is preserved, and `Result.success(Stop)` is returned

#### Scenario: DAO throws exception
- **WHEN** the DAO throws an exception during `upsertDestination`
- **THEN** `Result.failure` with the exception is returned

## ADDED Requirements

### Requirement: SetDestinationUseCase validates and delegates
`SetDestinationUseCase` SHALL trim the place name and delegate to `StopRepository.upsertDestination`.

Note: `SetStartingPointUseCase` and `SetDestinationUseCase` are candidates for consolidation into a single `SetStopUseCase` when `StopType` is introduced in story 1.2.3.

#### Scenario: Successful invocation
- **WHEN** `invoke` is called with valid parameters
- **THEN** the repository receives the trimmed place name and returns `Result.success(Stop)`

#### Scenario: Repository returns failure
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

#### Scenario: Place name is trimmed
- **WHEN** `invoke` is called with `placeName = "  Barcelona  "`
- **THEN** the repository receives `placeName = "Barcelona"`
