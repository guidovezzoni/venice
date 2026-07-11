## ADDED Requirements

### Requirement: TripDao provides observe-by-ID query
The `TripDao` SHALL expose a `fun observeById(id: String): Flow<TripWithStopCount?>` method that returns a live `Flow` for a single trip, joined with its stop count using the same correlated-subquery shape as `observeAll()`, filtered by `WHERE t.id = :id`. The flow SHALL emit `null` when no trip with the given `id` exists.

#### Scenario: Observing an existing trip emits its data
- **WHEN** `observeById` is called with a trip ID that exists
- **THEN** the flow emits a `TripWithStopCount` with that trip's `name`, `createdAt`, `updatedAt`, and current stop count

#### Scenario: Observing a non-existent trip emits null
- **WHEN** `observeById` is called with an ID that does not exist
- **THEN** the flow emits `null`

#### Scenario: Observing reflects live updates
- **WHEN** a collector is active on `observeById(id)` and a stop is added for that trip
- **THEN** the collector receives an updated emission with the new stop count
