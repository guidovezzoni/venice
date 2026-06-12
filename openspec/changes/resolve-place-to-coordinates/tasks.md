## 1. Prerequisites (state & strings)

- [ ] 1.1 Add `isResolvingPlace: Boolean = false` and `placeDetailError: String? = null` fields to `TripDetailUiState`
- [ ] 1.2 Add string resource `trip_detail_place_detail_error` ("Could not resolve place. Tap a suggestion to try again.") to `strings.xml` and translate to other languages if present
- [ ] 1.3 Add string resource `global_resolving_place` ("Resolving place") to `strings.xml` for the spinner content description and translate to other languages if present

## 2. ViewModel resolving state (BDD)

- [ ] 2.1 Write test: GIVEN a suggestion is selected WHEN `GetPlaceDetailUseCase` is in-flight THEN `isResolvingPlace` is `true` in `TripDetailViewModelTest`
- [ ] 2.2 Write test: GIVEN a suggestion is selected WHEN `GetPlaceDetailUseCase` succeeds THEN `isResolvingPlace` is `false` and `selectedPlaceDetail` is populated in `TripDetailViewModelTest`
- [ ] 2.3 Implement: Update `OnSuggestionSelected` handler in `TripDetailViewModel` to set `isResolvingPlace = true` before the call and `isResolvingPlace = false` on success, along with setting `selectedPlaceDetail` and clearing `placeSuggestions`

## 3. ViewModel inline error (BDD)

- [ ] 3.1 Write test: GIVEN a suggestion is selected WHEN `GetPlaceDetailUseCase` fails THEN `isResolvingPlace` is `false` and `placeDetailError` contains the error message in `TripDetailViewModelTest`
- [ ] 3.2 Implement: Update `OnSuggestionSelected` handler in `TripDetailViewModel` to set `placeDetailError` on failure instead of emitting `ShowError` effect, and set `isResolvingPlace = false`
- [ ] 3.3 Update existing test `GIVEN OnSuggestionSelected dispatched WHEN GetPlaceDetailUseCase fails THEN ShowError emitted` in `TripDetailViewModelTest` to assert `placeDetailError` is set instead of checking for `ShowError` effect

## 4. ViewModel error clearing (BDD)

- [ ] 4.1 Write test: GIVEN `placeDetailError` is set WHEN `OnSearchQueryChanged` is dispatched with a non-blank query THEN `placeDetailError` is cleared in `TripDetailViewModelTest`
- [ ] 4.2 Write test: GIVEN `placeDetailError` is set WHEN `OnSuggestionSelected` is dispatched THEN `placeDetailError` is cleared in `TripDetailViewModelTest`
- [ ] 4.3 Implement: In `OnSearchQueryChanged` handler, clear `placeDetailError` when query is non-blank. In `OnSuggestionSelected` handler, clear `placeDetailError` before starting the API call
- [ ] 4.4 Implement: Update `clearSearchState()` to also reset `isResolvingPlace` to `false` and `placeDetailError` to `null`

## 5. SetStopDialog resolving indicator (BDD)

- [ ] 5.1 Write Compose UI test: GIVEN `isResolvingPlace` is `true` THEN the progress indicator with content description "Resolving place" is visible and confirm button is disabled in `SetStopDialogTest`
- [ ] 5.2 Write Compose UI test: GIVEN `placeDetailError` is set THEN the error message is displayed inline in `SetStopDialogTest`
- [ ] 5.3 Write Compose UI test: GIVEN `isResolvingPlace` is `false` and `placeDetailError` is `null` THEN no resolving indicator or error is shown in `SetStopDialogTest`
- [ ] 5.4 Implement: Add `isResolvingPlace: Boolean = false` and `placeDetailError: String? = null` parameters to `SetStopDialog` and `StopForm`. Show `CircularProgressIndicator` with content description "Resolving place" when `isResolvingPlace` is `true`. Show inline error `Text` when `placeDetailError` is not null. Disable confirm button when `isResolvingPlace` is `true`

## 6. TripDetailScreen wiring

- [ ] 6.1 Pass `isResolvingPlace = uiState.isResolvingPlace` and `placeDetailError = uiState.placeDetailError` to all four `SetStopDialog` invocations in `TripDetailScreen` (starting point, destination, add stop, edit stop)

## 7. Compose previews

- [ ] 7.1 Add `PreviewSetStopDialogResolvingPlace` preview showing `StopForm` with `isResolvingPlace = true`
- [ ] 7.2 Add `PreviewSetStopDialogPlaceDetailError` preview showing `StopForm` with `placeDetailError` set
- [ ] 7.3 Add `PreviewTripDetailScreenResolvingPlace` preview showing `TripDetailScreen` with a dialog open and `isResolvingPlace = true`

## 8. Verification

- [ ] 8.1 Run `./gradlew check` and confirm all unit tests pass
- [ ] 8.2 Run `./gradlew connectedDebugAndroidTest` and confirm all Compose UI tests pass (if device available)
- [ ] 8.3 Verify that all new `TripDetailUiState` fields appear in non-default values in at least one preview
