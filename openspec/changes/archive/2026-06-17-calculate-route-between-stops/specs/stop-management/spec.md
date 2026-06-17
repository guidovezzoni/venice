## MODIFIED Requirements

### Requirement: SetStopUseCase validates and delegates by StopType
`SetStopUseCase` SHALL accept `tripId: String`, `placeName: String`, `latitude: Double`, `longitude: Double`, `stopType: StopType` and:
1. Trim the place name.
2. When `stopType` is `INTERMEDIATE`: query `StopRepository.getStopCount(tripId)` and return `Result.failure(IllegalStateException("Maximum of 25 stops reached"))` if count >= 25.
3. Delegate to the correct repository method based on `stopType`:
   - `STARTING_POINT` → `StopRepository.upsertStartingPoint(tripId, trimmedName, latitude, longitude)`
   - `DESTINATION` → `StopRepository.upsertDestination(tripId, trimmedName, latitude, longitude)`
   - `INTERMEDIATE` → `StopRepository.addIntermediateStop(tripId, trimmedName, latitude, longitude)`
4. On success, call `InvalidateRouteUseCase(tripId)` to delete all legs for the trip.
5. Return the repository result.

#### Scenario: Starting point — successful invocation
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and valid parameters
- **THEN** `StopRepository.upsertStartingPoint` is called with the trimmed place name, `InvalidateRouteUseCase` is called with the `tripId`, and `Result.success(Stop)` is returned

#### Scenario: Destination — successful invocation
- **WHEN** `invoke` is called with `stopType = DESTINATION` and valid parameters
- **THEN** `StopRepository.upsertDestination` is called with the trimmed place name, `InvalidateRouteUseCase` is called with the `tripId`, and `Result.success(Stop)` is returned

#### Scenario: Intermediate — successful invocation below limit
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and the trip has fewer than 25 stops
- **THEN** `StopRepository.addIntermediateStop` is called with the trimmed place name, `InvalidateRouteUseCase` is called with the `tripId`, and `Result.success(Stop)` is returned

#### Scenario: Intermediate — stop count at limit rejected
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and `StopRepository.getStopCount(tripId)` returns 25
- **THEN** `Result.failure` is returned with an `IllegalStateException`, `addIntermediateStop` is not called, and `InvalidateRouteUseCase` is not called

#### Scenario: Starting point — place name is trimmed
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and `placeName = "  Rome  "`
- **THEN** the repository receives `placeName = "Rome"`

#### Scenario: Destination — place name is trimmed
- **WHEN** `invoke` is called with `stopType = DESTINATION` and `placeName = "  Barcelona  "`
- **THEN** the repository receives `placeName = "Barcelona"`

#### Scenario: Intermediate — place name is trimmed
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and `placeName = "  Florence  "`
- **THEN** the repository receives `placeName = "Florence"`

#### Scenario: Starting point — repository failure propagated
- **WHEN** `invoke` is called with `stopType = STARTING_POINT` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called

#### Scenario: Destination — repository failure propagated
- **WHEN** `invoke` is called with `stopType = DESTINATION` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called

#### Scenario: Intermediate — repository failure propagated
- **WHEN** `invoke` is called with `stopType = INTERMEDIATE` and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called

### Requirement: MoveStopUseCase validates and performs stop reorder
`MoveStopUseCase` SHALL accept `tripId: String`, `fromOrder: Int`, and `toOrder: Int`, and:
1. Delegate to `StopRepository.swapStopOrder(tripId, fromOrder, toOrder)`.
2. On success, call `InvalidateRouteUseCase(tripId)` to delete all legs for the trip.
3. Return `Result.success(Unit)` on success.
4. Return `Result.failure` if the repository operation fails (without calling `InvalidateRouteUseCase`).

#### Scenario: Moving an intermediate stop up — happy path
- **WHEN** `invoke` is called with `fromOrder = 2` and `toOrder = 1` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `StopRepository.swapStopOrder` is called with `(tripId, 2, 1)`, `InvalidateRouteUseCase` is called with `tripId`, and `Result.success(Unit)` is returned

#### Scenario: Moving an intermediate stop down — happy path
- **WHEN** `invoke` is called with `fromOrder = 1` and `toOrder = 2` for a trip with stops at orders `[0, 1, 2, 3]`
- **THEN** `StopRepository.swapStopOrder` is called with `(tripId, 1, 2)`, `InvalidateRouteUseCase` is called with `tripId`, and `Result.success(Unit)` is returned

#### Scenario: Repository failure is propagated
- **WHEN** `invoke` is called and `StopRepository.swapStopOrder` returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called

### Requirement: EditStopUseCase validates and delegates
`EditStopUseCase` SHALL accept `stopId: String`, `placeName: String`, `latitude: Double`, `longitude: Double` and:
1. Trim the `placeName`.
2. Delegate to `StopRepository.updateStop(stopId, trimmedName, latitude, longitude)`.
3. On success, call `InvalidateRouteUseCase(result.tripId)` to delete all legs for the trip.
4. Return the repository result.

#### Scenario: Successful edit
- **WHEN** `invoke` is called with valid parameters and the repository returns `Result.success`
- **THEN** `Result.success(Stop)` is returned with the updated stop and `InvalidateRouteUseCase` is called with the stop's `tripId`

#### Scenario: Place name is trimmed
- **WHEN** `invoke` is called with `placeName = "  Florence  "`
- **THEN** the repository receives `placeName = "Florence"`

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called
