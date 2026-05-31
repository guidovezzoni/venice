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
- `suspend fun deleteStop(tripId: String, stopId: String): Result<Unit>`

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `StopRepository`
- **THEN** it depends only on the domain layer, not the data layer

## ADDED Requirements

### Requirement: StopRepositoryImpl implements deleteStop
`StopRepositoryImpl.deleteStop` SHALL:
1. Delegate to `StopDao.deleteAndReorder(tripId, stopId)`.
2. Wrap the call in `runCatching` so any exception yields `Result.failure`.
3. Return `Result.success(Unit)` on success.

#### Scenario: Successful deletion
- **WHEN** `deleteStop` is called with a valid `tripId` and `stopId`
- **THEN** `StopDao.deleteAndReorder` is called and `Result.success(Unit)` is returned

#### Scenario: DAO throws exception
- **WHEN** `StopDao.deleteAndReorder` throws an exception during `deleteStop`
- **THEN** `Result.failure` with the exception is returned
