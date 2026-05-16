## ADDED Requirements

### Requirement: SetDestinationUseCase validates and delegates
`SetDestinationUseCase` SHALL trim the place name and delegate to `StopRepository.upsertDestination`.

#### Scenario: Successful invocation
- **WHEN** `invoke` is called with valid parameters
- **THEN** the repository receives the trimmed place name and returns `Result.success(Stop)`

#### Scenario: Repository returns failure
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

#### Scenario: Place name is trimmed
- **WHEN** `invoke` is called with `placeName = "  Barcelona  "`
- **THEN** the repository receives `placeName = "Barcelona"`
