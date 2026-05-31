# Stop Removal

## Purpose

Defines the DAO operations and use case for removing a stop from a trip, including order compaction to keep stop orders contiguous after deletion.

## Requirements

### Requirement: StopDao deleteById query
`StopDao` SHALL provide a `deleteById(stopId: String)` method annotated with `@Query("DELETE FROM stops WHERE id = :stopId")` that deletes the stop with the given primary key.

#### Scenario: Delete existing stop
- **WHEN** `deleteById` is called with a valid stop ID that exists in the database
- **THEN** the stop row is removed from the `stops` table

#### Scenario: Delete non-existent stop
- **WHEN** `deleteById` is called with a stop ID that does not exist
- **THEN** no rows are affected and no exception is thrown

### Requirement: StopDao decrementOrderAbove query
`StopDao` SHALL provide a `decrementOrderAbove(tripId: String, fromOrder: Int)` method annotated with `@Query("UPDATE stops SET order = order - 1 WHERE tripId = :tripId AND order > :fromOrder")` that decrements the `order` of all stops in the given trip whose `order` is strictly greater than `fromOrder`.

#### Scenario: Decrement orders after deletion
- **WHEN** `decrementOrderAbove` is called with `tripId = "trip-1"` and `fromOrder = 2` for a trip with stops at orders `[0, 1, 2, 3, 4]`
- **THEN** stops at orders 3 and 4 become orders 2 and 3 respectively; stops at orders 0 and 1 are unchanged

#### Scenario: No stops above fromOrder
- **WHEN** `decrementOrderAbove` is called with `fromOrder` equal to the highest order in the trip
- **THEN** no rows are affected

### Requirement: StopDao deleteAndReorder transaction
`StopDao` SHALL provide a `deleteAndReorder(tripId: String, stopId: String)` method annotated with `@Transaction` that:
1. Queries the stop to delete via `getStopById(stopId)` to obtain its `order`.
2. Deletes the stop via `deleteById(stopId)`.
3. Decrements the `order` of all remaining stops in the same trip where `order > deletedOrder` via `decrementOrderAbove(tripId, deletedOrder)`.

If the stop does not exist, the method SHALL throw an `IllegalStateException`.

#### Scenario: Delete intermediate stop and reorder
- **WHEN** `deleteAndReorder` is called for a stop at order 2 in a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** the stop at order 2 is deleted, and the stop formerly at order 3 becomes order 2

#### Scenario: Delete starting point and promote
- **WHEN** `deleteAndReorder` is called for the stop at order 0 in a trip with stops at orders `[0, 1, 2]`
- **THEN** the stop at order 0 is deleted, the former order-1 stop becomes order 0, and the former order-2 stop becomes order 1

#### Scenario: Delete destination (highest order)
- **WHEN** `deleteAndReorder` is called for the stop at order 3 (the highest) in a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** the stop at order 3 is deleted, and stops at orders 0, 1, 2 are unchanged

#### Scenario: Delete last remaining stop
- **WHEN** `deleteAndReorder` is called for the only stop in a trip (order 0)
- **THEN** the stop is deleted and the trip has zero stops

#### Scenario: Stop not found
- **WHEN** `deleteAndReorder` is called with a `stopId` that does not exist
- **THEN** an `IllegalStateException` is thrown

### Requirement: RemoveStopUseCase delegates to repository
`RemoveStopUseCase` SHALL accept `tripId: String` and `stopId: String`, delegate to `StopRepository.deleteStop(tripId, stopId)`, and return the repository result. It SHALL include a TODO comment for leg invalidation (deferred to Epic 3).

#### Scenario: Successful removal
- **WHEN** `invoke` is called and the repository returns `Result.success(Unit)`
- **THEN** `Result.success(Unit)` is returned

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller
