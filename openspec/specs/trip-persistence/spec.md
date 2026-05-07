# Trip Persistence

## Purpose

Defines the requirements for the local persistence layer for trips, covering the Room database setup, the `TripEntity` schema, DAO operations, and the domain model mapping.

## Requirements

### Requirement: Room database exists with trips table
The system SHALL provide a Room database (`AppDatabase`) containing a `trips` table. The database SHALL be a singleton accessible via the application context.

#### Scenario: Database initialisation
- **WHEN** the application starts
- **THEN** the Room database is initialised with the `trips` table available for read/write operations

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
