## MODIFIED Requirements

### Requirement: TripRepository interface in domain layer
The domain layer SHALL define a `TripRepository` interface with:
- `suspend fun createTrip(name: String): Result<Trip>`
- `fun observeTrips(): Flow<List<Trip>>`
- `fun observeTripById(tripId: String): Flow<Trip?>` — observes a single trip by ID, emitting `null` if the trip does not exist

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case or ViewModel references `TripRepository`
- **THEN** it depends only on the domain layer, not the data layer

#### Scenario: Observing a single trip by ID
- **WHEN** `observeTripById(tripId)` is called for an existing trip
- **THEN** a `Flow<Trip?>` is returned that emits the current `Trip` (mapped from `TripWithStopCount`) whenever the underlying data changes

### Requirement: TripRepositoryImpl persists trips
`TripRepositoryImpl` SHALL implement `TripRepository`. The `createTrip` method SHALL generate a UUID for the ID, trim the name, set `createdAt` and `updatedAt` to the current epoch milliseconds, insert via `TripDao`, and return the created `Trip` wrapped in `Result.success`. The `observeTripById` method SHALL delegate to `TripDao.observeById(tripId)` and map each non-null emission to a domain `Trip` via the existing `TripWithStopCount.toDomain()` mapper, passing through `null` unchanged.

#### Scenario: Successful trip creation
- **WHEN** `createTrip("  Summer Drive  ")` is called
- **THEN** a `TripEntity` with `name = "Summer Drive"` and a UUID `id` is inserted and a `Result.success(Trip)` is returned

#### Scenario: DAO insert failure
- **WHEN** the DAO throws an exception during insert
- **THEN** `createTrip` returns `Result.failure` with the exception

#### Scenario: Observing a single trip maps DAO emissions to domain
- **WHEN** `TripDao.observeById(tripId)` emits a `TripWithStopCount` for `tripId`
- **THEN** `observeTripById(tripId)` emits the equivalent domain `Trip`, with `name`, `createdAt`, `updatedAt`, and `stopCount` preserved

#### Scenario: Observing a missing trip emits null
- **WHEN** `TripDao.observeById(tripId)` emits `null` for a trip ID that does not exist
- **THEN** `observeTripById(tripId)` emits `null`

## ADDED Requirements

### Requirement: ObserveTripUseCase wraps repository observation
`ObserveTripUseCase` SHALL expose `operator fun invoke(tripId: String): Flow<Trip?>` that delegates to `TripRepository.observeTripById`, mirroring the existing `ObserveStopsUseCase` / `ObserveLegsUseCase` shape (a single-argument, `Flow`-returning pass-through with no additional logic).

#### Scenario: Trip flow is returned
- **WHEN** `invoke` is called with a trip ID
- **THEN** the `Flow<Trip?>` from `TripRepository.observeTripById` is returned unchanged
