## MODIFIED Requirements

### Requirement: RemoveStopUseCase delegates to repository
`RemoveStopUseCase` SHALL accept `tripId: String` and `stopId: String`, delegate to `StopRepository.deleteStop(tripId, stopId)`, and on success call `InvalidateRouteUseCase(tripId)` to delete all legs for the trip. Return the repository result.

#### Scenario: Successful removal
- **WHEN** `invoke` is called and the repository returns `Result.success(Unit)`
- **THEN** `InvalidateRouteUseCase` is called with `tripId` and `Result.success(Unit)` is returned

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller and `InvalidateRouteUseCase` is not called
