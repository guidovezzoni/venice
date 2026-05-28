## ADDED Requirements

### Requirement: StopDao getStopById query
`StopDao` SHALL provide a `getStopById(stopId: String): StopEntity?` query that returns the stop entity matching the given primary key, or `null` if no such stop exists.

#### Scenario: Stop exists
- **WHEN** `getStopById` is called with a valid stop ID that exists in the database
- **THEN** the corresponding `StopEntity` is returned

#### Scenario: Stop does not exist
- **WHEN** `getStopById` is called with a stop ID that does not exist in the database
- **THEN** `null` is returned

### Requirement: StopRepository updateStop method
`StopRepository` SHALL define `suspend fun updateStop(stopId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>` that updates an existing stop's place name, latitude, and longitude while preserving its `id`, `tripId`, `order`, and `status`.

#### Scenario: Successful update
- **WHEN** `updateStop` is called with a valid `stopId` and new field values
- **THEN** the stop's `placeName`, `latitude`, and `longitude` are updated, its `id`, `tripId`, `order`, and `status` are preserved, and `Result.success(Stop)` is returned with the updated domain model

#### Scenario: Stop not found
- **WHEN** `updateStop` is called with a `stopId` that does not exist
- **THEN** `Result.failure` is returned with an `IllegalStateException`

#### Scenario: DAO throws exception during update
- **WHEN** the DAO throws an exception during the update operation
- **THEN** `Result.failure` with the exception is returned

### Requirement: StopRepositoryImpl implements updateStop
`StopRepositoryImpl.updateStop` SHALL:
1. Query `StopDao.getStopById(stopId)` to retrieve the existing entity.
2. If not found, throw `IllegalStateException("Stop not found: $stopId")`.
3. Copy the existing entity with new `placeName`, `latitude`, `longitude` values.
4. Call `StopDao.update()` with the modified entity.
5. Return `Result.success` with the updated entity mapped to the domain model.
6. Wrap the entire operation in `runCatching` so any exception yields `Result.failure`.

#### Scenario: Update preserves identity fields
- **WHEN** `updateStop` is called for a stop with `id = "abc"`, `tripId = "trip-1"`, `order = 2`, `status = PENDING`
- **THEN** after update, the stop retains `id = "abc"`, `tripId = "trip-1"`, `order = 2`, `status = PENDING` with only `placeName`, `latitude`, `longitude` changed

#### Scenario: Update with stop not found
- **WHEN** `updateStop` is called with `stopId = "nonexistent"`
- **THEN** `Result.failure(IllegalStateException("Stop not found: nonexistent"))` is returned and no DAO update is performed

### Requirement: EditStopUseCase validates and delegates
`EditStopUseCase` SHALL accept `stopId: String`, `placeName: String`, `latitude: Double`, `longitude: Double` and:
1. Trim the `placeName`.
2. Delegate to `StopRepository.updateStop(stopId, trimmedName, latitude, longitude)`.
3. Return the repository result.

#### Scenario: Successful edit
- **WHEN** `invoke` is called with valid parameters and the repository returns `Result.success`
- **THEN** `Result.success(Stop)` is returned with the updated stop

#### Scenario: Place name is trimmed
- **WHEN** `invoke` is called with `placeName = "  Florence  "`
- **THEN** the repository receives `placeName = "Florence"`

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

## MODIFIED Requirements

### Requirement: StopRepository interface
The domain layer SHALL define a `StopRepository` interface with:
- `suspend fun upsertStartingPoint(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun upsertDestination(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `fun observeStopsForTrip(tripId: String): Flow<List<Stop>>`
- `suspend fun addIntermediateStop(tripId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`
- `suspend fun getStopCount(tripId: String): Int`
- `suspend fun swapStopOrder(tripId: String, fromOrder: Int, toOrder: Int): Result<Unit>`
- `suspend fun updateStop(stopId: String, placeName: String, latitude: Double, longitude: Double): Result<Stop>`

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `StopRepository`
- **THEN** it depends only on the domain layer, not the data layer
