## 1. Prerequisites

- [x] 1.1 Add `updateStopStatus(stopId: String, status: String)` query to `StopDao`
- [x] 1.2 Add `updateStopStatus(stopId: String, status: StopStatus): Result<Unit>` to `StopRepository` interface
- [x] 1.3 Add `OnMarkStopDepartedClicked(stopId: String)` and `OnUndoMarkStopDepartedClicked(stopId: String)` to `TripDetailUiIntent`

## 2. StopRepositoryImpl.updateStopStatus (BDD)

- [x] 2.1 Write test: GIVEN a valid stop ID WHEN updateStopStatus is called with VISITED THEN StopDao.updateStopStatus is called with the stop ID and "VISITED" and Result.success(Unit) is returned in `StopRepositoryImplTest`
- [x] 2.2 Write test: GIVEN StopDao throws WHEN updateStopStatus is called THEN Result.failure with the exception is returned in `StopRepositoryImplTest`
- [x] 2.3 Implement `updateStopStatus` in `StopRepositoryImpl`

## 3. MarkStopDepartedUseCase (BDD)

- [x] 3.1 Write test: GIVEN the target stop is the current stop (first PENDING by order) WHEN invoke is called THEN updateStopStatus is called with VISITED and Result.success is returned in `MarkStopDepartedUseCaseTest`
- [x] 3.2 Write test: GIVEN the target stop is NOT the current stop WHEN invoke is called THEN Result.failure is returned with "Only the current stop can be marked as departed" in `MarkStopDepartedUseCaseTest`
- [x] 3.3 Write test: GIVEN no PENDING stops exist WHEN invoke is called THEN Result.failure is returned in `MarkStopDepartedUseCaseTest`
- [x] 3.4 Write test: GIVEN repository updateStopStatus fails WHEN invoke is called THEN Result.failure is propagated in `MarkStopDepartedUseCaseTest`
- [x] 3.5 Implement `MarkStopDepartedUseCase`

## 4. UndoMarkStopDepartedUseCase (BDD)

- [x] 4.1 Write test: GIVEN at least one VISITED stop exists WHEN invoke is called THEN updateStopStatus is called with the highest-order VISITED stop's ID and PENDING and Result.success is returned in `UndoMarkStopDepartedUseCaseTest`
- [x] 4.2 Write test: GIVEN no VISITED stops exist WHEN invoke is called THEN Result.failure is returned with "No departed stop to undo" in `UndoMarkStopDepartedUseCaseTest`
- [x] 4.3 Write test: GIVEN multiple VISITED stops WHEN invoke is called THEN only the highest-order one is reverted in `UndoMarkStopDepartedUseCaseTest`
- [x] 4.4 Write test: GIVEN repository updateStopStatus fails WHEN invoke is called THEN Result.failure is propagated in `UndoMarkStopDepartedUseCaseTest`
- [x] 4.5 Implement `UndoMarkStopDepartedUseCase`

## 5. TripDetailViewModel — mark/undo intents (BDD)

- [x] 5.1 Write test: GIVEN OnMarkStopDepartedClicked is dispatched WHEN MarkStopDepartedUseCase succeeds THEN no error effect is emitted in `TripDetailViewModelTest`
- [x] 5.2 Write test: GIVEN OnMarkStopDepartedClicked is dispatched WHEN MarkStopDepartedUseCase fails THEN ShowError effect is emitted in `TripDetailViewModelTest`
- [x] 5.3 Write test: GIVEN OnUndoMarkStopDepartedClicked is dispatched WHEN UndoMarkStopDepartedUseCase succeeds THEN no error effect is emitted in `TripDetailViewModelTest`
- [x] 5.4 Write test: GIVEN OnUndoMarkStopDepartedClicked is dispatched WHEN UndoMarkStopDepartedUseCase fails THEN ShowError effect is emitted in `TripDetailViewModelTest`
- [x] 5.5 Implement mark/undo intent handling in `TripDetailViewModel`

## 6. UI — Visual differentiation and action buttons

- [x] 6.1 Create `StopDisplayState` enum with values `DEPARTED`, `CURRENT`, `UPCOMING` in the UI layer
- [x] 6.2 Add `stopDisplayState` parameter and status-based styling to `StopSection` (muted alpha for DEPARTED, primary for CURRENT, default for UPCOMING)
- [x] 6.3 Add `onMarkDeparted` lambda and "Mark as departed" button to `StopSection`
- [x] 6.4 Add `onUndoDeparted` lambda and "Undo departure" button to `StopSection`
- [x] 6.5 Update `TripDetailScreen` to derive display state and pass mark/undo lambdas based on stop status
- [x] 6.6 Add Compose previews covering departed, current, and upcoming stop states

## 7. String resources

- [x] 7.1 Add `trip_detail_mark_departed` and `trip_detail_undo_departed` to `res/values/strings.xml` (EN)
- [x] 7.2 Add Italian translations to `res/values-it/strings.xml`

## 8. Verification

- [x] 8.1 Run `./gradlew check` and verify all tests pass
- [x] 8.2 On-device verification: confirm departed/current/upcoming styling, mark and undo flow
