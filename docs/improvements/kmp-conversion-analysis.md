# KMP Conversion Analysis — Venice ("Heading to Venice!")

## Context

The project is a single-module Android app (~43 Kotlin source files) with Clean Architecture (data/domain/UI layers), MVI pattern, and Jetpack Compose. The goal is to convert to Kotlin Multiplatform targeting **Android + iOS with shared UI via Compose Multiplatform**.

---

## Current Architecture Summary

| Layer | Key Tech | Files | KMP-Ready? |
|-------|----------|-------|------------|
| **Domain models** | Pure Kotlin data classes, enums | ~4 | Yes |
| **Domain interfaces** | Kotlin interfaces, Flow | ~2 | Yes |
| **Use cases** | Pure Kotlin, coroutines | ~4 | Yes |
| **MVI contracts** (State/Intent/Effect) | Pure Kotlin sealed/data classes | ~6 | Yes |
| **Repository impls** | Kotlin + Room DAOs | ~2 | Mostly (UUID/timestamp need expect/actual) |
| **Database** | Room (entities, DAOs, migrations) | ~6 | **No** — Room is Android-only |
| **ViewModels** | AndroidX ViewModel, Hilt, viewModelScope | ~2 | **No** — Android lifecycle dependency |
| **DI modules** | Hilt (@Module, @InstallIn) | ~2 | **No** — Hilt is Android-only |
| **Compose UI** | Jetpack Compose, Material 3, Navigation Compose | ~10 | **No** — Android Compose (Compose Multiplatform is the path) |
| **Entry points** | MainActivity, VeniceApplication | ~2 | **No** — Android platform glue |
| **Resources** | strings.xml (EN/ES/IT), drawables, themes | — | **No** — Android resource system |

---

## What's Already Portable (~40% of codebase)

These files can move to a `shared` KMP module with zero or trivial changes:

- **Domain models**: `Trip`, `Stop`, `StopStatus`, `StopType`
- **Repository interfaces**: `TripRepository`, `StopRepository`
- **Use cases**: `CreateTripUseCase`, `ObserveStopsUseCase`, `SetStopUseCase`, `MoveStopUseCase`
- **MVI contracts**: All `UiState`, `UiIntent`, `UiEffect` classes
- **Mappers**: `TripMapper`, `StopMapper` (logic is pure Kotlin)
- **Unit tests** for use cases and repository logic (mockk + coroutines.test have KMP support)

---

## What Needs Replacing or Abstracting

### 1. Database — Room → SQLDelight or Room KMP (HIGH effort)

Room has experimental KMP support as of Room 2.7+, but with limitations. The cleaner KMP-native option is **SQLDelight**.

**Work required:**
- Rewrite `TripDao` and `StopDao` as SQLDelight `.sq` files (the SQL itself is mostly reusable)
- Replace `TripEntity`/`StopEntity` Room annotations with SQLDelight-generated types
- Rewrite `AppDatabase` setup and migration from Room's builder pattern to SQLDelight's driver pattern
- Provide platform-specific `SqlDriver` via expect/actual (Android: `AndroidSqliteDriver`, iOS: `NativeSqliteDriver`)
- Port the v1→v2 migration

**Files affected:** `AppDatabase.kt`, `TripDao.kt`, `StopDao.kt`, `TripEntity.kt`, `StopEntity.kt`, `TripWithStopCount.kt`, `DatabaseModule.kt`

### 2. Dependency Injection — Hilt → Koin (MEDIUM effort)

Hilt is Android-only. **Koin** is the standard KMP-compatible DI framework.

**Work required:**
- Replace `DatabaseModule.kt` and `RepositoryModule.kt` with Koin module DSL
- Remove `@HiltAndroidApp` from `VeniceApplication`, replace with `startKoin {}`
- Remove `@AndroidEntryPoint` from `MainActivity`
- Remove `@HiltViewModel` from ViewModels, inject via Koin
- Update `hiltViewModel()` calls in Compose to `koinViewModel()`

**Files affected:** `DatabaseModule.kt`, `RepositoryModule.kt`, `VeniceApplication.kt`, `MainActivity.kt`, `TripListViewModel.kt`, `TripDetailViewModel.kt`, `MainScreen.kt`

### 3. ViewModels — AndroidX ViewModel → KMP ViewModel (LOW-MEDIUM effort)

Since Kotlin 2.0+, `androidx.lifecycle:lifecycle-viewmodel` has KMP support. Alternatively, use a KMP-native approach.

**Work required:**
- Replace `viewModelScope` with KMP-compatible coroutine scope (or use the KMP lifecycle-viewmodel artifact)
- Replace `SavedStateHandle` usage in `TripDetailViewModel` with constructor parameter or navigation argument passing
- The core intent-routing and state management logic stays unchanged

**Files affected:** `TripListViewModel.kt`, `TripDetailViewModel.kt`

### 4. UI — Jetpack Compose → Compose Multiplatform (HIGH effort)

Since shared UI is the goal, adopt **Compose Multiplatform** (JetBrains).

**Work required:**
- Migrate Compose dependencies from AndroidX Compose to JetBrains Compose Multiplatform
- Replace `stringResource(R.string.*)` with a multiplatform string solution (e.g., `moko-resources`, `lyricist`, or Compose Multiplatform's built-in resource system)
- Replace `Navigation Compose` with a KMP-compatible navigation library (e.g., `Voyager`, `Decompose`, or Compose Multiplatform's navigation)
- Compose UI code itself is largely reusable — Material 3 components, layouts, and modifiers work in Compose Multiplatform
- Create iOS entry point (`MainViewController`)

**Files affected:** All files under `ui/screens/`, `ui/theme/`, `MainScreen.kt`, `MainActivity.kt`

### 5. Platform Utilities (TRIVIAL)

- `UUID.randomUUID()` → `kotlin.uuid.Uuid` (available in Kotlin 2.0+) or expect/actual
- `System.currentTimeMillis()` → `Clock.System.now()` from `kotlinx-datetime`

---

## Recommended Module Structure

```
venice/
├── shared/                         # KMP shared module
│   ├── commonMain/
│   │   ├── domain/                 # Models, interfaces, use cases (moved as-is)
│   │   ├── data/                   # Repository impls, SQLDelight, mappers
│   │   └── di/                     # Koin modules for shared dependencies
│   ├── androidMain/                # Android-specific: SqlDriver, platform utils
│   └── iosMain/                    # iOS-specific: SqlDriver, platform utils
├── app/                            # Android app (Compose UI, Android entry points)
│   └── (existing UI code, adapted for Compose Multiplatform or kept Android-only)
└── iosApp/                         # iOS app (SwiftUI or Compose Multiplatform entry point)
```

---

## Effort Estimate

| Work Item | Effort | Notes |
|-----------|--------|-------|
| Project restructuring (shared module, Gradle KMP setup) | 1-2 days | New module, expect/actual scaffolding |
| Database migration (Room → SQLDelight) | 2-3 days | SQL rewrite, driver setup, migration porting |
| DI migration (Hilt → Koin) | 0.5-1 day | Straightforward DSL replacement |
| ViewModel adaptation | 0.5 day | Minor scope/lifecycle changes |
| Platform utilities (UUID, time) | 0.5 day | Trivial expect/actual or library swap |
| UI → Compose Multiplatform | 3-5 days | Navigation, resources, iOS entry point |
| Test migration | 1 day | Move tests to shared, adapt framework |
| **Total** | **8-13 days** | |

---

## Remaining Decision Points

1. **Database**: SQLDelight (mature KMP) vs Room KMP (newer, experimental)?
2. **Navigation**: Voyager, Decompose, or Compose Multiplatform Navigation?
3. **Phased or big-bang**: Migrate incrementally (shared module first, then UI) or all at once?

---

## Verification

After conversion:
- `./gradlew :shared:allTests` — runs common + platform-specific tests
- `./gradlew :app:assembleDebug` — Android app builds and runs
- iOS app builds via Xcode or `./gradlew :shared:linkDebugFrameworkIos`
- All existing unit tests pass in the shared module
- Manual verification: app behavior unchanged on Android
