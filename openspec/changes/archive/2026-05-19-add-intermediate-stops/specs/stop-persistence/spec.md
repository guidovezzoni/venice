## ADDED Requirements

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
