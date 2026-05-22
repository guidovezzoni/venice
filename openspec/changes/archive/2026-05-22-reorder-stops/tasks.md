## 1. Prerequisites — DAO queries

- [x] 1.1 Add `getStopByTripIdAndOrder(tripId: String, order: Int): StopEntity?` query to `StopDao`
- [x] 1.2 Add `updateStopOrder(stopId: String, newOrder: Int)` query to `StopDao`

## 2. StopRepository swap (BDD)

- [x] 2.1 Write test: `GIVEN two stops at adjacent orders WHEN swapStopOrder is called THEN both orders are updated` in `StopRepositoryImplTest`
- [x] 2.2 Write test: `GIVEN a non-existent source order WHEN swapStopOrder is called THEN result is failure` in `StopRepositoryImplTest`
- [x] 2.3 Write test: `GIVEN a non-existent target order WHEN swapStopOrder is called THEN result is failure` in `StopRepositoryImplTest`
- [x] 2.4 Add `suspend fun swapStopOrder(tripId: String, fromOrder: Int, toOrder: Int): Result<Unit>` to `StopRepository` interface
- [x] 2.5 Implement `swapStopOrder` in `StopRepositoryImpl` using a `@Transaction` to make all tests pass

## 3. MoveStopUseCase (BDD)

- [x] 3.1 Write test: `GIVEN two adjacent intermediate stops WHEN move up is called on the second THEN swapStopOrder is called with correct orders` in `MoveStopUseCaseTest`
- [x] 3.2 Write test: `GIVEN two adjacent intermediate stops WHEN move down is called on the first THEN swapStopOrder is called with correct orders` in `MoveStopUseCaseTest`
- [x] 3.3 Write test: `GIVEN repository returns failure WHEN move is called THEN failure is propagated` in `MoveStopUseCaseTest`
- [x] 3.4 Implement `MoveStopUseCase` in `app/src/main/java/com/guidovezzoni/venice/domain/usecase/MoveStopUseCase.kt` to make all tests pass

## 4. TripDetailViewModel reorder intents (BDD)

- [x] 4.1 Add `OnMoveStopUp(stopId: String, currentOrder: Int)` and `OnMoveStopDown(stopId: String, currentOrder: Int)` to `TripDetailUiIntent`
- [x] 4.2 Write test: `GIVEN intermediate stops WHEN OnMoveStopUp intent is received THEN MoveStopUseCase is called with correct orders` in `TripDetailViewModelTest`
- [x] 4.3 Write test: `GIVEN intermediate stops WHEN OnMoveStopDown intent is received THEN MoveStopUseCase is called with correct orders` in `TripDetailViewModelTest`
- [x] 4.4 Write test: `GIVEN a move fails WHEN intent is received THEN ShowError effect is emitted` in `TripDetailViewModelTest`
- [x] 4.5 Implement `OnMoveStopUp` and `OnMoveStopDown` handling in `TripDetailViewModel` to make all tests pass

## 5. UI — StopSection reorder buttons

- [x] 5.1 Add `onMoveUp: (() -> Unit)? = null` and `onMoveDown: (() -> Unit)? = null` parameters to `StopSection`
- [x] 5.2 Render `IconButton` with `Icons.Filled.KeyboardArrowUp` when `onMoveUp` is non-null, and `Icons.Filled.KeyboardArrowDown` when `onMoveDown` is non-null
- [x] 5.3 Update `StopSection` previews to cover states with and without reorder buttons

## 6. UI — TripDetailScreen wiring

- [x] 6.1 Pass `onMoveUp` / `onMoveDown` lambdas from `TripDetailScreen` to `StopSection` for each intermediate stop, applying visibility rules (hide up for first, hide down for last, hide all when fewer than 2 intermediates)
- [x] 6.2 Update `TripDetailScreen` previews if needed

## 7. String resources

- [x] 7.1 Add `trip_detail_move_stop_up` ("Move up") and `trip_detail_move_stop_down` ("Move down") to `res/values/strings.xml`
- [x] 7.2 Add Italian translations ("Sposta su", "Sposta giu") to `res/values-it/strings.xml`
- [x] 7.3 Add Spanish translations ("Mover arriba", "Mover abajo") to `res/values-es-rES/strings.xml`

## 8. Verification

- [x] 8.1 Run `./gradlew check` and confirm all tests pass with no lint errors
- [x] 8.2 Manual verification: build debug APK, launch on emulator, add 3+ intermediate stops, confirm move-up/move-down buttons work correctly
