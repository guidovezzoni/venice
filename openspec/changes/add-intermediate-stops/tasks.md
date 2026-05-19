## 1. Prerequisites — Data Layer

- [ ] 1.1 Add `incrementOrderFrom(tripId, fromOrder)` query to `StopDao`
- [ ] 1.2 Add `getStopCount(tripId)` query to `StopDao`

## 2. Prerequisites — Domain Layer

- [ ] 2.1 Add `addIntermediateStop(...)` and `getStopCount(...)` to `StopRepository` interface

## 3. StopRepositoryImpl — addIntermediateStop (BDD)

- [ ] 3.1 Write test: GIVEN existing stops WHEN `addIntermediateStop` is called THEN orders are shifted and new stop is inserted at the correct position in `StopRepositoryImplTest`
- [ ] 3.2 Write test: GIVEN no destination exists WHEN `addIntermediateStop` is called THEN new stop is inserted at order 1 in `StopRepositoryImplTest`
- [ ] 3.3 Write test: GIVEN DAO throws exception WHEN `addIntermediateStop` is called THEN Result.failure is returned in `StopRepositoryImplTest`
- [ ] 3.4 Write test: GIVEN a trip WHEN `getStopCount` is called THEN correct count is returned in `StopRepositoryImplTest`
- [ ] 3.5 Implement `addIntermediateStop` and `getStopCount` in `StopRepositoryImpl`

## 4. AddIntermediateStopUseCase (BDD)

- [ ] 4.1 Write test: GIVEN valid inputs WHEN `invoke` is called THEN repository method is called with correct arguments in `AddIntermediateStopUseCaseTest`
- [ ] 4.2 Write test: GIVEN blank place name WHEN `invoke` is called THEN returns failure in `AddIntermediateStopUseCaseTest`
- [ ] 4.3 Write test: GIVEN coordinates out of range WHEN `invoke` is called THEN returns failure in `AddIntermediateStopUseCaseTest`
- [ ] 4.4 Write test: GIVEN stop count already at 25 WHEN `invoke` is called THEN returns failure in `AddIntermediateStopUseCaseTest`
- [ ] 4.5 Write test: GIVEN place name with whitespace WHEN `invoke` is called THEN repository receives trimmed name in `AddIntermediateStopUseCaseTest`
- [ ] 4.6 Write test: GIVEN repository returns failure WHEN `invoke` is called THEN failure is propagated in `AddIntermediateStopUseCaseTest`
- [ ] 4.7 Implement `AddIntermediateStopUseCase`

## 5. TripDetailViewModel — Intermediate Stops (BDD)

- [ ] 5.1 Write test: GIVEN stops exist WHEN observed THEN `intermediateStops` contains only stops with order between start and destination in `TripDetailViewModelTest`
- [ ] 5.2 Write test: GIVEN `OnAddStopClicked` intent WHEN dispatched THEN `isAddStopDialogVisible` becomes true in `TripDetailViewModelTest`
- [ ] 5.3 Write test: GIVEN `OnDismissAddStopDialog` intent WHEN dispatched THEN `isAddStopDialogVisible` becomes false in `TripDetailViewModelTest`
- [ ] 5.4 Write test: GIVEN `OnAddStopConfirmed` intent WHEN dispatched and use case succeeds THEN dialog is dismissed in `TripDetailViewModelTest`
- [ ] 5.5 Write test: GIVEN `OnAddStopConfirmed` intent WHEN dispatched and use case fails THEN `ShowError` effect is emitted in `TripDetailViewModelTest`
- [ ] 5.6 Write test: GIVEN 25 stops exist WHEN observed THEN `canAddMoreStops` is false in `TripDetailViewModelTest`
- [ ] 5.7 Extend `TripDetailUiState` with `intermediateStops`, `isAddStopDialogVisible`, `canAddMoreStops`
- [ ] 5.8 Extend `TripDetailUiIntent` with `OnAddStopClicked`, `OnAddStopConfirmed`, `OnDismissAddStopDialog`
- [ ] 5.9 Implement intermediate stop handling in `TripDetailViewModel`

## 6. Integration — DI and Strings

- [ ] 6.1 Register `AddIntermediateStopUseCase` in DI module (Hilt or manual)
- [ ] 6.2 Add new strings to `values/strings.xml`: `trip_detail_add_stop`, `error_max_stops_reached`
- [ ] 6.3 Add translations to other `values-*/strings.xml` language variants

## 7. UI — TripDetailScreen

- [ ] 7.1 Render intermediate stops between starting point and destination using `StopSection` with `LocationOn` icon
- [ ] 7.2 Add "Add Stop" button between last intermediate stop and destination, visible only when `canAddMoreStops` is true
- [ ] 7.3 Show `SetStopDialog` for adding an intermediate stop when `isAddStopDialogVisible` is true
- [ ] 7.4 Ensure "Add Stop" button has `contentDescription` for accessibility
- [ ] 7.5 Update composable previews for new states (with intermediate stops, at stop limit)

## 8. Verification

- [ ] 8.1 Run `./gradlew clean`
- [ ] 8.2 Run `./gradlew test` — all unit tests pass
- [ ] 8.3 Run `./gradlew check` — code quality passes
- [ ] 8.4 Run `./gradlew assembleDebug` — build succeeds
