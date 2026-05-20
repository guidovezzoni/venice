## REMOVED Requirements

### Requirement: SetDestinationUseCase validates and delegates
**Reason**: Replaced by `SetStopUseCase` with `stopType = StopType.DESTINATION`. The place-name trimming is now handled by the consolidated use case.
**Migration**: All callers should use `SetStopUseCase(tripId, placeName, latitude, longitude, StopType.DESTINATION)` instead of `SetDestinationUseCase(tripId, placeName, latitude, longitude)`.
