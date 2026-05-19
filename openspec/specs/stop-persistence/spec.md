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
- `suspend fun getDestination(tripId: String): StopEntity?` — returns the stop with the highest `order` where `order > 0` for the given trip, or null.
- `suspend fun insert(stop: StopEntity)` — inserts a new stop.
- `suspend fun update(stop: StopEntity)` — updates an existing stop.

#### Scenario: Querying starting point when one exists
- **WHEN** `getStartingPoint` is called for a trip that has a stop with `order = 0`
- **THEN** that `StopEntity` is returned

#### Scenario: Querying starting point when none exists
- **WHEN** `getStartingPoint` is called for a trip with no `order = 0` stop
- **THEN** `null` is returned

#### Scenario: Querying destination when one exists
- **WHEN** `getDestination` is called for a trip that has a stop with `order > 0`
- **THEN** the `StopEntity` with the highest `order` is returned

#### Scenario: Querying destination when none exists
- **WHEN** `getDestination` is called for a trip with no `order > 0` stop
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

### Requirement: StopDao provides order-shifting query
The `StopDao` SHALL expose `suspend fun incrementOrderFrom(tripId: String, fromOrder: Int)` that increments the `order` column by 1 for all stops in the given trip where `order >= fromOrder`.

#### Scenario: Shifting orders from a given position
- **WHEN** `incrementOrderFrom("t1", 1)` is called and stops exist with orders `[0, 1, 2]`
- **THEN** orders become `[0, 2, 3]`

#### Scenario: Shifting with no matching stops
- **WHEN** `incrementOrderFrom("t1", 5)` is called and no stops have `order >= 5`
- **THEN** no rows are updated

#### Scenario: Only affects the specified trip
- **WHEN** `incrementOrderFrom("t1", 1)` is called
- **THEN** stops belonging to other trips are not affected

### Requirement: StopDao provides stop count query
The `StopDao` SHALL expose `suspend fun getStopCount(tripId: String): Int` that returns the total number of stops for the given trip.

#### Scenario: Counting stops for a trip
- **WHEN** `getStopCount("t1")` is called and 3 stops exist for trip "t1"
- **THEN** `3` is returned

#### Scenario: Counting stops for a trip with no stops
- **WHEN** `getStopCount("t1")` is called and no stops exist for trip "t1"
- **THEN** `0` is returned

### Requirement: StopMapper converts between entity and domain model
A mapper function SHALL convert `StopEntity` to the domain `Stop` model. The `status` string field SHALL be mapped to the `StopStatus` enum.

#### Scenario: Entity to domain mapping
- **WHEN** a `StopEntity` with `status = "PENDING"` is mapped to a `Stop`
- **THEN** all field values are preserved and `status` is `StopStatus.PENDING`

#### Scenario: Entity to domain mapping with VISITED status
- **WHEN** a `StopEntity` with `status = "VISITED"` is mapped to a `Stop`
- **THEN** `status` is `StopStatus.VISITED`
