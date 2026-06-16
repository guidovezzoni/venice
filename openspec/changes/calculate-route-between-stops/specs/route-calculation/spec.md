## ADDED Requirements

### Requirement: Leg domain model
The domain layer SHALL define a `Leg` data class in `domain/model/Leg.kt` with fields:
- `id: String` — UUID primary key
- `tripId: String` — FK to Trip
- `fromStopId: String` — FK to the origin Stop of this leg
- `toStopId: String` — FK to the destination Stop of this leg
- `distanceMetres: Int` — distance in metres
- `durationSeconds: Int` — duration in seconds
- `encodedPolyline: String` — Google-encoded polyline string

#### Scenario: Leg model is a data class
- **WHEN** a `Leg` instance is created with all fields
- **THEN** it holds all fields and supports equality, copy, and destructuring

### Requirement: LegEntity Room entity
The data layer SHALL define a `LegEntity` Room entity in `data/database/entity/LegEntity.kt` for the `legs` table with:
- `id: String` as `@PrimaryKey`
- `tripId: String` with a foreign key to `trips.id` (ON DELETE CASCADE)
- `fromStopId: String`
- `toStopId: String`
- `distanceMetres: Int`
- `durationSeconds: Int`
- `encodedPolyline: String`
- An index on `tripId`

#### Scenario: LegEntity maps to legs table
- **WHEN** `LegEntity` is annotated with `@Entity(tableName = "legs")`
- **THEN** Room generates the `legs` table with all columns and constraints

### Requirement: LegDao provides leg persistence operations
The data layer SHALL define a `LegDao` interface in `data/database/dao/LegDao.kt` with:
- `suspend fun insertAll(legs: List<LegEntity>)` — batch insert using `@Insert`
- `suspend fun deleteByTripId(tripId: String)` — delete all legs for a trip using `@Query`
- `fun observeByTripId(tripId: String): Flow<List<LegEntity>>` — observe legs for a trip, ordered by the from-stop's order (join with stops table)
- `suspend fun getByTripId(tripId: String): List<LegEntity>` — get all legs for a trip

#### Scenario: Insert all legs
- **WHEN** `insertAll` is called with a list of 3 `LegEntity` objects
- **THEN** all 3 are inserted into the `legs` table

#### Scenario: Delete by trip ID
- **WHEN** `deleteByTripId` is called with `tripId = "trip-1"`
- **THEN** all legs with `tripId = "trip-1"` are deleted

#### Scenario: Observe by trip ID
- **WHEN** `observeByTripId` is called with `tripId = "trip-1"` and 2 legs exist for that trip
- **THEN** a `Flow` emitting a list of 2 `LegEntity` objects is returned

### Requirement: LegEntityMapper maps between entity and domain
The data layer SHALL define extension functions in `data/database/mapper/LegEntityMapper.kt`:
- `LegEntity.toDomain(): Leg` — maps entity to domain model
- `Leg.toEntity(): LegEntity` — maps domain model to entity

#### Scenario: Entity to domain mapping
- **WHEN** a `LegEntity` is mapped via `toDomain()`
- **THEN** all fields are copied to the resulting `Leg` domain model

#### Scenario: Domain to entity mapping
- **WHEN** a `Leg` is mapped via `toEntity()`
- **THEN** all fields are copied to the resulting `LegEntity`

### Requirement: Database migration 2 to 3
`AppDatabase` SHALL include `MIGRATION_2_3` that creates the `legs` table with all columns, the foreign key to `trips`, and the index on `tripId`. The database version SHALL be updated to 3, and `LegEntity` SHALL be included in the `@Database` entities list.

#### Scenario: Migration creates legs table
- **WHEN** the database is upgraded from version 2 to version 3
- **THEN** the `legs` table exists with columns `id`, `tripId`, `fromStopId`, `toStopId`, `distanceMetres`, `durationSeconds`, `encodedPolyline`, a foreign key on `tripId` referencing `trips(id)` with ON DELETE CASCADE, and an index on `tripId`

### Requirement: DirectionsApiService calls Google Directions API
The data layer SHALL define a `DirectionsApiService` class in `data/network/DirectionsApiService.kt` that:
- Accepts an `OkHttpClient` and the API key via constructor injection.
- Provides `suspend fun getDirections(origin: String, destination: String, waypoints: List<String>): Result<DirectionsResponse>`.
- Constructs a GET request to `https://maps.googleapis.com/maps/api/directions/json` with query parameters `origin`, `destination`, `waypoints` (pipe-separated), and `key`.
- Parses the JSON response using `org.json` into a `DirectionsResponse`.
- Returns `Result.failure` if the HTTP status is not 200, if the API response status is not `"OK"`, or if parsing fails.
- Executes the OkHttp call using `execute()` (synchronous) within the caller's coroutine context.
- SHALL be the only class that imports or references OkHttp types. All OkHttp usage is isolated here so the HTTP layer can be replaced with Retrofit when additional endpoints are added.
- SHALL include a TODO comment: `// TODO: Replace with Retrofit when additional API endpoints are added`

#### Scenario: Successful API call with waypoints
- **WHEN** `getDirections` is called with origin `"41.9,12.5"`, destination `"41.4,2.2"`, and waypoints `["43.8,11.3", "43.7,7.3"]`
- **THEN** a GET request is made with `origin=41.9,12.5&destination=41.4,2.2&waypoints=43.8,11.3|43.7,7.3&key=<API_KEY>` and a `Result.success(DirectionsResponse)` is returned

#### Scenario: Successful API call without waypoints
- **WHEN** `getDirections` is called with origin and destination but an empty waypoints list
- **THEN** the request omits the `waypoints` parameter and returns a `Result.success(DirectionsResponse)`

#### Scenario: HTTP error
- **WHEN** the API returns a non-200 HTTP status code
- **THEN** `Result.failure` is returned with an appropriate error message

#### Scenario: API error status
- **WHEN** the API returns HTTP 200 but the JSON `status` field is not `"OK"` (e.g., `"ZERO_RESULTS"`, `"REQUEST_DENIED"`)
- **THEN** `Result.failure` is returned with the API status as the error message

#### Scenario: Network failure
- **WHEN** the OkHttp call throws an `IOException`
- **THEN** `Result.failure` is returned with the exception

### Requirement: DirectionsResponse DTO
The data layer SHALL define `DirectionsResponse` in `data/network/dto/DirectionsResponse.kt` as a data class with:
- `status: String` — API response status
- `legs: List<DirectionsLeg>` — list of leg results

And `DirectionsLeg` as a data class with:
- `distanceMetres: Int` — `routes[0].legs[n].distance.value`
- `durationSeconds: Int` — `routes[0].legs[n].duration.value`
- `encodedPolyline: String` — `routes[0].legs[n].overview_polyline.points`

And a companion `fromJson(jsonString: String): DirectionsResponse` factory that parses the raw JSON.

#### Scenario: Parse valid API response
- **WHEN** `fromJson` is called with a valid Directions API JSON string containing 2 legs
- **THEN** a `DirectionsResponse` is returned with `status = "OK"` and 2 `DirectionsLeg` entries with correct values

#### Scenario: Parse response with missing routes
- **WHEN** `fromJson` is called with a JSON string where `routes` is empty
- **THEN** a `DirectionsResponse` is returned with an empty `legs` list

### Requirement: LegMapper maps DTO to domain
The data layer SHALL define a `LegMapper` class in `data/mapper/LegMapper.kt` that maps a `DirectionsLeg` to a domain `Leg`, accepting `tripId`, `fromStopId`, and `toStopId` as additional parameters. A new UUID SHALL be generated for each mapped leg.

#### Scenario: Map DirectionsLeg to Leg
- **WHEN** a `DirectionsLeg` with `distanceMetres = 1000`, `durationSeconds = 600`, `encodedPolyline = "abc"` is mapped with `tripId = "t1"`, `fromStopId = "s1"`, `toStopId = "s2"`
- **THEN** a `Leg` is returned with `tripId = "t1"`, `fromStopId = "s1"`, `toStopId = "s2"`, `distanceMetres = 1000`, `durationSeconds = 600`, `encodedPolyline = "abc"`, and a non-empty UUID `id`

### Requirement: RouteRepository interface
The domain layer SHALL define a `RouteRepository` interface in `domain/repository/RouteRepository.kt` with:
- `suspend fun calculateRoute(tripId: String, stops: List<Stop>): Result<List<Leg>>` — calls the Directions API with the ordered stops, persists the resulting legs, and returns them
- `suspend fun deleteLegsForTrip(tripId: String): Result<Unit>` — deletes all legs for a trip
- `fun observeLegsForTrip(tripId: String): Flow<List<Leg>>` — observes legs for a trip

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `RouteRepository`
- **THEN** it depends only on the domain layer, not the data layer

### Requirement: RouteRepositoryImpl implements RouteRepository
`RouteRepositoryImpl` SHALL:
1. Accept `DirectionsApiService`, `LegDao`, `LegMapper`, and `LegEntityMapper` via constructor injection.
2. In `calculateRoute`: format stop coordinates as `"lat,lng"` strings, extract origin (first stop), destination (last stop), and waypoints (intermediate stops), call `DirectionsApiService.getDirections`, map each `DirectionsLeg` to a domain `Leg` using `LegMapper` with the correct `fromStopId`/`toStopId` pairs, delete existing legs for the trip, insert all new leg entities, and return `Result.success(legs)`.
3. In `deleteLegsForTrip`: call `LegDao.deleteByTripId` and return `Result.success(Unit)`.
4. In `observeLegsForTrip`: delegate to `LegDao.observeByTripId` and map entities to domain models.
5. Wrap all operations in `runCatching`.

#### Scenario: Calculate route — happy path
- **WHEN** `calculateRoute` is called with 3 stops and the API returns 2 legs
- **THEN** existing legs are deleted, 2 new legs are persisted, and `Result.success` with 2 `Leg` domain objects is returned

#### Scenario: Calculate route — API failure
- **WHEN** `calculateRoute` is called and the API returns failure
- **THEN** `Result.failure` is returned and no legs are persisted

#### Scenario: Delete legs for trip
- **WHEN** `deleteLegsForTrip` is called with `tripId = "t1"`
- **THEN** all legs with `tripId = "t1"` are deleted from the database and `Result.success(Unit)` is returned

#### Scenario: Observe legs for trip
- **WHEN** `observeLegsForTrip` is called with `tripId = "t1"`
- **THEN** a `Flow<List<Leg>>` is returned reflecting the current legs in the database

### Requirement: CalculateRouteUseCase orchestrates route calculation
`CalculateRouteUseCase` SHALL accept `tripId: String` and a `List<Stop>` (sorted by order) and:
1. Validate that at least 2 stops exist; return `Result.failure(IllegalStateException)` if not.
2. Delegate to `RouteRepository.calculateRoute(tripId, stops)`.
3. Return the repository result.

#### Scenario: Happy path with 3 stops
- **WHEN** `invoke` is called with `tripId = "t1"` and 3 stops sorted by order
- **THEN** `RouteRepository.calculateRoute` is called and `Result.success(List<Leg>)` is returned

#### Scenario: Fewer than 2 stops
- **WHEN** `invoke` is called with 1 stop
- **THEN** `Result.failure(IllegalStateException)` is returned without calling the repository

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

### Requirement: ObserveLegsUseCase exposes leg observation
`ObserveLegsUseCase` SHALL expose `operator fun invoke(tripId: String): Flow<List<Leg>>` that delegates to `RouteRepository.observeLegsForTrip`.

#### Scenario: Legs flow is returned
- **WHEN** `invoke` is called with a trip ID
- **THEN** a `Flow<List<Leg>>` from the repository is returned

### Requirement: InvalidateRouteUseCase deletes legs for a trip
`InvalidateRouteUseCase` SHALL accept `tripId: String` and delegate to `RouteRepository.deleteLegsForTrip(tripId)`.

#### Scenario: Successful invalidation
- **WHEN** `invoke` is called with `tripId = "t1"`
- **THEN** `RouteRepository.deleteLegsForTrip("t1")` is called and `Result.success(Unit)` is returned

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

### Requirement: NetworkModule provides OkHttpClient
The DI layer SHALL define a `NetworkModule` Hilt module in `di/NetworkModule.kt` that provides a singleton `OkHttpClient`. SHALL include a TODO comment: `// TODO: Replace with Retrofit module when additional API endpoints are added`

#### Scenario: OkHttpClient is singleton
- **WHEN** `OkHttpClient` is injected in multiple places
- **THEN** the same instance is reused

### Requirement: DatabaseModule exposes LegDao
`DatabaseModule` SHALL provide a `LegDao` via `AppDatabase.legDao()` and register `MIGRATION_2_3`.

#### Scenario: LegDao is provided
- **WHEN** `LegDao` is injected
- **THEN** it is the DAO instance from `AppDatabase`

### Requirement: RepositoryModule binds RouteRepository
`RepositoryModule` SHALL bind `RouteRepositoryImpl` to `RouteRepository` as a singleton.

#### Scenario: RouteRepository resolves to RouteRepositoryImpl
- **WHEN** `RouteRepository` is injected
- **THEN** it is an instance of `RouteRepositoryImpl`
