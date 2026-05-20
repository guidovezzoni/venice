## MODIFIED Requirements

### Requirement: StopRepository interface
The domain layer SHALL define a `StopRepository` interface with:
- `suspend fun upsertStartingPoint(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun upsertDestination(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `fun observeStopsForTrip(tripId: String): Flow<List<Stop>>`
- `suspend fun addIntermediateStop(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun getStopCount(tripId: String): Int`

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

`StopRepositoryImpl` SHALL additionally implement:
- `addIntermediateStop(tripId, placeName, latitude, longitude)`: Queries the destination's current order via `StopDao.getDestination(tripId)`, calls `StopDao.incrementOrderFrom(tripId, destinationOrder)` to shift the destination up, inserts a new stop with `order = destinationOrder`, `status = PENDING`, and a fresh UUID, all within a `@Transaction`. Returns `Result.success(Stop)` on success or `Result.failure` on exception. If no destination exists, the new stop is inserted with `order = 1`.
- `getStopCount(tripId)`: Delegates to `StopDao.getStopCount(tripId)` and returns the count.

#### Scenario: No existing starting point — insert
- **WHEN** `upsertStartingPoint` is called for a trip with no `order = 0` stop
- **THEN** a new stop is inserted with `order = 0`, `status = PENDING`, and a UUID `id`, and `Result.success(Stop)` is returned

#### Scenario: Existing starting point — update
- **WHEN** `upsertStartingPoint` is called for a trip that already has an `order = 0` stop
- **THEN** the existing stop's `placeName`, `latitude`, and `longitude` are updated, its `id` is preserved, and `Result.success(Stop)` is returned

#### Scenario: DAO throws exception
- **WHEN** the DAO throws an exception during `upsertStartingPoint`
- **THEN** `Result.failure` with the exception is returned

#### Scenario: No existing destination — insert
- **WHEN** `upsertDestination` is called for a trip with no `order > 0` stop
- **THEN** a new stop is inserted with `order = 1`, `status = PENDING`, and a UUID `id`, and `Result.success(Stop)` is returned

#### Scenario: Existing destination — update
- **WHEN** `upsertDestination` is called for a trip that already has an `order > 0` stop
- **THEN** the existing stop's `placeName`, `latitude`, and `longitude` are updated, its `id` is preserved, and `Result.success(Stop)` is returned

#### Scenario: DAO throws exception during upsertDestination
- **WHEN** the DAO throws an exception during `upsertDestination`
- **THEN** `Result.failure` with the exception is returned

#### Scenario: Adding intermediate stop with existing destination
- **WHEN** `addIntermediateStop` is called for a trip with a destination at `order = 1`
- **THEN** the destination's order is shifted to `2`, and the new stop is inserted at `order = 1` with `status = PENDING` and a UUID `id`, and `Result.success(Stop)` is returned

#### Scenario: Adding intermediate stop without destination
- **WHEN** `addIntermediateStop` is called for a trip with no destination
- **THEN** the new stop is inserted at `order = 1` with `status = PENDING` and `Result.success(Stop)` is returned

#### Scenario: Adding multiple intermediate stops
- **WHEN** `addIntermediateStop` is called twice for a trip that initially has a starting point (order 0) and destination (order 1)
- **THEN** after the first call the destination is at order 2 and the first intermediate is at order 1; after the second call the destination is at order 3, the first intermediate is at order 2, and the second intermediate is at order 1 (but reordering aside, after observation they are returned sorted by order ascending)

#### Scenario: DAO throws exception during addIntermediateStop
- **WHEN** the DAO throws an exception during the shift or insert in `addIntermediateStop`
- **THEN** `Result.failure` with the exception is returned and no partial changes are persisted

#### Scenario: getStopCount returns correct count
- **WHEN** `getStopCount` is called for a trip with 3 stops
- **THEN** `3` is returned
