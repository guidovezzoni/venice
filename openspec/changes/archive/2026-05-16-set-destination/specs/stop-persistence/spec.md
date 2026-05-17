## MODIFIED Requirements

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
