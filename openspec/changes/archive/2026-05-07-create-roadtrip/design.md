## Context

Venice is a greenfield Android roadtrip planning app built with Jetpack Compose and Material 3. The codebase currently contains only the scaffolded `MainActivity` and theme files — no data layer, no screens, and no architecture beyond the default template.

This change introduces the first user-facing feature (trip creation) and, with it, the foundational architecture: Room for persistence, clean architecture layering (data/domain/UI), and the MVI pattern for UI state management. Every subsequent feature will build on these patterns, so getting the foundation right matters.

The package is `com.guidovezzoni.venice`, targeting SDK 36 with min SDK 24.

## Goals / Non-Goals

**Goals:**
- Establish Room database with a `trips` table and migration-safe setup.
- Implement end-to-end trip creation: FAB → dialog → validation → persistence → navigation.
- Set up the MVI pattern (UiState/UiIntent/UiEffect) as the standard for all future ViewModels.
- Keep the `TripRepository` interface in the domain layer so Android Auto can consume it in the future without touching the data layer.
- Ensure the architecture supports offline-first operation (all local, no network).
- Use UUID-based IDs from the start to avoid a breaking migration when remote sync is introduced.

**Non-Goals:**
- Trip editing, deletion, or detail screen content — only navigation to a placeholder detail screen.
- Stops, legs, route calculation, or map integration.
- Cloud sync, user authentication, or multi-device support (though the ID strategy is sync-ready).
- Android Auto integration (the interface is prepared, but no Car App Library code).
- Hilt modules beyond what this feature requires (e.g. network, analytics).

## Decisions

### 1. Hilt for dependency injection

**Decision**: Use Hilt (Dagger) for dependency injection from the start.

**Rationale**: The app will grow beyond this single feature, and retrofitting Hilt later means touching every ViewModel, repository, and database provider. Setting it up now establishes the pattern for all future features and avoids a dedicated migration change. The annotation processing cost is negligible for a project of this size.

**Alternative considered**: Manual DI — lighter setup for a single feature, but doesn't scale and would need to be replaced soon.

### 2. Room for persistence

**Decision**: Use Room as the local database.

**Rationale**: Room is the recommended Android persistence library, provides compile-time SQL verification, integrates natively with Kotlin coroutines and Flow, and handles schema migrations. The user story explicitly recommends it.

**Alternative considered**: DataStore — suitable for key-value preferences but not for relational data with queries.

### 3. MVI over MVVM

**Decision**: Use MVI (Model-View-Intent) with explicit `UiState`, `UiIntent`, and `UiEffect` types per the project's Android guidelines.

**Rationale**: MVI enforces unidirectional data flow, makes state transitions explicit, and simplifies testing (dispatch intent → assert state). The project guidelines mandate this pattern.

**Alternative considered**: MVVM with LiveData — simpler but less structured; the guidelines specifically require MVI.

### 4. UUID primary key

**Decision**: Use `String` (UUID v4) as the primary key for `TripEntity.id`, generated client-side via `java.util.UUID.randomUUID().toString()`.

**Rationale**: Trip data will eventually be synchronised with a remote backend for multi-device access and trip sharing. Auto-generated `Long` IDs would collide across devices and require a painful migration later. Using UUIDs from day one avoids that migration entirely at a negligible storage cost.

**Alternative considered**: Auto-generated `Long` — simpler for local-only, but switching to UUID later means a database migration on every user's device plus updating every foreign key reference across all tables added in the interim.

### 5. Navigation Compose for screen transitions

**Decision**: Use Jetpack Navigation Compose with string routes for navigating from Trip List to Trip Detail.

**Rationale**: Standard Compose navigation solution. One-shot navigation pattern via `navigateToTripId` in UiEffect avoids repeated navigation on recomposition.

**Alternative considered**: Type-safe navigation with sealed classes — a good evolution but premature for two screens.

### 6. Epoch millis for timestamps

**Decision**: Store `createdAt` and `updatedAt` as `Long` (epoch milliseconds) rather than ISO strings or `Instant`.

**Rationale**: Room handles `Long` natively without type converters. `System.currentTimeMillis()` is straightforward. If timezone-aware display is needed later, conversion is trivial.

## Risks / Trade-offs

- **Hilt adds build complexity** → Mitigated by using KSP (not KAPT) for annotation processing, which is faster and better supported.
- **No database migration strategy yet** → The database is new, so version 1 has no migration. Future schema changes will require `Migration` objects. Mitigated by starting with a versioned database from day one.
- **Placeholder Trip Detail screen** → Users will navigate to an empty screen after creation. Acceptable for this story's scope; the detail screen is a separate user story.
- **No input debouncing or duplicate prevention** → A user could theoretically tap "Create" twice quickly. Mitigated by disabling the confirm button during creation (via `isLoading` state).
