# Trip Creation

## Purpose

Defines the requirements for creating a new road trip, covering the domain layer (use case, repository interface), the data layer (repository implementation), the UI dialog, and supporting concerns such as validation, error feedback, and state survival across configuration changes.

## Requirements

### Requirement: TripRepository interface in domain layer
The domain layer SHALL define a `TripRepository` interface with:
- `suspend fun createTrip(name: String): Result<Trip>`
- `fun observeTrips(): Flow<List<Trip>>`
- `fun observeTripById(tripId: String): Flow<Trip?>` — observes a single trip by ID, emitting `null` if the trip does not exist

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case or ViewModel references `TripRepository`
- **THEN** it depends only on the domain layer, not the data layer

#### Scenario: Observing a single trip by ID
- **WHEN** `observeTripById(tripId)` is called for an existing trip
- **THEN** a `Flow<Trip?>` is returned that emits the current `Trip` (mapped from `TripWithStopCount`) whenever the underlying data changes

### Requirement: TripRepositoryImpl persists trips
`TripRepositoryImpl` SHALL implement `TripRepository`. The `createTrip` method SHALL generate a UUID for the ID, trim the name, set `createdAt` and `updatedAt` to the current epoch milliseconds, insert via `TripDao`, and return the created `Trip` wrapped in `Result.success`. The `observeTripById` method SHALL delegate to `TripDao.observeById(tripId)` and map each non-null emission to a domain `Trip` via the existing `TripWithStopCount.toDomain()` mapper, passing through `null` unchanged.

#### Scenario: Successful trip creation
- **WHEN** `createTrip("  Summer Drive  ")` is called
- **THEN** a `TripEntity` with `name = "Summer Drive"` and a UUID `id` is inserted and a `Result.success(Trip)` is returned

#### Scenario: DAO insert failure
- **WHEN** the DAO throws an exception during insert
- **THEN** `createTrip` returns `Result.failure` with the exception

#### Scenario: Observing a single trip maps DAO emissions to domain
- **WHEN** `TripDao.observeById(tripId)` emits a `TripWithStopCount` for `tripId`
- **THEN** `observeTripById(tripId)` emits the equivalent domain `Trip`, with `name`, `createdAt`, `updatedAt`, and `stopCount` preserved

#### Scenario: Observing a missing trip emits null
- **WHEN** `TripDao.observeById(tripId)` emits `null` for a trip ID that does not exist
- **THEN** `observeTripById(tripId)` emits `null`

### Requirement: ObserveTripUseCase wraps repository observation
`ObserveTripUseCase` SHALL expose `operator fun invoke(tripId: String): Flow<Trip?>` that delegates to `TripRepository.observeTripById`, mirroring the existing `ObserveStopsUseCase` / `ObserveLegsUseCase` shape (a single-argument, `Flow`-returning pass-through with no additional logic).

#### Scenario: Trip flow is returned
- **WHEN** `invoke` is called with a trip ID
- **THEN** the `Flow<Trip?>` from `TripRepository.observeTripById` is returned unchanged

### Requirement: CreateTripUseCase validates and delegates
`CreateTripUseCase` SHALL validate that the trip name is not blank before delegating to `TripRepository.createTrip`.

#### Scenario: Blank name rejected
- **WHEN** `CreateTripUseCase` is invoked with a blank or whitespace-only name
- **THEN** it returns `Result.failure(IllegalArgumentException)` without calling the repository

#### Scenario: Valid name delegates to repository
- **WHEN** `CreateTripUseCase` is invoked with `"Coast Trip"`
- **THEN** it calls `repository.createTrip("Coast Trip")` and returns the result

### Requirement: Trip name validation rules
The trip name SHALL be validated as follows:
- MUST NOT be empty or whitespace-only.
- MUST be trimmed before saving.
- MUST NOT exceed 100 characters (enforced at the UI layer via `maxLength` on the text field).

#### Scenario: Whitespace-only name is rejected
- **WHEN** the user enters `"   "` as the trip name
- **THEN** the Confirm button remains disabled and the trip is not created

#### Scenario: Name exceeding 100 characters is prevented
- **WHEN** the user types more than 100 characters in the trip name field
- **THEN** the text field does not accept characters beyond the 100th

### Requirement: Create trip dialog UI
The UI SHALL present a modal dialog (bottom sheet or `AlertDialog`) with:
- A labelled text input field ("Trip name").
- A Confirm button (disabled while the name is blank or whitespace-only, or while `isLoading` is `true`).
- A Cancel button or dismiss-by-tapping-outside behaviour.

The dialog composable SHALL be stateless, receiving `tripName`, `onNameChange`, `onConfirm`, `onDismiss`, and `isLoading` as parameters.

When `isLoading` is `true`:
- The confirm button SHALL be disabled regardless of whether the trip name is valid
- A `CircularProgressIndicator` SHALL be displayed inline in the confirm button slot
- The dismiss button SHALL remain enabled so the user can still close the dialog

#### Scenario: Dialog opens with empty input
- **WHEN** the user taps the FAB to create a trip
- **THEN** the dialog appears with an empty text field and the Confirm button disabled

#### Scenario: Confirm enabled after valid input
- **WHEN** the user types a non-blank trip name and `isLoading` is `false`
- **THEN** the Confirm button becomes enabled

#### Scenario: Dialog dismissed via Cancel
- **WHEN** the user taps Cancel or taps outside the dialog
- **THEN** the dialog closes without creating a trip

#### Scenario: Confirm button disabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the Confirm button is disabled regardless of the trip name value

#### Scenario: Spinner visible while loading
- **WHEN** `isLoading` is `true`
- **THEN** a `CircularProgressIndicator` is displayed in the confirm button slot

#### Scenario: Dismiss button enabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the Cancel/dismiss button remains enabled

### Requirement: Error feedback on creation failure
If the Room insert fails, the system SHALL display an error Snackbar on the Trip List screen with the message "Could not create trip. Please try again." The dialog SHALL remain open so the user can retry.

#### Scenario: Insert failure shows error
- **WHEN** the trip creation fails due to a database error
- **THEN** a Snackbar with the error message is displayed and the dialog remains open

### Requirement: Dialog state survives configuration changes
The dialog visibility and text input state SHALL survive device rotation via `rememberSaveable` or ViewModel-held state.

#### Scenario: Rotation preserves dialog state
- **WHEN** the user has typed a trip name in the open dialog and rotates the device
- **THEN** the dialog remains open with the typed text preserved
