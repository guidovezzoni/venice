## Why

Users currently must manually enter latitude and longitude coordinates when adding or editing stops — an error-prone workflow that requires external tools to look up coordinates. Integrating Google Places Autocomplete lets users search by place name and auto-populate coordinates, making stop creation fast and reliable.

## What Changes

- Add Google Places SDK (New) dependency and initialise it on app start with an API key from `BuildConfig`
- Add `kotlinx-coroutines-play-services` dependency for `Task<T>.await()` coroutine bridge
- Introduce domain models `PlaceSuggestion` and `PlaceDetail` for autocomplete results
- Introduce `PlaceSearchRepository` (interface + implementation) with session-token management hidden inside the data layer
- Introduce `SearchPlacesUseCase` and `GetPlaceDetailUseCase`
- Extend `SetStopDialog` with a suggestion list below the place name field, a search loading indicator, and read-only coordinate fields after selection
- Extend `TripDetailUiState` with search-related fields (`placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail`)
- Extend `TripDetailUiIntent` with `OnSearchQueryChanged` and `OnSuggestionSelected`
- Add debounced search logic and place selection handling to `TripDetailViewModel`
- Add Hilt module for `PlacesClient` provisioning
- Preserve manual coordinate entry as a fallback when autocomplete is not used

## Capabilities

### New Capabilities
- `place-search`: Domain models, repository, and use cases for searching places via Google Places Autocomplete and fetching place details (coordinates)

### Modified Capabilities
- `destination-ui`: SetStopDialog gains autocomplete suggestion list, search loading indicator, and read-only coordinate fields after place selection
- `trip-detail`: TripDetailUiState, TripDetailUiIntent, and TripDetailViewModel extended with place search state, intents, and debounced search logic

## Impact

- **Dependencies**: `com.google.android.libraries.places:places` (new), `kotlinx-coroutines-play-services` (new)
- **Build config**: `buildFeatures.buildConfig = true` added, `MAPS_API_KEY` surfaced via `buildConfigField`
- **Application class**: `VeniceApplication.onCreate()` gains Places SDK initialisation
- **DI**: New `PlacesModule` provides `PlacesClient`; `RepositoryModule` gains `PlaceSearchRepository` binding
- **UI**: All 4 stop dialog call sites in `TripDetailScreen` pass new search-related parameters
- **API key**: Stored in `local.properties` (gitignored), must be obtained from Google Cloud Console with Places API (New) enabled
- **Billing**: ~$0.017 per autocomplete session (well within $200/month free tier)
