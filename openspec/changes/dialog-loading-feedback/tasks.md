## 1. ViewModel — isLoading Wrapping (BDD)

- [ ] 1.1 Write test: GIVEN editStop operation WHEN OnEditStopConfirmed is dispatched and EditStopUseCase succeeds THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.2 Write test: GIVEN editStop operation WHEN OnEditStopConfirmed is dispatched and EditStopUseCase fails THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.3 Implement: Wrap editStop() with isLoading = true before async call and isLoading = false on success/failure in TripDetailViewModel
- [ ] 1.4 Write test: GIVEN removeStop operation WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase succeeds THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.5 Write test: GIVEN removeStop operation WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase fails THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.6 Implement: Wrap removeStop() with isLoading = true/false in TripDetailViewModel
- [ ] 1.7 Write test: GIVEN moveStop operation WHEN OnMoveStopUp is dispatched and MoveStopUseCase succeeds THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.8 Write test: GIVEN moveStop operation WHEN OnMoveStopUp is dispatched and MoveStopUseCase fails THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.9 Implement: Wrap moveStop() with isLoading = true/false in TripDetailViewModel
- [ ] 1.10 Write test: GIVEN markStopDeparted operation WHEN OnMarkStopDepartedClicked is dispatched and MarkStopDepartedUseCase succeeds THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.11 Write test: GIVEN markStopDeparted operation WHEN OnMarkStopDepartedClicked is dispatched and MarkStopDepartedUseCase fails THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.12 Implement: Wrap markStopDeparted() with isLoading = true/false in TripDetailViewModel
- [ ] 1.13 Write test: GIVEN undoMarkStopDeparted operation WHEN OnUndoMarkStopDepartedClicked is dispatched and UndoMarkStopDepartedUseCase succeeds THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.14 Write test: GIVEN undoMarkStopDeparted operation WHEN OnUndoMarkStopDepartedClicked is dispatched and UndoMarkStopDepartedUseCase fails THEN isLoading is false after completion in TripDetailViewModelTest
- [ ] 1.15 Implement: Wrap undoMarkStopDeparted() with isLoading = true/false in TripDetailViewModel

## 2. SetStopDialog — Loading State (BDD)

- [ ] 2.1 Write test: GIVEN SetStopDialog with isLoading true WHEN rendered THEN confirm button is disabled in SetStopDialog Compose UI test
- [ ] 2.2 Write test: GIVEN SetStopDialog with isLoading true WHEN rendered THEN CircularProgressIndicator is visible in SetStopDialog Compose UI test
- [ ] 2.3 Write test: GIVEN SetStopDialog with isLoading false and valid input WHEN rendered THEN confirm button is enabled in SetStopDialog Compose UI test
- [ ] 2.4 Implement: Add isLoading parameter to SetStopDialog, disable confirm button when loading, show CircularProgressIndicator in confirm button slot

## 3. CreateTripDialog — Loading State (BDD)

- [ ] 3.1 Write test: GIVEN CreateTripDialog with isLoading true WHEN rendered THEN confirm button is disabled in CreateTripDialog Compose UI test
- [ ] 3.2 Write test: GIVEN CreateTripDialog with isLoading true WHEN rendered THEN CircularProgressIndicator is visible in CreateTripDialog Compose UI test
- [ ] 3.3 Write test: GIVEN CreateTripDialog with isLoading false and valid name WHEN rendered THEN confirm button is enabled in CreateTripDialog Compose UI test
- [ ] 3.4 Implement: Add isLoading parameter to CreateTripDialog, disable confirm button when loading, show CircularProgressIndicator in confirm button slot

## 4. StopSection — Loading State (BDD)

- [ ] 4.1 Write test: GIVEN StopSection with isLoading true WHEN rendered with action buttons THEN all action buttons are disabled in StopSection Compose UI test
- [ ] 4.2 Write test: GIVEN StopSection with isLoading false WHEN rendered with action buttons THEN all action buttons are enabled in StopSection Compose UI test
- [ ] 4.3 Implement: Add isLoading parameter to StopSection, disable IconButtons and OutlinedButtons when loading

## 5. Screen Wiring — TripDetailScreen

- [ ] 5.1 Pass uiState.isLoading to all four SetStopDialog instances (starting point, add stop, edit stop, destination) in TripDetailScreen
- [ ] 5.2 Pass uiState.isLoading to all StopSection instances (starting point, intermediates, destination) in TripDetailScreen
- [ ] 5.3 Disable the "Add Stop" OutlinedButton when uiState.isLoading is true in TripDetailScreen
- [ ] 5.4 Disable the remove-stop confirmation dialog confirm button when uiState.isLoading is true in TripDetailScreen

## 6. Screen Wiring — TripListScreen

- [ ] 6.1 Pass uiState.isLoading to CreateTripDialog in TripListScreen

## 7. Previews

- [ ] 7.1 Add loading-state preview for SetStopDialog (StopForm with isLoading = true showing spinner and disabled confirm)
- [ ] 7.2 Add loading-state preview for CreateTripDialog (isLoading = true showing spinner and disabled confirm)
- [ ] 7.3 Verify existing PreviewTripDetailScreenLoading preview now visually shows disabled buttons (no code change needed if wiring is correct)
- [ ] 7.4 Verify existing PreviewTripListScreenLoading preview now visually shows loading state (no code change needed if wiring is correct)

## 8. Compose UI Tests — Screen Level

- [ ] 8.1 Write test: GIVEN TripDetailScreen with isLoading true WHEN rendered THEN action buttons are disabled in TripDetailScreenTest
- [ ] 8.2 Write test: GIVEN TripListScreen with isLoading true and dialog visible WHEN rendered THEN confirm button is disabled in TripListScreenTest

## 9. Verification

- [ ] 9.1 Run ./gradlew check and verify all tests pass
- [ ] 9.2 Update OpenSpec main specs via /opsx:sync
