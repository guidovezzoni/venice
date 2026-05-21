## ADDED Requirements

### Requirement: StopDao provides lookup by trip and order
The `StopDao` SHALL expose `suspend fun getStopByTripIdAndOrder(tripId: String, order: Int): StopEntity?` that returns the stop at the given order position for the specified trip, or `null` if no stop exists at that position.

#### Scenario: Querying a stop that exists at the given order
- **WHEN** `getStopByTripIdAndOrder("t1", 2)` is called and a stop exists with `tripId = "t1"` and `order = 2`
- **THEN** that `StopEntity` is returned

#### Scenario: Querying a stop at a non-existent order
- **WHEN** `getStopByTripIdAndOrder("t1", 99)` is called and no stop exists at that order
- **THEN** `null` is returned

#### Scenario: Only matches the specified trip
- **WHEN** `getStopByTripIdAndOrder("t1", 1)` is called and a stop with `order = 1` exists only for trip "t2"
- **THEN** `null` is returned

### Requirement: StopDao provides order update for a single stop
The `StopDao` SHALL expose `suspend fun updateStopOrder(stopId: String, newOrder: Int)` that updates the `order` column of the stop with the given `id` to `newOrder`.

#### Scenario: Updating order of an existing stop
- **WHEN** `updateStopOrder("s1", 3)` is called and stop "s1" exists with `order = 1`
- **THEN** the stop's `order` is updated to `3`

#### Scenario: Updating order of a non-existent stop
- **WHEN** `updateStopOrder("nonexistent", 3)` is called
- **THEN** no rows are updated and no exception is thrown
