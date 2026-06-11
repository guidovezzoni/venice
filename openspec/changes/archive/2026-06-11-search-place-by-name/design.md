## Context

Venice currently requires users to manually enter latitude and longitude when adding or editing stops. The existing `SetStopDialog` composable uses local state (`rememberSaveable`) for form fields and is shared across 4 dialog use cases (starting point, destination, add stop, edit stop) via parameterised string resources and callbacks.

The Google Places SDK (New) for Android provides an autocomplete API that returns place predictions as the user types, and a place details API that returns coordinates for a selected prediction. The SDK uses session tokens to group prediction + selection into a single billing event (~$0.017).

## Goals / Non-Goals

**Goals:**
- Users can search for places by name and auto-populate stop coordinates
- Clean Architecture compliance: domain layer remains SDK-agnostic
- Session-token billing optimisation handled transparently
- Manual coordinate entry preserved as fallback
- Existing dialog functionality and test coverage unaffected

**Non-Goals:**
- Map-based place selection (future story)
- Reverse geocoding (coordinates → place name)
- Place photos, reviews, or opening hours
- Offline place search / caching
- Location-biased search results (no current user location access)

## Decisions

### 1. Session token managed inside the data layer

**Decision:** `PlaceSearchRepositoryImpl` owns the `AutocompleteSessionToken` lifecycle internally. The domain API has no token parameter.

**Rationale:** The session token is a billing optimisation detail with no domain significance. Exposing it as `Any` at the domain boundary (as the user story initially suggested) is type-unsafe and leaks a concern that belongs entirely to the data layer.

**Alternatives considered:**
- `sessionToken: Any` in domain interfaces — rejected: type-unsafe, testability issues, domain layer shouldn't know about billing
- Domain wrapper type — rejected: extra abstraction for something only the data layer uses

**Implementation:** The repository auto-creates a token on the first `getAutocompleteSuggestions` call if none exists. The token is consumed (cleared) after `getPlaceDetails`. A `resetSession()` method allows the ViewModel to explicitly end an abandoned session on dialog dismiss.

### 2. Debounce via Job cancellation

**Decision:** Use a cancellable `Job` variable in `TripDetailViewModel` for search debouncing (300 ms).

**Rationale:** Matches the existing imperative coroutine style used throughout the ViewModel. A `MutableStateFlow + debounce + collectLatest` pipeline would be more reactive but introduces a different pattern into a codebase that consistently uses `viewModelScope.launch`.

**Alternatives considered:**
- `MutableStateFlow.debounce().collectLatest()` — rejected: different pattern from existing code, harder to test the initial empty emission

### 3. Hybrid state: ViewModel search state + composable form state

**Decision:** Search-related state (`placeSuggestions`, `isSearchingPlaces`, `searchError`, `selectedPlaceDetail`) lives in `TripDetailUiState`. Form fields (`placeName`, `latitudeText`, `longitudeText`) remain as local `rememberSaveable` state in `SetStopDialog`.

**Rationale:** Hoisting all form fields to the ViewModel would require a major refactor of the dialog and all 4 call sites, with every keystroke flowing through the ViewModel. The hybrid approach is minimal: the composable notifies the ViewModel of text changes via a callback, and the ViewModel writes back selected coordinates via a `selectedPlaceDetail` field that the composable observes with `LaunchedEffect`.

**Alternatives considered:**
- Hoist all form state to ViewModel — rejected: major refactor, every keystroke goes through state flow, changes all 4 dialog call sites significantly
- Keep everything local — rejected: search logic requires async use case calls that must live in the ViewModel

### 4. PlacesClient initialisation split

**Decision:** SDK initialisation in `VeniceApplication.onCreate()`, `PlacesClient` instance provided via Hilt `PlacesModule`.

**Rationale:** `Places.initializeWithNewPlacesApiEnabled()` is a global one-time call that must happen before any client creation. The `PlacesClient` instance is then injectable and testable via DI.

### 5. Coroutine bridge for Play Services Tasks

**Decision:** Add `kotlinx-coroutines-play-services` dependency for the `Task<T>.await()` extension.

**Rationale:** The Places SDK returns `Task<T>` objects. Without this library, we'd need manual `suspendCancellableCoroutine` wrappers — boilerplate that this well-maintained JetBrains library eliminates.

### 6. Read-only coordinates after autocomplete selection

**Decision:** Track via a local `coordinatesFromAutocomplete` boolean in the composable. Set to `true` when `selectedPlaceDetail` arrives, reset to `false` when the user manually changes the place name text to something different from the selected name.

**Rationale:** This is purely a UI presentation concern — the ViewModel doesn't need to know whether coordinates are editable. Keeping it local avoids adding transient UI state to `TripDetailUiState`.

## Risks / Trade-offs

- **[Risk] API key exposure in APK** → The key is in `BuildConfig` (compiled into the APK). Mitigation: restrict the key in Google Cloud Console to the app's package name and SHA-1 signing certificate. This is standard practice for mobile API keys.

- **[Risk] Places SDK not available (Play Services missing)** → Some devices lack Google Play Services. Mitigation: manual entry fallback is always available. The search simply won't return results. Future: could check `Places.isInitialized()` and hide the search affordance.

- **[Risk] Network errors during search** → Mitigation: inline error message ("Search unavailable"), search state cleared, user can still enter data manually.

- **[Trade-off] Hybrid state model adds complexity** → The composable observes both local state and ViewModel state. This is less pure than full MVI but avoids a large refactor. Acceptable given the existing local-state pattern.

- **[Trade-off] No location bias** → Search results are not biased to the user's current location. This may return less relevant results for common place names. Acceptable for v1; location-biased search can be added later with device location permission.
