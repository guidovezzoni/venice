## REMOVED Requirements

### Requirement: AddIntermediateStopUseCase validates and delegates
**Reason**: Replaced by `SetStopUseCase` with `stopType = StopType.INTERMEDIATE`. The stop-count validation and place-name trimming are now handled by the consolidated use case.
**Migration**: All callers should use `SetStopUseCase(tripId, placeName, latitude, longitude, StopType.INTERMEDIATE)` instead of `AddIntermediateStopUseCase(tripId, placeName, latitude, longitude)`.
