## Context

Venice is a road trip planning app where users create trips with stops (starting point, intermediate, destination). The app currently supports full stop CRUD, reordering, and departure tracking. There is no route information — users cannot see distances or travel times between stops.

The Google Routes API is the natural choice since the app already uses Google Places for stop search. The API key (`MAPS_API_KEY`) is already configured via `BuildConfig`. The project has no HTTP client dependency — Places uses the Google SDK directly. The Routes API replaces the legacy Directions API and provides the same leg-level distance/duration/polyline data via a POST endpoint with JSON body and field masking.

## Goals / Non-Goals

**Goals:**
- Calculate route (distance, duration, polyline) between all consecutive stops via a single Google Routes API call
- Persist leg data locally in Room so results survive screen rotation and app restarts
- Display distance/duration between consecutive stops on the trip detail screen
- Invalidate cached legs when stops are mutated (add, remove, reorder, edit)
- Provide clear loading and error feedback during API calls

**Non-Goals:**
- Map rendering of polylines (stored for future use but not rendered in this change)
- Live GPS-based route recalculation (Level 2 feature)
- Turn-by-turn navigation
- Alternative route display
- Per-leg recalculation (always recalculate the entire route)

## Decisions

### 1. HTTP Client: OkHttp + org.json

**Decision**: Use OkHttp for the HTTP client and Android's built-in `org.json` for JSON parsing.

**Alternatives considered**:
- `HttpURLConnection`: Zero dependencies but verbose boilerplate, poor error handling, no connection pooling.
- Retrofit: Overkill for a single endpoint. Adds Retrofit + converter dependency for one GET request.
- Ktor: Kotlin-native but heavy dependency for one endpoint.

**Rationale**: OkHttp is the industry standard for Android HTTP. It provides connection pooling, timeouts, and clean API. `org.json` is built into the Android SDK — no extra dependency needed for parsing the single response type. OkHttp is already a transitive dependency of the Google Places SDK. The Routes API uses POST with a JSON body and field masking via `X-Goog-FieldMask` header, which OkHttp handles cleanly.

**Isolation**: All OkHttp usage MUST be confined to `DirectionsApiService`. No other class may import or reference OkHttp types. This keeps the HTTP layer swappable — when additional endpoints are added, `DirectionsApiService` can be replaced with a Retrofit-based implementation without touching the repository, use cases, or DI beyond the network module. A TODO in `DirectionsApiService` and `NetworkModule` will document this intent.

### 2. Invalidation Strategy: Domain-Level via Use Cases

**Decision**: Inject `InvalidateRouteUseCase` into `SetStopUseCase`, `MoveStopUseCase`, `EditStopUseCase`, and `RemoveStopUseCase`. After a successful stop mutation, each use case calls `InvalidateRouteUseCase(tripId)` to delete all legs.

**Alternatives considered**:
- ViewModel-level: Simpler but fragile — any future ViewModel or code path that mutates stops could forget to invalidate.
- Database triggers: Not idiomatic Room. Triggers are invisible and hard to test.
- Room `InvalidationTracker`: Observes table changes but doesn't provide tripId context for targeted deletion.

**Rationale**: Domain-level enforcement ensures the invariant holds regardless of which ViewModel or entry point triggers the mutation. The existing TODOs in `EditStopUseCase` and `RemoveStopUseCase` already anticipate this pattern. Invalidation only runs on success — a failed mutation should not clear valid legs.

### 3. Single API Call with All Waypoints

**Decision**: Send origin, destination, and all intermediate stops in a single Routes API request.

**Rationale**: The Google Routes API returns one `legs[]` entry per consecutive stop pair. A single call is cheaper (billing), faster (one round-trip), and simpler (no stitching). The app already caps stops at 25 (origin + destination + 23 intermediates), within Google's 25-intermediate limit.

### 4. Leg Storage: Separate Room Table with FK

**Decision**: Store legs in a `legs` table with a foreign key to `trips` (ON DELETE CASCADE) and an index on `tripId`.

**Rationale**: Legs are a separate entity with their own lifecycle (created on calculation, deleted on invalidation or trip deletion). CASCADE ensures cleanup when trips are deleted. The index enables efficient query-by-trip.

### 5. Leg Display: Inline Summary Between Stops

**Decision**: Render a `LegSummary` composable between consecutive stops in the LazyColumn, showing formatted distance (km/m) and duration (hours/minutes).

**Rationale**: This provides immediate visual value without requiring a map view. Placing it between stops in the list creates a natural visual flow of the trip's structure.

### 6. StopRepository Needs tripId on Mutations

**Decision**: For `SetStopUseCase` and `MoveStopUseCase`, the `tripId` is already a parameter, so `InvalidateRouteUseCase(tripId)` can be called directly. For `EditStopUseCase`, the stop's `tripId` is obtained from the `Result<Stop>` return value. For `RemoveStopUseCase`, `tripId` is already a parameter.

**Rationale**: No additional repository queries needed — all mutating use cases either receive `tripId` or can extract it from the result.

## Risks / Trade-offs

- **Network dependency**: Route calculation requires internet. Mitigated by persisting results locally and showing a clear error on failure.
- **API quota**: Google Routes API has usage limits. Mitigated by lazy invalidation (delete legs, don't recalculate) and only calculating on explicit user action.
- **Invalidation granularity**: We delete all legs for a trip even if only one stop changed. This is simpler and safer than trying to determine which specific legs are affected, at the cost of requiring a full recalculation. Acceptable for the small scale (max 24 legs).
- **OkHttp threading**: Routes API calls run on `Dispatchers.IO` via the use case/repository coroutine context. OkHttp's synchronous `execute()` is used within the coroutine to avoid callback complexity.
- **Migration risk**: `MIGRATION_2_3` is additive (new table only) — low risk. Destructive migration fallback is not needed.
