## ADDED Requirements

### Requirement: AddIntermediateStopUseCase validates and delegates
`AddIntermediateStopUseCase` SHALL accept `tripId: String`, `placeName: String`, `latitude: Double`, `longitude: Double` and delegate to `StopRepository.addIntermediateStop` after validation. The use case SHALL:
1. Trim the place name and reject it if blank (return `Result.failure`).
2. Reject latitude outside the range -90..90 (return `Result.failure`).
3. Reject longitude outside the range -180..180 (return `Result.failure`).
4. Query the current stop count for the trip via `StopRepository.getStopCount(tripId)` and reject if the count is already 25 or more (return `Result.failure`).
5. If all validations pass, call `StopRepository.addIntermediateStop(tripId, trimmedPlaceName, latitude, longitude)` and return its result.

#### Scenario: Successful invocation
- **WHEN** `invoke` is called with `tripId = "t1"`, `placeName = "Florence"`, `latitude = 43.77`, `longitude = 11.25` and the trip has fewer than 25 stops
- **THEN** `StopRepository.addIntermediateStop` is called with the trimmed place name and `Result.success(Stop)` is returned

#### Scenario: Blank place name rejected
- **WHEN** `invoke` is called with `placeName = "   "`
- **THEN** `Result.failure` is returned with an `IllegalArgumentException` and the repository is not called

#### Scenario: Latitude out of range rejected
- **WHEN** `invoke` is called with `latitude = 91.0`
- **THEN** `Result.failure` is returned with an `IllegalArgumentException`

#### Scenario: Longitude out of range rejected
- **WHEN** `invoke` is called with `longitude = -181.0`
- **THEN** `Result.failure` is returned with an `IllegalArgumentException`

#### Scenario: Stop count at maximum rejected
- **WHEN** `invoke` is called and `StopRepository.getStopCount(tripId)` returns 25
- **THEN** `Result.failure` is returned with an `IllegalStateException` and the repository's `addIntermediateStop` is not called

#### Scenario: Place name is trimmed
- **WHEN** `invoke` is called with `placeName = "  Florence  "`
- **THEN** the repository receives `placeName = "Florence"`

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called with valid inputs and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller
