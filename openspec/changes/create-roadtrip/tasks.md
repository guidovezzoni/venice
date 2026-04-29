## 1. Dependencies & Project Setup

- [ ] 1.1 Add Room dependencies (runtime, KTX, compiler/KSP) to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [ ] 1.2 Add Hilt dependencies (hilt-android, hilt-compiler/KSP, hilt-navigation-compose) to `gradle/libs.versions.toml` and `app/build.gradle.kts`; apply the `dagger.hilt.android.plugin` and KSP plugin
- [ ] 1.3 Add Navigation Compose dependency to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [ ] 1.4 Add MockK and kotlinx-coroutines-test dependencies for unit testing
- [ ] 1.5 Create package structure: `data/database/entity`, `data/database/dao`, `data/database/mapper`, `data/repository`, `domain/model`, `domain/repository`, `domain/usecase`, `ui/screens/triplist`, `ui/state`, `ui/intent`, `ui/effect`, `ui/viewmodel`, `di`

## 2. Data Layer — Room Database

- [ ] 2.1 Create `TripEntity` data class with Room annotations (`@Entity`, `@PrimaryKey` with `String` UUID, fields: `id`, `name`, `createdAt`, `updatedAt`)
- [ ] 2.2 Create `TripDao` interface with `insert`, `observeAll`, and `getById` methods
- [ ] 2.3 Create `AppDatabase` abstract class extending `RoomDatabase`, exposing `TripDao`
- [ ] 2.4 Create `TripMapper.kt` with `TripEntity.toDomain(): Trip` extension function

## 3. Domain Layer

- [ ] 3.1 Create `Trip` domain model data class (`id: String`, `name`, `createdAt`, `updatedAt`)
- [ ] 3.2 Create `TripRepository` interface in domain layer with `createTrip(name: String): Result<Trip>` and `observeTrips(): Flow<List<Trip>>`
- [ ] 3.3 Create `TripRepositoryImpl` implementing `TripRepository` — generates UUID, trims name, sets timestamps, inserts via DAO, maps to domain model
- [ ] 3.4 Create `CreateTripUseCase` — validates name is not blank, delegates to repository

## 4. DI & Wiring (Hilt)

- [ ] 4.1 Create `@HiltAndroidApp` Application class
- [ ] 4.2 Annotate `MainActivity` with `@AndroidEntryPoint`
- [ ] 4.3 Create `DatabaseModule` (`@Module @InstallIn(SingletonComponent)`) providing `AppDatabase` and `TripDao`
- [ ] 4.4 Create `RepositoryModule` (`@Module @InstallIn(SingletonComponent)`) binding `TripRepository` to `TripRepositoryImpl`
- [ ] 4.5 Annotate `TripRepositoryImpl` and `CreateTripUseCase` with `@Inject`
- [ ] 4.6 Annotate `TripListViewModel` with `@HiltViewModel` and `@Inject constructor`

## 5. UI Layer — MVI Contract

- [ ] 5.1 Create `TripListUiState` data class (trips list, `isCreateDialogVisible`, `isLoading`)
- [ ] 5.2 Create `TripListUiIntent` sealed class (`OnCreateTripClicked`, `OnDismissCreateDialog`, `OnTripNameChanged`, `ConfirmCreateTrip`)
- [ ] 5.3 Create `TripListUiEffect` sealed class (`NavigateToTripDetail(tripId)`, `ShowError(message)`)
- [ ] 5.4 Create `TripListViewModel` with `uiState: StateFlow`, `uiEffect: SharedFlow`, and `onIntent()` — handles create trip flow, observes trips from repository

## 6. UI Layer — Screens & Composables

- [ ] 6.1 Create `CreateTripDialog` stateless composable (text field with 100-char max, Confirm/Cancel buttons, previews for empty and filled states)
- [ ] 6.2 Create `TripListScreen` composable with Scaffold, FAB, trip list display, and empty state
- [ ] 6.3 Create placeholder `TripDetailScreen` composable (navigation target showing trip ID)
- [ ] 6.4 Set up Navigation Compose graph in `MainScreen` with `tripList` and `tripDetail/{tripId}` routes
- [ ] 6.5 Wire `TripListScreen` to `TripListViewModel` — collect state, collect effects, dispatch intents

## 7. Internationalisation

- [ ] 7.1 Add all new strings to `res/values/strings.xml` (FAB label, dialog title, field label, placeholder, confirm, cancel, error message)
- [ ] 7.2 Update other locale `strings.xml` files if present

## 8. Unit Tests

- [ ] 8.1 Write `CreateTripUseCaseTest` — blank name returns failure; valid name delegates to repository
- [ ] 8.2 Write `TripRepositoryImplTest` — successful insert returns Trip with ID; DAO failure returns Result.failure
- [ ] 8.3 Write `TripListViewModelTest` — create trip intent with valid name sets navigation effect; use case failure emits error effect; open/dismiss dialog intents update state

## 9. Verification

- [ ] 9.1 Run `./gradlew clean` then `./gradlew assembleDebug` — build passes
- [ ] 9.2 Run `./gradlew test` — all unit tests pass
- [ ] 9.3 Run `./gradlew check` — no new lint warnings
