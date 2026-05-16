## Context

The trip detail screen currently supports setting a starting point (a `Stop` with `order = 0`). The existing `Stop` data model, `StopEntity`, `StopDao`, and `StopRepository` were introduced in story 1.2.1. The destination is the natural next stop type, reusing the same data model with `order > 0`.

No schema migration is needed — the `stops` table already supports arbitrary `order` values. The destination is identified by convention: the stop with the highest `order` where `order > 0` (currently always `order = 1`).

## Goals / Non-Goals

**Goals:**
- Allow the user to set and change a destination for a trip
- Reuse the existing `Stop` infrastructure (model, entity, DAO, repository) with minimal additions
- Follow the same MVI patterns established by the starting point feature
- Maintain full test coverage for new logic

**Non-Goals:**
- Introducing a `StopType` enum (deferred to story 1.2.3 when intermediate stops are added)
- Place search integration (deferred to story 2.1.1; a stub dialog is used)
- Route calculation between starting point and destination
- Supporting multiple destinations

## Decisions

### Decision 1: Identify destination by order convention, not by type field

The destination is the stop with `max(order) WHERE order > 0`. No `StopType` enum is introduced.

**Rationale:** Only two stop types exist (starting point and destination). Adding a type field now would require a database migration that provides no immediate value. Story 1.2.3 will introduce `StopType` when intermediate stops make the convention insufficient.

**Alternative considered:** Add `StopType` now to avoid future migration. Rejected because it adds unnecessary complexity and migration cost for a distinction that `order` already encodes.

### Decision 2: Dedicated DAO query for destination retrieval

Add `getDestination(tripId)` as a suspend query rather than filtering the observed list in the ViewModel.

**Rationale:** The `upsertDestination` repository method needs to check for an existing destination before deciding to insert or update. A dedicated DAO query is more efficient and clearer than loading all stops and filtering. The ViewModel still extracts the destination from the existing `observeStopsForTrip` flow for reactive observation.

### Decision 3: Separate composable files for DestinationSection and SetDestinationDialog

Follow the same file-per-component pattern used for `StartingPointSection` and `SetStartingPointDialog`.

**Rationale:** Keeps composables focused, testable via previews, and consistent with the established project structure. Both files live under `ui/screens/tripdetail/`.

### Decision 4: Reuse existing `ShowError` effect for destination errors

No new `UiEffect` subclass is needed.

**Rationale:** The existing `ShowError(message: String)` effect in `TripDetailUiEffect` is generic enough to handle any error message, including destination-related ones. Adding a separate effect would be unnecessary duplication.

### Decision 5: Consolidate stop UI composables and repository internals

`StartingPointSection` and `DestinationSection` are merged into a single `StopSection` composable, parameterised by icon and string resource IDs. `SetStartingPointDialog` and `SetDestinationDialog` are merged into `SetStopDialog`, parameterised by string resource IDs. `StopRepositoryImpl` extracts a private `upsertStop` helper used by both public methods.

**Rationale:** The two stop types use identical layout, validation, and upsert logic — only string resources, icon, and order value differ. Consolidating now prevents the duplication from growing when intermediate stops arrive in story 1.2.3.

**Alternative considered:** Keep separate composables for each stop type. Rejected because the composables are structurally identical and differ only in parameterisable resources.

## Risks / Trade-offs

- **[Risk] Order convention breaks with intermediate stops** → Mitigated by design note in user story: story 1.2.3 will introduce `StopType` and migrate. The convention is intentionally temporary.
- **[Risk] Stub dialog is not the final UX** → Accepted: the stub is a known placeholder until place search (2.1.1) is implemented. The composable API (`onConfirm` callback with name + lat + lng) will remain stable.
- **[Trade-off] No input validation in the use case layer** → Validation is handled in the dialog composable (inline field errors). The use case only trims the place name. This is consistent with the starting point implementation and keeps the domain layer thin.
