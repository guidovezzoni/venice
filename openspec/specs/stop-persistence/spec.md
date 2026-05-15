# Stop Persistence

## Purpose

Defines the requirements for the local persistence layer for stops, covering the `StopEntity` schema, DAO operations, and the domain model mapping.

## Requirements

### Requirement: StopEntity schema
The `stops` table SHALL store rows with the following columns:
- `id` (String, primary key, UUID v4 generated client-side)
- `tripId` (String, non-null, foreign key referencing `trips.id` with `ON DELETE CASCADE`)
- `placeName` (String, non-null)
- `latitude` (Real, non-null)
- `longitude` (Real, non-null)
- `order` (Integer, non-null)
- `status` (String, non-null, mapped to/from `StopStatus` enum)

The table SHALL have an index on `tripId`.

#### Scenario: Entity matches schema
- **WHEN** a `StopEntity` is inserted into the database
- **THEN** all seven columns (`id`, `tripId`, `placeName`, `latitude`, `longitude`, `order`, `status`) are persisted correctly

#### Scenario: Cascade delete removes stops when trip is deleted
- **WHEN** a trip is deleted from the `trips` table
- **THEN** all associated stops in the `stops` table are also deleted

### Requirement: StopDao provides upsert for starting point
The `StopDao` SHALL expose:
- `suspend fun getStartingPoint(tripId: String): StopEntity?` — returns the stop with `order = 0` for the given trip, or null.
- `suspend fun insert(stop: StopEntity)` — inserts a new stop.
- `suspend fun update(stop: StopEntity)` — updates an existing stop.

#### Scenario: Querying starting point when one exists
- **WHEN** `getStartingPoint` is called for a trip that has a stop with `order = 0`
- **THEN** that `StopEntity` is returned

#### Scenario: Querying starting point when none exists
- **WHEN** `getStartingPoint` is called for a trip with no `order = 0` stop
- **THEN** `null` is returned

#### Scenario: Inserting a new stop
- **WHEN** `insert` is called with a `StopEntity`
- **THEN** the entity is persisted and retrievable by its ID

#### Scenario: Updating an existing stop
- **WHEN** `update` is called with a modified `StopEntity`
- **THEN** the persisted row reflects the updated values

### Requirement: StopDao provides observe-by-trip query
The `StopDao` SHALL expose `fun observeByTripId(tripId: String): Flow<List<StopEntity>>` that returns all stops for the given trip ordered by `order` ascending.

#### Scenario: Observing stops reflects inserts
- **WHEN** a new stop is inserted for a trip after a collector is active on `observeByTripId`
- **THEN** the collector receives an updated list containing the new stop

#### Scenario: Stops are ordered by order field
- **WHEN** stops with `order = 0` and `order = 1` exist for a trip
- **THEN** `observeByTripId` returns them in ascending order (`0` first, `1` second)

### Requirement: StopMapper converts between entity and domain model
A mapper function SHALL convert `StopEntity` to the domain `Stop` model. The `status` string field SHALL be mapped to the `StopStatus` enum.

#### Scenario: Entity to domain mapping
- **WHEN** a `StopEntity` with `status = "PENDING"` is mapped to a `Stop`
- **THEN** all field values are preserved and `status` is `StopStatus.PENDING`

#### Scenario: Entity to domain mapping with VISITED status
- **WHEN** a `StopEntity` with `status = "VISITED"` is mapped to a `Stop`
- **THEN** `status` is `StopStatus.VISITED`
