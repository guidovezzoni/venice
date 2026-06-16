## 1. Prerequisites — Dependencies, Models, and Data Layer Setup

- [ ] 1.1 Add OkHttp dependency to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [ ] 1.2 Create `domain/model/Leg.kt` — data class with `id`, `tripId`, `fromStopId`, `toStopId`, `distanceMetres`, `durationSeconds`, `encodedPolyline`
- [ ] 1.3 Create `data/database/entity/LegEntity.kt` — Room entity for `legs` table with FK to `trips` (ON DELETE CASCADE) and index on `tripId`
- [ ] 1.4 Create `data/database/dao/LegDao.kt` — DAO with `insertAll`, `deleteByTripId`, `observeByTripId`, `getByTripId`
- [ ] 1.5 Create `data/database/mapper/LegEntityMapper.kt` — extension functions `LegEntity.toDomain()` and `Leg.toEntity()`
- [ ] 1.6 Update `data/database/AppDatabase.kt` — add `LegEntity` to `@Database`, add `legDao()`, bump version to 3, add `MIGRATION_2_3`
- [ ] 1.7 Create `data/network/dto/DirectionsResponse.kt` — DTOs `DirectionsResponse` and `DirectionsLeg` with `fromJson` factory
- [ ] 1.8 Create `data/network/DirectionsApiService.kt` — OkHttp-based HTTP client for Google Directions API. All OkHttp imports MUST be confined to this class. Include TODO: `Replace with Retrofit when additional API endpoints are added`
- [ ] 1.9 Create `data/mapper/LegMapper.kt` — maps `DirectionsLeg` to domain `Leg` with `tripId`, `fromStopId`, `toStopId` parameters
- [ ] 1.10 Create `domain/repository/RouteRepository.kt` — interface with `calculateRoute`, `deleteLegsForTrip`, `observeLegsForTrip`
- [ ] 1.11 Create `data/repository/RouteRepositoryImpl.kt` — implements `RouteRepository` using `DirectionsApiService`, `LegDao`, mappers

## 2. DI Wiring

- [ ] 2.1 Create `di/NetworkModule.kt` — Hilt module providing singleton `OkHttpClient`. Include TODO: `Replace with Retrofit module when additional API endpoints are added`
- [ ] 2.2 Update `di/DatabaseModule.kt` — expose `legDao()` and register `MIGRATION_2_3`
- [ ] 2.3 Update `di/RepositoryModule.kt` — bind `RouteRepositoryImpl` to `RouteRepository`

## 3. CalculateRouteUseCase (BDD)

- [ ] 3.1 Write test: GIVEN 3 stops WHEN CalculateRouteUseCase is invoked THEN RouteRepository.calculateRoute is called and Result.success is returned — in `CalculateRouteUseCaseTest`
- [ ] 3.2 Write test: GIVEN 1 stop WHEN CalculateRouteUseCase is invoked THEN Result.failure(IllegalStateException) is returned — in `CalculateRouteUseCaseTest`
- [ ] 3.3 Write test: GIVEN repository failure WHEN CalculateRouteUseCase is invoked THEN Result.failure is propagated — in `CalculateRouteUseCaseTest`
- [ ] 3.4 Implement `domain/usecase/CalculateRouteUseCase.kt` — validate ≥2 stops, delegate to repository

## 4. InvalidateRouteUseCase (BDD)

- [ ] 4.1 Write test: GIVEN a tripId WHEN InvalidateRouteUseCase is invoked THEN RouteRepository.deleteLegsForTrip is called — in `InvalidateRouteUseCaseTest`
- [ ] 4.2 Write test: GIVEN repository failure WHEN InvalidateRouteUseCase is invoked THEN Result.failure is propagated — in `InvalidateRouteUseCaseTest`
- [ ] 4.3 Implement `domain/usecase/InvalidateRouteUseCase.kt` — delegate to repository

## 5. ObserveLegsUseCase (BDD)

- [ ] 5.1 Write test: GIVEN a tripId WHEN ObserveLegsUseCase is invoked THEN a Flow from RouteRepository is returned — in `ObserveLegsUseCaseTest`
- [ ] 5.2 Implement `domain/usecase/ObserveLegsUseCase.kt` — delegate to repository

## 6. RouteRepositoryImpl (BDD)

- [ ] 6.1 Write test: GIVEN 3 stops and API returns 2 legs WHEN calculateRoute is called THEN existing legs are deleted and 2 new legs are persisted — in `RouteRepositoryImplTest`
- [ ] 6.2 Write test: GIVEN API failure WHEN calculateRoute is called THEN Result.failure is returned — in `RouteRepositoryImplTest`
- [ ] 6.3 Write test: GIVEN legs exist WHEN deleteLegsForTrip is called THEN legs are deleted — in `RouteRepositoryImplTest`
- [ ] 6.4 Write test: GIVEN legs exist WHEN observeLegsForTrip is called THEN Flow emits mapped domain legs — in `RouteRepositoryImplTest`
- [ ] 6.5 Implement/verify `RouteRepositoryImpl` to pass all tests

## 7. DirectionsResponse Parsing (BDD)

- [ ] 7.1 Write test: GIVEN valid Directions API JSON WHEN fromJson is called THEN DirectionsResponse with correct legs is returned — in `DirectionsResponseTest`
- [ ] 7.2 Write test: GIVEN JSON with empty routes WHEN fromJson is called THEN DirectionsResponse with empty legs is returned — in `DirectionsResponseTest`
- [ ] 7.3 Verify `DirectionsResponse.fromJson` implementation passes tests

## 8. LegMapper (BDD)

- [ ] 8.1 Write test: GIVEN a DirectionsLeg WHEN mapped with tripId and stopIds THEN a Leg with correct fields and UUID id is returned — in `LegMapperTest`
- [ ] 8.2 Verify `LegMapper` implementation passes tests

## 9. Stop Use Case Invalidation Wiring (BDD)

- [ ] 9.1 Write test: GIVEN a successful stop set WHEN SetStopUseCase is invoked THEN InvalidateRouteUseCase is called — in `SetStopUseCaseTest` (update existing tests)
- [ ] 9.2 Write test: GIVEN a failed stop set WHEN SetStopUseCase is invoked THEN InvalidateRouteUseCase is NOT called — in `SetStopUseCaseTest`
- [ ] 9.3 Update `SetStopUseCase` — inject `InvalidateRouteUseCase`, call on success
- [ ] 9.4 Write test: GIVEN a successful move WHEN MoveStopUseCase is invoked THEN InvalidateRouteUseCase is called — in `MoveStopUseCaseTest` (update existing tests)
- [ ] 9.5 Write test: GIVEN a failed move WHEN MoveStopUseCase is invoked THEN InvalidateRouteUseCase is NOT called — in `MoveStopUseCaseTest`
- [ ] 9.6 Update `MoveStopUseCase` — inject `InvalidateRouteUseCase`, call on success
- [ ] 9.7 Write test: GIVEN a successful edit WHEN EditStopUseCase is invoked THEN InvalidateRouteUseCase is called with stop's tripId — in `EditStopUseCaseTest` (update existing tests)
- [ ] 9.8 Write test: GIVEN a failed edit WHEN EditStopUseCase is invoked THEN InvalidateRouteUseCase is NOT called — in `EditStopUseCaseTest`
- [ ] 9.9 Update `EditStopUseCase` — inject `InvalidateRouteUseCase`, call on success using result's `tripId`, remove TODO comment
- [ ] 9.10 Write test: GIVEN a successful removal WHEN RemoveStopUseCase is invoked THEN InvalidateRouteUseCase is called — in `RemoveStopUseCaseTest` (update existing tests)
- [ ] 9.11 Write test: GIVEN a failed removal WHEN RemoveStopUseCase is invoked THEN InvalidateRouteUseCase is NOT called — in `RemoveStopUseCaseTest`
- [ ] 9.12 Update `RemoveStopUseCase` — inject `InvalidateRouteUseCase`, call on success, remove TODO comment

## 10. TripDetailViewModel Route Handling (BDD)

- [ ] 10.1 Write test: GIVEN legs exist in database WHEN ViewModel initialises THEN uiState.legs contains the persisted legs — in `TripDetailViewModelTest`
- [ ] 10.2 Write test: GIVEN OnCalculateRouteClicked dispatched WHEN CalculateRouteUseCase succeeds THEN isCalculatingRoute becomes false and routeError is null — in `TripDetailViewModelTest`
- [ ] 10.3 Write test: GIVEN OnCalculateRouteClicked dispatched WHEN CalculateRouteUseCase fails THEN isCalculatingRoute becomes false and routeError contains error message — in `TripDetailViewModelTest`
- [ ] 10.4 Write test: GIVEN OnCalculateRouteClicked dispatched WHEN API call is in-flight THEN isCalculatingRoute is true — in `TripDetailViewModelTest`
- [ ] 10.5 Update `TripDetailViewModel` — inject `CalculateRouteUseCase` and `ObserveLegsUseCase`, handle `OnCalculateRouteClicked`, observe legs
- [ ] 10.6 Update `TripDetailUiState` — add `legs: List<Leg>`, `isCalculatingRoute: Boolean`, `routeError: String?`
- [ ] 10.7 Update `TripDetailUiIntent` — add `OnCalculateRouteClicked`

## 11. String Resources

- [ ] 11.1 Add route calculation string resources to `strings.xml`: `trip_detail_calculate_route`, `trip_detail_calculating_route`, `trip_detail_route_error`, `trip_detail_leg_distance_metres`, `trip_detail_leg_distance_kilometres`, `trip_detail_leg_duration_minutes`, `trip_detail_leg_duration_hours_minutes`

## 12. UI Composables

- [ ] 12.1 Create `ui/screens/tripdetail/LegSummary.kt` — composable showing formatted distance/duration between stops
- [ ] 12.2 Add previews for `LegSummary` — short distance and long distance variants
- [ ] 12.3 Update `TripDetailScreen` — render "Calculate route" button when ≥2 stops exist, show loading state, display route error, render `LegSummary` between consecutive stops
- [ ] 12.4 Add previews for route states in `TripDetailScreen` — legs between stops, calculating state, route error state

## 13. Compose UI Tests

- [ ] 13.1 Write Compose UI test: GIVEN ≥2 stops WHEN screen renders THEN "Calculate route" button is visible — in `TripDetailScreenTest`
- [ ] 13.2 Write Compose UI test: GIVEN <2 stops WHEN screen renders THEN "Calculate route" button is not visible — in `TripDetailScreenTest`
- [ ] 13.3 Write Compose UI test: GIVEN isCalculatingRoute is true WHEN screen renders THEN button is disabled and loading indicator shown — in `TripDetailScreenTest`
- [ ] 13.4 Write Compose UI test: GIVEN routeError is set WHEN screen renders THEN error message is displayed — in `TripDetailScreenTest`
- [ ] 13.5 Write Compose UI test: GIVEN button is tapped WHEN screen is interactive THEN OnCalculateRouteClicked intent is dispatched — in `TripDetailScreenTest`

## 14. Final Verification

- [ ] 14.1 Run `./gradlew clean check` — all tests pass, no lint errors
- [ ] 14.2 Run `./gradlew connectedDebugAndroidTest` — Compose UI tests pass on device
- [ ] 14.3 Verify on device: create trip with ≥2 stops, tap "Calculate route", verify distance/duration appears between stops
- [ ] 14.4 Verify on device: edit a stop after calculation, verify legs are cleared and button reappears
