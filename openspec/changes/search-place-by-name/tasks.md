## 1. Dependencies and Build Configuration

- [x] 1.1 Add Google Places SDK and kotlinx-coroutines-play-services to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Enable `buildFeatures.buildConfig = true` and add `buildConfigField` for `MAPS_API_KEY` from `local.properties` in `app/build.gradle.kts`
- [x] 1.3 Verify the project compiles with `./gradlew assembleDebug`

## 2. Domain Models

- [x] 2.1 Create `PlaceSuggestion` data class in `domain/model/PlaceSuggestion.kt`
- [x] 2.2 Create `PlaceDetail` data class in `domain/model/PlaceDetail.kt`

## 3. Domain Repository Interface

- [x] 3.1 Create `PlaceSearchRepository` interface in `domain/repository/PlaceSearchRepository.kt` with `getAutocompleteSuggestions`, `getPlaceDetails`, and `resetSession`

## 4. Data Layer — Mappers (BDD)

- [x] 4.1 Write test: GIVEN an AutocompletePrediction WHEN mapped THEN returns PlaceSuggestion with correct fields in `PlaceSuggestionMapperTest`
- [x] 4.2 Implement `PlaceSuggestionMapper` in `data/mapper/PlaceSuggestionMapper.kt`
- [x] 4.3 Write test: GIVEN a Place with displayName and location WHEN mapped THEN returns PlaceDetail with correct fields; GIVEN a Place with null location WHEN mapped THEN throws in `PlaceDetailMapperTest`
- [x] 4.4 Implement `PlaceDetailMapper` in `data/mapper/PlaceDetailMapper.kt`

## 5. Data Layer — Repository Implementation (BDD)

- [x] 5.1 Write test: GIVEN no session token WHEN getAutocompleteSuggestions called THEN token auto-created; GIVEN blank query WHEN called THEN returns empty list; GIVEN valid query WHEN API succeeds THEN returns mapped suggestions; GIVEN valid query WHEN API fails THEN returns failure in `PlaceSearchRepositoryImplTest`
- [x] 5.2 Write test: GIVEN a placeId WHEN getPlaceDetails succeeds THEN returns mapped PlaceDetail and clears token; GIVEN a placeId WHEN API fails THEN returns failure in `PlaceSearchRepositoryImplTest`
- [x] 5.3 Write test: GIVEN an active session WHEN resetSession called THEN token is cleared in `PlaceSearchRepositoryImplTest`
- [x] 5.4 Implement `PlaceSearchRepositoryImpl` in `data/repository/PlaceSearchRepositoryImpl.kt`

## 6. Domain Layer — Use Cases (BDD)

- [x] 6.1 Write test: GIVEN a query WHEN SearchPlacesUseCase invoked THEN delegates to repository with trimmed query; GIVEN repository failure WHEN invoked THEN failure propagated in `SearchPlacesUseCaseTest`
- [x] 6.2 Implement `SearchPlacesUseCase` in `domain/usecase/SearchPlacesUseCase.kt`
- [x] 6.3 Write test: GIVEN a placeId WHEN GetPlaceDetailUseCase invoked THEN delegates to repository; GIVEN repository failure WHEN invoked THEN failure propagated in `GetPlaceDetailUseCaseTest`
- [x] 6.4 Implement `GetPlaceDetailUseCase` in `domain/usecase/GetPlaceDetailUseCase.kt`

## 7. DI Wiring

- [x] 7.1 Create `PlacesModule` in `di/PlacesModule.kt` providing `PlacesClient` singleton
- [x] 7.2 Add `PlaceSearchRepository` binding in `RepositoryModule`

## 8. Places SDK Initialisation

- [x] 8.1 Update `VeniceApplication.onCreate()` to call `Places.initializeWithNewPlacesApiEnabled`

## 9. UI State and Intents

- [x] 9.1 Add `placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail` fields to `TripDetailUiState`
- [x] 9.2 Add `OnSearchQueryChanged` and `OnSuggestionSelected` intents to `TripDetailUiIntent`

## 10. ViewModel — Search Logic (BDD)

- [x] 10.1 Write test: GIVEN OnSearchQueryChanged dispatched WHEN debounce elapses THEN SearchPlacesUseCase called and placeSuggestions updated; GIVEN blank query WHEN dispatched THEN search state cleared without use case call in `TripDetailViewModelTest`
- [x] 10.2 Write test: GIVEN OnSearchQueryChanged dispatched twice within 300ms WHEN debounce elapses THEN only last query triggers use case call in `TripDetailViewModelTest`
- [x] 10.3 Write test: GIVEN OnSuggestionSelected dispatched WHEN GetPlaceDetailUseCase succeeds THEN selectedPlaceDetail set and suggestions cleared; WHEN fails THEN ShowError emitted in `TripDetailViewModelTest`
- [x] 10.4 Write test: GIVEN any dialog dismiss intent dispatched THEN placeSuggestions cleared, isSearchingPlaces false, searchError null, selectedPlaceDetail null, resetSession called in `TripDetailViewModelTest`
- [x] 10.5 Implement search intent handling in `TripDetailViewModel`: inject `SearchPlacesUseCase`, `GetPlaceDetailUseCase`, and `PlaceSearchRepository`; add debounce Job; handle `OnSearchQueryChanged`, `OnSuggestionSelected`; clear search state on dialog dismiss intents

## 11. SetStopDialog — Autocomplete UI (BDD)

- [x] 11.1 Write Compose UI test: GIVEN suggestions list is non-empty WHEN dialog rendered THEN suggestion items displayed with primary and secondary text in `SetStopDialogTest`
- [x] 11.2 Write Compose UI test: GIVEN a suggestion is tapped WHEN user clicks it THEN onSuggestionSelected callback fires in `SetStopDialogTest`
- [x] 11.3 Write Compose UI test: GIVEN isSearchingPlaces is true WHEN dialog rendered THEN search loading indicator displayed in `SetStopDialogTest`
- [x] 11.4 Write Compose UI test: GIVEN selectedPlaceDetail is non-null WHEN dialog rendered THEN latitude and longitude fields are read-only in `SetStopDialogTest`
- [x] 11.5 Update `SetStopDialog` composable: add `suggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail`, `onSearchQueryChanged`, `onSuggestionSelected` parameters; render suggestion list below place name field; show search loading indicator; implement `LaunchedEffect` for `selectedPlaceDetail` writeback; implement read-only coordinates via local `coordinatesFromAutocomplete` flag
- [x] 11.6 Update `StopForm` composable: accept `readOnly` parameter for latitude/longitude fields
- [x] 11.7 Add Compose previews for new states: suggestion list visible, search loading, read-only coordinates after selection

## 12. TripDetailScreen — Wire Search to Dialogs

- [x] 12.1 Update all 4 `SetStopDialog` call sites in `TripDetailScreen` to pass search-related state and callbacks from `uiState`

## 13. String Resources

- [x] 13.1 Add search-related strings (`trip_detail_search_no_results`, `trip_detail_search_unavailable`, `trip_detail_search_loading`) to `strings.xml` (EN, IT, ES)

## 14. Previews and Documentation

- [x] 14.1 Add `TripDetailScreen` preview showing dialog with suggestion list
- [x] 14.2 Ensure all new `UiState` fields appear in at least one preview with non-default values

## 15. Verification

- [x] 15.1 Run `./gradlew clean check` — all unit tests pass
- [x] 15.2 Run `./gradlew connectedDebugAndroidTest` — all Compose UI tests pass
- [x] 15.3 On-device verification: type a place name, see suggestions, select one, coordinates auto-fill, latitude/longitude become read-only, stop saves correctly
- [x] 15.4 On-device verification: edit an existing stop, search for a new place, select it, save — stop updated correctly
- [x] 15.5 On-device verification: manual entry still works without selecting a suggestion
