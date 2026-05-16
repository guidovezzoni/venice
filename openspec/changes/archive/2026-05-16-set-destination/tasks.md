## 1. Prerequisites

- [x] 1.1 Add `getDestination` query to `StopDao`
- [x] 1.2 Add `upsertDestination` to `StopRepository` interface
- [x] 1.3 Add destination string resources to `strings.xml`

## 2. StopRepositoryImpl — upsertDestination (BDD)

- [x] 2.1 Write test: `GIVEN no stop with order greater than 0 for the trip WHEN upsertDestination is called THEN a new stop is inserted with order=1 and status=PENDING` in `StopRepositoryImplTest`
- [x] 2.2 Write test: `GIVEN a destination stop already exists WHEN upsertDestination is called THEN the existing stop is updated with the new name and coordinates` in `StopRepositoryImplTest`
- [x] 2.3 Write test: `GIVEN DAO throws an exception WHEN upsertDestination is called THEN Result failure is returned` in `StopRepositoryImplTest`
- [x] 2.4 Implement `upsertDestination` in `StopRepositoryImpl`

## 3. SetDestinationUseCase (BDD)

- [x] 3.1 Write test: `GIVEN repository returns success WHEN invoke is called THEN Result success with the Stop is returned` in `SetDestinationUseCaseTest`
- [x] 3.2 Write test: `GIVEN repository returns failure WHEN invoke is called THEN Result failure is returned` in `SetDestinationUseCaseTest`
- [x] 3.3 Write test: `GIVEN a place name with leading and trailing spaces WHEN invoke is called THEN repository receives the trimmed name` in `SetDestinationUseCaseTest`
- [x] 3.4 Implement `SetDestinationUseCase`

## 4. MVI Contract Updates

- [x] 4.1 Add `destination: Stop?` and `isSetDestinationDialogVisible: Boolean` to `TripDetailUiState`
- [x] 4.2 Add `OnSetDestinationClicked`, `OnDestinationConfirmed`, `OnDismissDestinationDialog` to `TripDetailUiIntent`

## 5. TripDetailViewModel — Destination Handling (BDD)

- [x] 5.1 Write test: `GIVEN initial state WHEN OnSetDestinationClicked is dispatched THEN isSetDestinationDialogVisible becomes true` in `TripDetailViewModelTest`
- [x] 5.2 Write test: `GIVEN dialog is visible WHEN OnDismissDestinationDialog is dispatched THEN isSetDestinationDialogVisible becomes false` in `TripDetailViewModelTest`
- [x] 5.3 Write test: `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case succeeds THEN destination is updated and dialog is dismissed` in `TripDetailViewModelTest`
- [x] 5.4 Write test: `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case fails THEN ShowError effect is emitted` in `TripDetailViewModelTest`
- [x] 5.5 Write test: `GIVEN a stop with order=1 in the stream WHEN ViewModel initialises THEN destination reflects that stop` in `TripDetailViewModelTest`
- [x] 5.6 Write test: `GIVEN only a stop with order=0 in the stream WHEN ViewModel initialises THEN destination is null` in `TripDetailViewModelTest`
- [x] 5.7 Implement destination intent handling and observation in `TripDetailViewModel`

## 6. UI Composables

- [x] 6.1 Create `DestinationSection` composable with empty and filled states and previews
- [x] 6.2 Create `SetDestinationDialog` composable with validation and previews

## 7. Screen Integration

- [x] 7.1 Update `TripDetailScreen` to render `DestinationSection` below starting point
- [x] 7.2 Update `TripDetailScreen` to show `SetDestinationDialog` when visible
- [x] 7.3 Update `MainScreen` to wire destination error string for snackbar

## 8. Verification (pre-consolidation)

- [x] 8.1 Run `./gradlew test` and confirm all tests pass
- [x] 8.2 Run `./gradlew check` and confirm no lint or compile errors

## 9. Refactoring — Consolidation

- [x] 9.1 Create `StopSection` composable with parameterised icon and string resources
- [x] 9.2 Create `SetStopDialog` composable with parameterised string resources
- [x] 9.3 Update `TripDetailScreen` to use `StopSection` and `SetStopDialog`
- [x] 9.4 Delete `StartingPointSection.kt`, `DestinationSection.kt`, `SetStartingPointDialog.kt`, `SetDestinationDialog.kt`
- [x] 9.5 Extract private `upsertStop` helper in `StopRepositoryImpl`
- [x] 9.6 Add TODO comments in `SetStartingPointUseCase` and `SetDestinationUseCase`
- [x] 9.7 Run `./gradlew test` and confirm all tests pass
- [x] 9.8 Run `./gradlew check` and confirm no lint or compile errors

## 10. Pre-populate dialog with existing values

- [x] 10.1 Add `initialPlaceName`, `initialLatitude`, `initialLongitude` parameters to `SetStopDialog`
- [x] 10.2 Update `TripDetailScreen` to pass existing stop data to `SetStopDialog` when the stop is non-null
- [x] 10.3 Add preview for pre-populated dialog state
- [x] 10.4 Run `./gradlew test` and confirm all tests pass
- [x] 10.5 Run `./gradlew check` and confirm no lint or compile errors
