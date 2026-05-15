## MODIFIED Requirements

### Requirement: StopRepository interface
The domain layer SHALL define a `StopRepository` interface with:
- `suspend fun upsertStartingPoint(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun upsertDestination(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `fun observeStopsForTrip(tripId: String): Flow<List<Stop>>`

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `StopRepository`
- **THEN** it depends only on the domain layer, not the data layer

### Requirement: StopRepositoryImpl upserts the destination
`StopRepositoryImpl` SHALL implement `upsertDestination`. The method SHALL:
1. Query for an existing destination stop via `StopDao.getDestination(tripId)`.
2. If found, update the existing stop's `placeName`, `latitude`, and `longitude` (preserving the same `id` and `order`).
3. If not found, insert a new stop with a fresh UUID, `order = 1`, and `status = PENDING`.
4. Return the resulting `Stop` wrapped in `Result.success`.
5. If any DAO operation throws, return `Result.failure` with the exception.

#### Scenario: No existing destination — insert
- **WHEN** `upsertDestination` is called for a trip with no `order > 0` stop
- **THEN** a new stop is inserted with `order = 1`, `status = PENDING`, and a UUID `id`, and `Result.success(Stop)` is returned

#### Scenario: Existing destination — update
- **WHEN** `upsertDestination` is called for a trip that already has an `order > 0` stop
- **THEN** the existing stop's `placeName`, `latitude`, and `longitude` are updated, its `id` is preserved, and `Result.success(Stop)` is returned

#### Scenario: DAO throws exception
- **WHEN** the DAO throws an exception during `upsertDestination`
- **THEN** `Result.failure` with the exception is returned
