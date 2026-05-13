## 1. Prerequisites — Domain Models & Data Classes

- [ ] 1.1 Create `StopStatus` enum (`PENDING`, `VISITED`) in `domain/model/StopStatus.kt`
- [ ] 1.2 Create `Stop` data class in `domain/model/Stop.kt`
- [ ] 1.3 Create `StopEntity` Room entity in `data/database/entity/StopEntity.kt`
- [ ] 1.4 Create `StopMapper` (`StopEntity.toDomain()`) in `data/database/mapper/StopMapper.kt`
- [ ] 1.5 Create `StopDao` interface in `data/database/dao/StopDao.kt`
- [ ] 1.6 Create `StopRepository` interface in `domain/repository/StopRepository.kt`

## 2. Database Migration (BDD)

- [ ] 2.1 Write test: `GIVEN database at version 1 WHEN MIGRATION_1_2 runs THEN stops table and index are created` in `MigrationTest`
- [ ] 2.2 Implement `MIGRATION_1_2` in `AppDatabase.kt`, bump version to 2, add `StopEntity`, expose `stopDao()`, wire migration into `databaseBuilder`

## 3. StopRepositoryImpl — Upsert (BDD)

- [ ] 3.1 Write test: `GIVEN no stop with order=0 for the trip WHEN upsertStartingPoint is called THEN a new stop is inserted and returned with order=0 and status=PENDING` in `StopRepositoryImplTest`
- [ ] 3.2 Write test: `GIVEN a stop with order=0 already exists WHEN upsertStartingPoint is called THEN the existing stop is updated with the new name and coordinates` in `StopRepositoryImplTest`
- [ ] 3.3 Write test: `GIVEN DAO throws an exception WHEN upsertStartingPoint is called THEN Result.failure is returned` in `StopRepositoryImplTest`
- [ ] 3.4 Implement `StopRepositoryImpl` with `upsertStartingPoint` and `observeStopsForTrip` in `data/repository/StopRepositoryImpl.kt`

## 4. SetStartingPointUseCase (BDD)

- [ ] 4.1 Write test: `GIVEN repository returns success WHEN invoke is called THEN Result.success with the Stop is returned` in `SetStartingPointUseCaseTest`
- [ ] 4.2 Write test: `GIVEN repository returns failure WHEN invoke is called THEN Result.failure is returned` in `SetStartingPointUseCaseTest`
- [ ] 4.3 Write test: `GIVEN a place name with leading and trailing spaces WHEN invoke is called THEN repository receives the trimmed name` in `SetStartingPointUseCaseTest`
- [ ] 4.4 Implement `SetStartingPointUseCase` in `domain/usecase/SetStartingPointUseCase.kt`
- [ ] 4.5 Implement `ObserveStopsUseCase` in `domain/usecase/ObserveStopsUseCase.kt`

## 5. TripDetailViewModel (BDD)

- [ ] 5.1 Create `TripDetailUiState` in `ui/state/TripDetailUiState.kt`
- [ ] 5.2 Create `TripDetailUiIntent` in `ui/intent/TripDetailUiIntent.kt`
- [ ] 5.3 Create `TripDetailUiEffect` in `ui/effect/TripDetailUiEffect.kt`
- [ ] 5.4 Write test: `GIVEN initial state WHEN OnSetStartingPointClicked is dispatched THEN isSetStartingPointDialogVisible becomes true` in `TripDetailViewModelTest`
- [ ] 5.5 Write test: `GIVEN dialog is visible WHEN OnDismissStartingPointDialog is dispatched THEN isSetStartingPointDialogVisible becomes false` in `TripDetailViewModelTest`
- [ ] 5.6 Write test: `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case succeeds THEN startingPoint is updated and dialog is dismissed` in `TripDetailViewModelTest`
- [ ] 5.7 Write test: `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case fails THEN ShowError effect is emitted` in `TripDetailViewModelTest`
- [ ] 5.8 Write test: `GIVEN a stop with order=0 in the stream WHEN ViewModel initialises THEN startingPoint reflects that stop` in `TripDetailViewModelTest`
- [ ] 5.9 Write test: `GIVEN no stops in the stream WHEN ViewModel initialises THEN startingPoint is null` in `TripDetailViewModelTest`
- [ ] 5.10 Implement `TripDetailViewModel` in `ui/viewmodel/TripDetailViewModel.kt`

## 6. String Resources

- [ ] 6.1 Add all new string resources to `res/values/strings.xml`

## 7. UI Composables

- [ ] 7.1 Create `StartingPointSection` composable with `@Preview` (empty-state and filled-state) in `ui/screens/tripdetail/StartingPointSection.kt`
- [ ] 7.2 Create `SetStartingPointDialog` composable with `@Preview` in `ui/screens/tripdetail/SetStartingPointDialog.kt`
- [ ] 7.3 Update `TripDetailScreen` to accept `uiState`/`onIntent`, render `StartingPointSection`, show dialog, consume `uiEffect` for snackbar

## 8. DI & Navigation Wiring

- [ ] 8.1 Add `@Provides fun provideStopDao` to `DatabaseModule`
- [ ] 8.2 Add `@Binds` for `StopRepositoryImpl → StopRepository` to `RepositoryModule`
- [ ] 8.3 Wire `TripDetailViewModel` via `hiltViewModel()` in `MainScreen` `ROUTE_TRIP_DETAIL` composable

## 9. Verification

- [ ] 9.1 Run `./gradlew test` — all unit tests pass
- [ ] 9.2 Run `./gradlew check` — no lint or compile errors
