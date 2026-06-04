# Stop Progress

## Purpose

Defines the domain logic for tracking trip progress by marking stops as departed and undoing that action, including the derived "current stop" concept and validation rules.

## ADDED Requirements

### Requirement: MarkStopDepartedUseCase marks the current stop as VISITED
`MarkStopDepartedUseCase` SHALL accept `tripId: String` and `stopId: String` and:
1. Query all stops for the trip via `StopRepository.observeStopsForTrip(tripId)` (first emission).
2. Determine the current stop: the first stop with `status = PENDING` ordered by `order` ascending.
3. Validate that `stopId` matches the current stop's `id`. If not, return `Result.failure(IllegalStateException("Only the current stop can be marked as departed"))`.
4. Call `StopRepository.updateStopStatus(stopId, StopStatus.VISITED)`.
5. Return `Result.success(Unit)` on success, or `Result.failure` if the repository call fails.

#### Scenario: Marking the current stop as departed — success
- **WHEN** `invoke` is called with a `stopId` that matches the first PENDING stop by order
- **THEN** `StopRepository.updateStopStatus(stopId, StopStatus.VISITED)` is called and `Result.success(Unit)` is returned

#### Scenario: Marking a non-current stop — rejected
- **WHEN** `invoke` is called with a `stopId` that does NOT match the first PENDING stop by order
- **THEN** `Result.failure` is returned with message "Only the current stop can be marked as departed" and no repository update occurs

#### Scenario: No PENDING stops exist — rejected
- **WHEN** `invoke` is called and all stops in the trip have `status = VISITED`
- **THEN** `Result.failure` is returned with message "Only the current stop can be marked as departed"

#### Scenario: Repository failure is propagated
- **WHEN** `invoke` is called with a valid current stop but `StopRepository.updateStopStatus` fails
- **THEN** `Result.failure` with the repository exception is returned

### Requirement: UndoMarkStopDepartedUseCase reverts the last departed stop
`UndoMarkStopDepartedUseCase` SHALL accept `tripId: String` and:
1. Query all stops for the trip via `StopRepository.observeStopsForTrip(tripId)` (first emission).
2. Find the last departed stop: the stop with `status = VISITED` and the highest `order`.
3. If no VISITED stop exists, return `Result.failure(IllegalStateException("No departed stop to undo"))`.
4. Call `StopRepository.updateStopStatus(lastDepartedStop.id, StopStatus.PENDING)`.
5. Return `Result.success(Unit)` on success, or `Result.failure` if the repository call fails.

#### Scenario: Undoing the last departed stop — success
- **WHEN** `invoke` is called and at least one stop has `status = VISITED`
- **THEN** `StopRepository.updateStopStatus` is called with the highest-order VISITED stop's ID and `StopStatus.PENDING`, and `Result.success(Unit)` is returned

#### Scenario: No departed stops to undo
- **WHEN** `invoke` is called and no stops have `status = VISITED`
- **THEN** `Result.failure` is returned with message "No departed stop to undo"

#### Scenario: Multiple departed stops — only last is reverted
- **WHEN** `invoke` is called and stops at orders 0, 1, 2 are VISITED
- **THEN** only the stop at order 2 is reverted to PENDING; stops at orders 0 and 1 remain VISITED

#### Scenario: Repository failure is propagated
- **WHEN** `invoke` is called with a valid last departed stop but `StopRepository.updateStopStatus` fails
- **THEN** `Result.failure` with the repository exception is returned
