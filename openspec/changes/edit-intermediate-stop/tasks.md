## 1. Prerequisites

- [x] 1.1 Add `getStopById(stopId: String): StopEntity?` query to `StopDao`
- [x] 1.2 Add `updateStop(stopId, placeName, latitude, longitude): Result<Stop>` to `StopRepository` interface
- [x] 1.3 Add `editingStop: Stop?` and `isEditStopDialogVisible: Boolean` fields to `TripDetailUiState`
- [x] 1.4 Add `OnEditStopClicked(stop)`, `OnEditStopConfirmed(stopId, placeName, latitude, longitude)`, `OnDismissEditStopDialog` intents to `TripDetailUiIntent`

## 2. StopRepositoryImpl updateStop (BDD)

- [x] 2.1 Write test: `GIVEN a stop exists WHEN updateStop is called THEN placeName latitude longitude are updated and id tripId order status are preserved` in `StopRepositoryImplTest`
- [x] 2.2 Write test: `GIVEN a stop does not exist WHEN updateStop is called THEN Result failure with IllegalStateException is returned` in `StopRepositoryImplTest`
- [x] 2.3 Implement `updateStop` in `StopRepositoryImpl`: fetch by ID via `getStopById`, copy with new values, call `StopDao.update()`, return domain model wrapped in `runCatching`

## 3. EditStopUseCase (BDD)

- [x] 3.1 Write test: `GIVEN valid parameters WHEN invoke is called THEN repository updateStop is called with trimmed placeName and Result success is returned` in `EditStopUseCaseTest`
- [x] 3.2 Write test: `GIVEN placeName with whitespace WHEN invoke is called THEN repository receives trimmed placeName` in `EditStopUseCaseTest`
- [x] 3.3 Write test: `GIVEN repository returns failure WHEN invoke is called THEN Result failure is propagated` in `EditStopUseCaseTest`
- [x] 3.4 Implement `EditStopUseCase` in `domain/usecase/EditStopUseCase.kt`: trim placeName, delegate to `stopRepository.updateStop()`, add TODO for leg invalidation (Epic 3)

## 4. TripDetailViewModel edit stop handling (BDD)

- [x] 4.1 Write test: `GIVEN initial state WHEN OnEditStopClicked is dispatched THEN editingStop is set and isEditStopDialogVisible is true` in `TripDetailViewModelTest`
- [x] 4.2 Write test: `GIVEN edit dialog visible WHEN OnDismissEditStopDialog is dispatched THEN editingStop is null and isEditStopDialogVisible is false` in `TripDetailViewModelTest`
- [x] 4.3 Write test: `GIVEN edit dialog visible WHEN OnEditStopConfirmed is dispatched and EditStopUseCase succeeds THEN editingStop is null and isEditStopDialogVisible is false` in `TripDetailViewModelTest`
- [x] 4.4 Write test: `GIVEN edit dialog visible WHEN OnEditStopConfirmed is dispatched and EditStopUseCase fails THEN ShowError effect is emitted` in `TripDetailViewModelTest`
- [x] 4.5 Implement edit stop intent handling in `TripDetailViewModel`: inject `EditStopUseCase`, handle `OnEditStopClicked`, `OnEditStopConfirmed`, `OnDismissEditStopDialog`

## 5. UI Integration

- [x] 5.1 Wire intermediate stop `onSetStopClicked` in `TripDetailScreen` to dispatch `OnEditStopClicked(stop)`
- [x] 5.2 Add edit stop `SetStopDialog` block in `TripDetailScreen`, guarded by `isEditStopDialogVisible`, pre-populated with `editingStop` values, using edit dialog title string resource
- [x] 5.3 Add `trip_detail_edit_stop_dialog_title` string resource ("Edit stop") to `strings.xml` and translate to other languages present

## 6. Verification

- [x] 6.1 Run `./gradlew clean` and `./gradlew test` — all unit tests pass
- [x] 6.2 Run `./gradlew assembleDebug` — build succeeds
- [ ] 6.3 Manual verification: tap intermediate stop → dialog pre-populated → edit → stop updated, order unchanged
