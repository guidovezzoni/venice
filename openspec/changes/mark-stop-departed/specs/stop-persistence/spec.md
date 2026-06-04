# Stop Persistence

## ADDED Requirements

### Requirement: StopDao provides targeted status update query
`StopDao` SHALL expose `suspend fun updateStopStatus(stopId: String, status: String)` that executes `UPDATE stops SET status = :status WHERE id = :stopId`.

#### Scenario: Updating status of an existing stop
- **WHEN** `updateStopStatus("s1", "VISITED")` is called and stop "s1" exists with `status = "PENDING"`
- **THEN** the stop's `status` column is updated to `"VISITED"`

#### Scenario: Updating status of a non-existent stop
- **WHEN** `updateStopStatus("nonexistent", "VISITED")` is called
- **THEN** no rows are updated and no exception is thrown

#### Scenario: Only affects the targeted stop
- **WHEN** `updateStopStatus("s1", "VISITED")` is called
- **THEN** other stops in the same trip retain their original status

### Requirement: StopRepository exposes updateStopStatus
`StopRepository` SHALL define `suspend fun updateStopStatus(stopId: String, status: StopStatus): Result<Unit>` that updates the persisted status of the specified stop.

#### Scenario: Successful status update
- **WHEN** `updateStopStatus` is called with a valid stop ID and status
- **THEN** the stop's status is persisted and `Result.success(Unit)` is returned

#### Scenario: DAO throws exception
- **WHEN** the DAO throws during the update
- **THEN** `Result.failure` with the exception is returned

### Requirement: StopRepositoryImpl implements updateStopStatus
`StopRepositoryImpl.updateStopStatus` SHALL:
1. Call `StopDao.updateStopStatus(stopId, status.name)`.
2. Wrap the call in `runCatching` so any exception yields `Result.failure`.
3. Return `Result.success(Unit)` on success.

#### Scenario: Delegates to DAO with enum name
- **WHEN** `updateStopStatus("s1", StopStatus.VISITED)` is called
- **THEN** `StopDao.updateStopStatus("s1", "VISITED")` is invoked

#### Scenario: Exception yields failure result
- **WHEN** `StopDao.updateStopStatus` throws
- **THEN** `Result.failure` with the thrown exception is returned
