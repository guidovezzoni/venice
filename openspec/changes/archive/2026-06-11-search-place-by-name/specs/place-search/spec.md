## ADDED Requirements

### Requirement: PlaceSuggestion domain model
The domain layer SHALL define a `PlaceSuggestion` data class in `domain/model/` with fields:
- `placeId: String` — unique identifier from the Places API
- `primaryText: String` — the main place name (e.g. "Colosseum")
- `secondaryText: String` — disambiguating detail (e.g. "Rome, Metropolitan City of Rome Capital, Italy")

#### Scenario: PlaceSuggestion is a data class
- **WHEN** a `PlaceSuggestion` instance is created with `placeId = "abc"`, `primaryText = "Colosseum"`, `secondaryText = "Rome, Italy"`
- **THEN** it holds all fields and supports equality, copy, and destructuring

### Requirement: PlaceDetail domain model
The domain layer SHALL define a `PlaceDetail` data class in `domain/model/` with fields:
- `name: String` — the display name of the place
- `latitude: Double` — the latitude coordinate
- `longitude: Double` — the longitude coordinate

#### Scenario: PlaceDetail is a data class
- **WHEN** a `PlaceDetail` instance is created with `name = "Colosseum"`, `latitude = 41.8902`, `longitude = 12.4922`
- **THEN** it holds all fields and supports equality, copy, and destructuring

### Requirement: PlaceSearchRepository interface
The domain layer SHALL define a `PlaceSearchRepository` interface in `domain/repository/` with:
- `suspend fun getAutocompleteSuggestions(query: String): Result<List<PlaceSuggestion>>` — returns matching place suggestions for the given query
- `suspend fun getPlaceDetails(placeId: String): Result<PlaceDetail>` — returns coordinates and name for a place ID
- `fun resetSession()` — clears the current session token, ending the billing session without a place selection

The interface SHALL NOT expose session tokens or any Places SDK types.

#### Scenario: Repository interface is accessible from domain layer
- **WHEN** a use case references `PlaceSearchRepository`
- **THEN** it depends only on the domain layer, not the data layer or Places SDK

### Requirement: PlaceSearchRepositoryImpl manages session tokens internally
`PlaceSearchRepositoryImpl` SHALL manage `AutocompleteSessionToken` lifecycle:
- A new token SHALL be created automatically on the first `getAutocompleteSuggestions` call if no token exists
- The same token SHALL be reused across subsequent `getAutocompleteSuggestions` calls within the same session
- The token SHALL be consumed (cleared) after a successful `getPlaceDetails` call
- `resetSession()` SHALL clear the current token without making an API call

#### Scenario: Token auto-created on first search
- **WHEN** `getAutocompleteSuggestions` is called and no session token exists
- **THEN** a new `AutocompleteSessionToken` is created and used for the request

#### Scenario: Token reused across searches
- **WHEN** `getAutocompleteSuggestions` is called twice within the same session
- **THEN** both requests use the same `AutocompleteSessionToken`

#### Scenario: Token consumed after place details
- **WHEN** `getPlaceDetails` is called successfully
- **THEN** the session token is cleared; the next `getAutocompleteSuggestions` call creates a new token

#### Scenario: Token cleared on session reset
- **WHEN** `resetSession()` is called
- **THEN** the current token is cleared without any API call

### Requirement: PlaceSearchRepositoryImpl fetches autocomplete suggestions
`PlaceSearchRepositoryImpl.getAutocompleteSuggestions` SHALL:
1. Return `Result.success(emptyList())` if the query is blank
2. Build a `FindAutocompletePredictionsRequest` with the query and current session token
3. Call `PlacesClient.findAutocompletePredictions()` and await the result
4. Map each `AutocompletePrediction` to a `PlaceSuggestion` using `PlaceSuggestionMapper`
5. Return `Result.success(List<PlaceSuggestion>)` on success
6. Return `Result.failure` on any exception

#### Scenario: Blank query returns empty list
- **WHEN** `getAutocompleteSuggestions` is called with `query = ""`
- **THEN** `Result.success(emptyList())` is returned without making an API call

#### Scenario: Successful autocomplete
- **WHEN** `getAutocompleteSuggestions` is called with `query = "Colo"` and the API returns 3 predictions
- **THEN** `Result.success` is returned with 3 `PlaceSuggestion` items mapped from the predictions

#### Scenario: API failure
- **WHEN** `getAutocompleteSuggestions` is called and the API throws an exception
- **THEN** `Result.failure` with the exception is returned

### Requirement: PlaceSearchRepositoryImpl fetches place details
`PlaceSearchRepositoryImpl.getPlaceDetails` SHALL:
1. Build a `FetchPlaceRequest` with the `placeId`, requesting `Place.Field.DISPLAY_NAME` and `Place.Field.LOCATION`, and the current session token
2. Call `PlacesClient.fetchPlace()` and await the result
3. Map the `Place` to a `PlaceDetail` using `PlaceDetailMapper`
4. Clear the session token
5. Return `Result.success(PlaceDetail)` on success
6. Return `Result.failure` on any exception (including if `location` is null)

#### Scenario: Successful place details fetch
- **WHEN** `getPlaceDetails` is called with a valid `placeId` and the API returns a place with `displayName = "Colosseum"` and `location = LatLng(41.8902, 12.4922)`
- **THEN** `Result.success(PlaceDetail("Colosseum", 41.8902, 12.4922))` is returned

#### Scenario: Place has null location
- **WHEN** `getPlaceDetails` is called and the returned `Place.location` is `null`
- **THEN** `Result.failure` is returned with an appropriate exception

#### Scenario: API failure on fetch
- **WHEN** `getPlaceDetails` is called and the API throws an exception
- **THEN** `Result.failure` with the exception is returned

### Requirement: PlaceSuggestionMapper
The data layer SHALL define a `PlaceSuggestionMapper` in `data/mapper/` that converts an `AutocompletePrediction` to a `PlaceSuggestion`:
- `placeId` from `prediction.placeId`
- `primaryText` from `prediction.getPrimaryText(null).toString()`
- `secondaryText` from `prediction.getSecondaryText(null).toString()`

#### Scenario: Prediction mapped to suggestion
- **WHEN** an `AutocompletePrediction` has `placeId = "abc"`, primary text "Colosseum", secondary text "Rome, Italy"
- **THEN** the mapper returns `PlaceSuggestion("abc", "Colosseum", "Rome, Italy")`

### Requirement: PlaceDetailMapper
The data layer SHALL define a `PlaceDetailMapper` in `data/mapper/` that converts a `Place` to a `PlaceDetail`:
- `name` from `place.displayName ?: throw IllegalStateException`
- `latitude` from `place.location?.latitude ?: throw IllegalStateException`
- `longitude` from `place.location?.longitude ?: throw IllegalStateException`

The mapper SHALL throw `IllegalStateException` if `displayName` is null or `location` is null. The caller (`PlaceSearchRepositoryImpl`) is responsible for catching exceptions.

#### Scenario: Place mapped to detail
- **WHEN** a `Place` has `displayName = "Colosseum"` and `location = LatLng(41.8902, 12.4922)`
- **THEN** the mapper returns `PlaceDetail("Colosseum", 41.8902, 12.4922)`

#### Scenario: Null displayName throws
- **WHEN** a `Place` has `displayName = null`
- **THEN** the mapper throws an `IllegalStateException`

#### Scenario: Null location throws
- **WHEN** a `Place` has `location = null`
- **THEN** the mapper throws an `IllegalStateException`

### Requirement: SearchPlacesUseCase
`SearchPlacesUseCase` SHALL be an `@Inject` constructor class in `domain/usecase/` that accepts `PlaceSearchRepository` and exposes:
```
suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>>
```
It SHALL trim the query and delegate to `PlaceSearchRepository.getAutocompleteSuggestions`.

#### Scenario: Successful search
- **WHEN** `invoke` is called with `query = "Colo"` and the repository returns results
- **THEN** `Result.success` with the suggestions list is returned

#### Scenario: Query is trimmed
- **WHEN** `invoke` is called with `query = "  Rome  "`
- **THEN** the repository receives `query = "Rome"`

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

### Requirement: GetPlaceDetailUseCase
`GetPlaceDetailUseCase` SHALL be an `@Inject` constructor class in `domain/usecase/` that accepts `PlaceSearchRepository` and exposes:
```
suspend operator fun invoke(placeId: String): Result<PlaceDetail>
```
It SHALL delegate to `PlaceSearchRepository.getPlaceDetails`.

#### Scenario: Successful detail fetch
- **WHEN** `invoke` is called with `placeId = "abc"` and the repository returns a `PlaceDetail`
- **THEN** `Result.success(PlaceDetail)` is returned

#### Scenario: Repository failure propagated
- **WHEN** `invoke` is called and the repository returns `Result.failure`
- **THEN** `Result.failure` is propagated to the caller

### Requirement: PlacesModule Hilt wiring
A `PlacesModule` Hilt module SHALL be defined in `di/` installed in `SingletonComponent` that provides:
- `PlacesClient` as a `@Singleton` via `Places.createClient(context)`

#### Scenario: PlacesClient is injectable
- **WHEN** a class declares `@Inject constructor(placesClient: PlacesClient)`
- **THEN** Hilt provides the `PlacesClient` instance

### Requirement: PlaceSearchRepository Hilt binding
The existing `RepositoryModule` SHALL bind `PlaceSearchRepositoryImpl` to `PlaceSearchRepository` as a `@Singleton`.

#### Scenario: PlaceSearchRepository is injectable
- **WHEN** a class declares `@Inject constructor(repository: PlaceSearchRepository)`
- **THEN** Hilt provides the `PlaceSearchRepositoryImpl` instance

### Requirement: Places SDK initialisation
`VeniceApplication.onCreate()` SHALL call `Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)` to initialise the Places SDK before any `PlacesClient` usage.

#### Scenario: SDK initialised on app start
- **WHEN** the application starts
- **THEN** `Places.initializeWithNewPlacesApiEnabled` is called with the application context and API key

### Requirement: API key via BuildConfig
The API key SHALL be:
- Stored in `local.properties` as `MAPS_API_KEY=<key>` (this file is gitignored)
- Surfaced via `buildConfigField("String", "MAPS_API_KEY", ...)` in `app/build.gradle.kts`
- `buildFeatures.buildConfig = true` SHALL be enabled

#### Scenario: API key available at runtime
- **WHEN** the app accesses `BuildConfig.MAPS_API_KEY`
- **THEN** it returns the value from `local.properties`

### Requirement: Places SDK dependency
`gradle/libs.versions.toml` SHALL include the Google Places SDK and `kotlinx-coroutines-play-services` library entries. `app/build.gradle.kts` SHALL include the corresponding `implementation` dependencies.

#### Scenario: Places SDK available at compile time
- **WHEN** the project is built
- **THEN** `com.google.android.libraries.places:places` classes are available

#### Scenario: Coroutine bridge available at compile time
- **WHEN** the project is built
- **THEN** `kotlinx.coroutines.tasks.await` extension is available on `Task<T>`
