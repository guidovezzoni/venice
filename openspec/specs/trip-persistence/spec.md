# Trip Persistence

## Purpose

Defines the requirements for the local persistence layer for trips, covering the Room database setup, the `TripEntity` schema, DAO operations, and the domain model mapping.

## Requirements

### Requirement: Room database exists with trips table
The system SHALL provide a Room database (`AppDatabase`) containing a `trips` table and a `stops` table. The database SHALL be a singleton accessible via the application context. The database version SHALL be `2`, with `MIGRATION_1_2` registered via `addMigrations()`. The database SHALL expose a `stopDao()` abstract method.

#### Scenario: Database initialisation
- **WHEN** the application starts
- **THEN** the Room database is initialised with both `trips` and `stops` tables available for read/write operations

#### Scenario: Migration from version 1 to 2
- **WHEN** the app is upgraded from database version 1 to version 2
- **THEN** `MIGRATION_1_2` creates the `stops` table and its `index_stops_tripId` index without data loss in the `trips` table

### Requirement: TripEntity schema
The `trips` table SHALL store rows with the following columns:
- `id` (String, primary key, UUID v4 generated client-side)
- `name` (String, non-null)
- `createdAt` (Long, epoch milliseconds, non-null)
- `updatedAt` (Long, epoch milliseconds, non-null)

#### Scenario: Entity matches schema
- **WHEN** a `TripEntity` is inserted into the database
- **THEN** all four columns (`id`, `name`, `createdAt`, `updatedAt`) are persisted correctly

### Requirement: TripDao provides insert operation
The `TripDao` SHALL expose a `suspend fun insert(trip: TripEntity)` method that inserts a trip. The ID is a UUID set by the caller before insertion.

#### Scenario: Successful insert persists entity
- **WHEN** a `TripEntity` with a UUID `id` is inserted
- **THEN** the entity is persisted and can be retrieved by its ID

### Requirement: TripDao provides observe-all query
The `TripDao` SHALL expose a `fun observeAll(): Flow<List<TripEntity>>` method that returns all trips ordered by `createdAt` descending.

#### Scenario: Observing trips reflects inserts
- **WHEN** a new trip is inserted after a collector is active on `observeAll()`
- **THEN** the collector receives an updated list containing the new trip

### Requirement: TripDao provides get-by-ID query
The `TripDao` SHALL expose a `suspend fun getById(id: String): TripEntity?` method that returns a single trip or null.

#### Scenario: Querying an existing trip by ID
- **WHEN** `getById` is called with a valid trip ID
- **THEN** the corresponding `TripEntity` is returned

#### Scenario: Querying a non-existent trip by ID
- **WHEN** `getById` is called with an ID that does not exist
- **THEN** `null` is returned

### Requirement: Domain model mapping
A mapper function SHALL convert between `TripEntity` and the domain `Trip` model. The domain `Trip` model SHALL have the same fields as `TripEntity` (`id: String`, `name`, `createdAt`, `updatedAt`).

#### Scenario: Entity to domain mapping
- **WHEN** a `TripEntity` is mapped to a `Trip`
- **THEN** all field values are preserved identically

### Requirement: MIGRATION_1_2 creates stops table
`MIGRATION_1_2` SHALL be a `Migration(1, 2)` that executes SQL to create the `stops` table with columns (`id TEXT NOT NULL PRIMARY KEY`, `tripId TEXT NOT NULL`, `placeName TEXT NOT NULL`, `latitude REAL NOT NULL`, `longitude REAL NOT NULL`, `order INTEGER NOT NULL`, `status TEXT NOT NULL`) and a foreign key on `tripId` referencing `trips(id)` with `ON DELETE CASCADE`. It SHALL also create the `index_stops_tripId` index.

#### Scenario: Migration SQL creates correct schema
- **WHEN** `MIGRATION_1_2.migrate` is executed
- **THEN** the `stops` table exists with all required columns, constraints, and the `index_stops_tripId` index

### Requirement: DatabaseModule provides StopDao
`DatabaseModule` SHALL provide a `StopDao` instance by calling `appDatabase.stopDao()`.

#### Scenario: StopDao is injectable
- **WHEN** a class requests `StopDao` via dependency injection
- **THEN** Hilt provides the instance from `AppDatabase.stopDao()`

### Requirement: RepositoryModule binds StopRepository
`RepositoryModule` SHALL bind `StopRepositoryImpl` to `StopRepository`.

#### Scenario: StopRepository is injectable
- **WHEN** a class requests `StopRepository` via dependency injection
- **THEN** Hilt provides an instance of `StopRepositoryImpl`
