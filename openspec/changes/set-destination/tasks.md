## 1. Prerequisites

- [ ] 1.1 Add `getDestination` query to `StopDao`
- [ ] 1.2 Add `upsertDestination` to `StopRepository` interface
- [ ] 1.3 Add destination string resources to `strings.xml`

## 2. StopRepositoryImpl — upsertDestination (BDD)

- [ ] 2.1 Write test: `GIVEN no stop with order greater than 0 for the trip WHEN upsertDestination is called THEN a new stop is inserted with order=1 and status=PENDING` in `StopRepositoryImplTest`
- [ ] 2.2 Write test: `GIVEN a destination stop already exists WHEN upsertDestination is called THEN the existing stop is updated with the new name and coordinates` in `StopRepositoryImplTest`
- [ ] 2.3 Write test: `GIVEN DAO throws an exception WHEN upsertDestination is called THEN Result failure is returned` in `StopRepositoryImplTest`
- [ ] 2.4 Implement `upsertDestination` in `StopRepositoryImpl`

## 3. SetDestinationUseCase (BDD)

- [ ] 3.1 Write test: `GIVEN repository returns success WHEN invoke is called THEN Result success with the Stop is returned` in `SetDestinationUseCaseTest`
- [ ] 3.2 Write test: `GIVEN repository returns failure WHEN invoke is called THEN Result failure is returned` in `SetDestinationUseCaseTest`
- [ ] 3.3 Write test: `GIVEN a place name with leading and trailing spaces WHEN invoke is called THEN repository receives the trimmed name` in `SetDestinationUseCaseTest`
- [ ] 3.4 Implement `SetDestinationUseCase`

## 4. MVI Contract Updates

- [ ] 4.1 Add `destination: Stop?` and `isSetDestinationDialogVisible: Boolean` to `TripDetailUiState`
- [ ] 4.2 Add `OnSetDestinationClicked`, `OnDestinationConfirmed`, `OnDismissDestinationDialog` to `TripDetailUiIntent`

## 5. TripDetailViewModel — Destination Handling (BDD)

- [ ] 5.1 Write test: `GIVEN initial state WHEN OnSetDestinationClicked is dispatched THEN isSetDestinationDialogVisible becomes true` in `TripDetailViewModelTest`
- [ ] 5.2 Write test: `GIVEN dialog is visible WHEN OnDismissDestinationDialog is dispatched THEN isSetDestinationDialogVisible becomes false` in `TripDetailViewModelTest`
- [ ] 5.3 Write test: `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case succeeds THEN destination is updated and dialog is dismissed` in `TripDetailViewModelTest`
- [ ] 5.4 Write test: `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case fails THEN ShowError effect is emitted` in `TripDetailViewModelTest`
- [ ] 5.5 Write test: `GIVEN a stop with order=1 in the stream WHEN ViewModel initialises THEN destination reflects that stop` in `TripDetailViewModelTest`
- [ ] 5.6 Write test: `GIVEN only a stop with order=0 in the stream WHEN ViewModel initialises THEN destination is null` in `TripDetailViewModelTest`
- [ ] 5.7 Implement destination intent handling and observation in `TripDetailViewModel`

## 6. UI Composables

- [ ] 6.1 Create `DestinationSection` composable with empty and filled states and previews
- [ ] 6.2 Create `SetDestinationDialog` composable with validation and previews

## 7. Screen Integration

- [ ] 7.1 Update `TripDetailScreen` to render `DestinationSection` below starting point
- [ ] 7.2 Update `TripDetailScreen` to show `SetDestinationDialog` when visible
- [ ] 7.3 Update `MainScreen` to wire destination error string for snackbar

## 8. Verification

- [ ] 8.1 Run `./gradlew test` and confirm all tests pass
- [ ] 8.2 Run `./gradlew check` and confirm no lint or compile errors
