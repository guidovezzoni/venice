## Context

The app currently supports creating and listing road trips but has no concept of route data. A trip's detail screen (`TripDetailScreen`) exists as a placeholder. This change introduces the `Stop` entity — the building block for routes — starting with the departure point (`order = 0`). The full place-search UX (story 2.1.1) is not yet available, so a stub dialog collects name and coordinates manually.

## Goals / Non-Goals

**Goals:**
- Establish the `Stop` data model and persistence layer that all future stop/route stories will build on.
- Allow the user to set and change a starting point from the trip detail screen.
- Validate coordinate input at the UI boundary before persisting.
- Maintain backwards compatibility with existing databases via a Room migration.

**Non-Goals:**
- Place search / autocomplete (deferred to story 2.1.1).
- Intermediate or destination stops (future stop management stories).
- Route calculation or map rendering.
- Cloud sync or backup of stop data.

## Decisions

### 1. Stop as a generic entity with order-based role

The starting point is stored as a regular `Stop` with `order = 0` rather than a dedicated `StartingPoint` entity.

**Rationale:** All stops share the same schema (name, coordinates, order, status). Using a single table avoids schema duplication and makes future stories (add intermediate stops, reorder) straightforward. The starting point is simply the stop where `order = 0`.

**Alternative considered:** Separate `StartingPoint` table — rejected because it would require migration/merge when intermediate stops are added later.

### 2. Upsert semantics for the starting point

`StopRepository.upsertStartingPoint` queries for an existing `order = 0` stop and either updates it in place or inserts a new one.

**Rationale:** The user story requires "change starting point" to replace, not append. Preserving the original row's `id` on update keeps any future foreign-key references (e.g. route legs) stable.

**Alternative considered:** Delete + insert — rejected because it would change the stop's UUID, potentially breaking references in future features.

### 3. Stub dialog instead of place search

A simple `AlertDialog` with three `OutlinedTextField` inputs (place name, latitude, longitude) stands in for the full place-search flow.

**Rationale:** This unblocks the entire data layer and MVI contract without waiting for the place-search epic. The dialog will be replaced by story 2.1.1.

### 4. Validation at the ViewModel layer

Coordinate and name validation happens in the ViewModel before calling the use case, with inline errors shown in the dialog.

**Rationale:** Keeps the use case and repository clean (they trust validated input from above). The ViewModel is the natural MVI boundary where user input is processed.

### 5. Database migration v1 → v2

A manual `Migration(1, 2)` creates the `stops` table and its index, rather than using `fallbackToDestructiveMigration`.

**Rationale:** Users already have trips stored; destructive migration would lose their data.

## Risks / Trade-offs

- **[Stub dialog UX]** → The manual lat/lng entry is awkward for real use. Mitigated by marking this as a temporary stub; story 2.1.1 replaces it with place search.
- **[Single-table stop model]** → If future stop types need radically different fields, the schema may need extension. Mitigated by keeping the current schema minimal and extensible (new nullable columns or a type discriminator can be added later).
- **[No network calls]** → All data is local-only. If cloud sync is added later, stop coordinates are PII under GDPR and will need encryption and consent handling. Mitigated by documenting this in the user story's non-functional section.
