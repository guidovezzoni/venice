## 1. Prerequisites — DAO queries and MVI contract

- [x] 1.1 Add `deleteById` and `decrementOrderAbove` queries to `StopDao`
- [x] 1.2 Add `OnRemoveStopClicked(stop: Stop)`, `OnRemoveStopConfirmed`, `OnDismissRemoveStopDialog` to `TripDetailUiIntent`
- [x] 1.3 Add `isRemoveStopDialogVisible: Boolean` and `stopToRemove: Stop?` to `TripDetailUiState`

## 2. StopDao deleteAndReorder (BDD)

- [x] 2.1 Write test: GIVEN a stop at order 2 in a trip with stops [0,1,2,3] WHEN deleteAndReorder is called THEN the stop is deleted and remaining orders are contiguous — in `StopDaoTest` (note: DAO transactional logic is tested via repository integration)
- [x] 2.2 Implement `deleteAndReorder` @Transaction method in `StopDao`

## 3. StopRepository deleteStop (BDD)

- [x] 3.1 Write test: GIVEN a valid tripId and stopId WHEN deleteStop is called THEN StopDao.deleteAndReorder is called and Result.success(Unit) is returned — in `StopRepositoryImplTest`
- [x] 3.2 Write test: GIVEN StopDao.deleteAndReorder throws exception WHEN deleteStop is called THEN Result.failure with the exception is returned — in `StopRepositoryImplTest`
- [x] 3.3 Add `deleteStop(tripId: String, stopId: String): Result<Unit>` to `StopRepository` interface
- [x] 3.4 Implement `deleteStop` in `StopRepositoryImpl` using `runCatching`

## 4. RemoveStopUseCase (BDD)

- [x] 4.1 Write test: GIVEN repository returns success WHEN invoke is called THEN Result.success(Unit) is returned — in `RemoveStopUseCaseTest`
- [x] 4.2 Write test: GIVEN repository returns failure WHEN invoke is called THEN Result.failure is propagated — in `RemoveStopUseCaseTest`
- [x] 4.3 Create `RemoveStopUseCase` with TODO for leg invalidation (Epic 3)

## 5. TripDetailViewModel remove stop handling (BDD)

- [x] 5.1 Write test: GIVEN the screen is displayed WHEN OnRemoveStopClicked is dispatched THEN stopToRemove is set and isRemoveStopDialogVisible becomes true — in `TripDetailViewModelTest`
- [x] 5.2 Write test: GIVEN remove dialog is visible WHEN OnDismissRemoveStopDialog is dispatched THEN stopToRemove is null and isRemoveStopDialogVisible becomes false — in `TripDetailViewModelTest`
- [x] 5.3 Write test: GIVEN remove dialog is visible WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase succeeds THEN stopToRemove is null and isRemoveStopDialogVisible becomes false — in `TripDetailViewModelTest`
- [x] 5.4 Write test: GIVEN remove dialog is visible WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase fails THEN ShowError effect is emitted — in `TripDetailViewModelTest`
- [x] 5.5 Implement remove stop intent handling in `TripDetailViewModel`

## 6. UI — StopSection delete button

- [x] 6.1 Add `onDelete: (() -> Unit)?` parameter to `StopSection` and render delete `IconButton` with `Icons.Outlined.Delete` when non-null and stop is filled
- [x] 6.2 Update `StopSection` previews to cover `onDelete` parameter

## 7. UI — TripDetailScreen confirmation dialog and wiring

- [x] 7.1 Pass `onDelete` lambda to all `StopSection` instances (starting point, intermediates, destination) dispatching `OnRemoveStopClicked(stop)`
- [x] 7.2 Add confirmation `AlertDialog` when `isRemoveStopDialogVisible` is true, displaying stop name, with confirm dispatching `OnRemoveStopConfirmed` and dismiss dispatching `OnDismissRemoveStopDialog`
- [x] 7.3 Update `TripDetailScreen` previews to cover remove dialog state

## 8. String resources

- [x] 8.1 Add remove-related strings to `values/strings.xml` (EN)
- [x] 8.2 Add remove-related strings to `values-it/strings.xml` (IT)
- [x] 8.3 Add remove-related strings to `values-es-rES/strings.xml` (ES)

## 9. Verification

- [x] 9.1 Run `./gradlew check` and confirm all tests pass
- [x] 9.2 Manual verification: remove intermediate stop, remove starting point, remove destination, cancel dialog, remove last stop from trip
