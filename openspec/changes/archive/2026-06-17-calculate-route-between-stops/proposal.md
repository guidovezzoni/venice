## Why

Users need to know the distance and travel time between consecutive stops in their road trip. Without route calculation, the app is a list of places with no trip-planning value. This is the first feature in Epic 3 (Route Calculation) and unlocks future features like map rendering and live navigation.

## What Changes

- Add a "Calculate route" button to the trip detail screen, visible when at least 2 stops exist.
- Call the Google Routes API with all stops in a single POST request (origin, destination, intermediates).
- Show a loading indicator and disable the button during the API call (minimum 500ms duration).
- Persist distance (metres), duration (seconds), and encoded polyline per leg in a new Room `legs` table.
- Display a distance/duration summary between each pair of consecutive stops on the trip detail screen.
- Show an inline error message on API failure (network error, invalid stops, quota exceeded).
- Replace previously stored legs when the route is recalculated.
- Invalidate (delete) all legs for a trip when stops are added, removed, reordered, or edited.

## Capabilities

### New Capabilities
- `route-calculation`: Covers the Google Routes API integration, route repository, leg persistence, calculation/invalidation use cases, and the Leg domain model.
- `route-display`: Covers the UI changes for showing the "Calculate route" button, loading/error states, and leg distance/duration summaries between stops.

### Modified Capabilities
- `trip-detail`: Add new UiState fields (`legs`, `isCalculatingRoute`, `routeError`), new UiIntent (`OnCalculateRouteClicked`), ViewModel wiring for route calculation and leg observation.
- `stop-management`: Each stop-mutating use case (`SetStopUseCase`, `MoveStopUseCase`, `EditStopUseCase`, `RemoveStopUseCase`) must invalidate legs after successful mutation.

## Impact

- **New dependency**: OkHttp for HTTP client (used to call the Routes API REST endpoint). JSON parsed via Android's built-in `org.json`.
- **Database migration**: `MIGRATION_2_3` adds the `legs` table with FK to `trips` (ON DELETE CASCADE).
- **DI changes**: New `NetworkModule` (OkHttpClient singleton), new `RouteRepository` binding, new `LegDao` provider. Existing `DatabaseModule` updated with new migration.
- **Domain layer coupling**: `InvalidateRouteUseCase` injected into four existing stop-mutating use cases for domain-level invalidation enforcement.
- **Build config**: `MAPS_API_KEY` already configured — no changes needed.
