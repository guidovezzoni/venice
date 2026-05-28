## Context

Starting point and destination editing already works through the `SetStopUseCase` → `upsertStartingPoint`/`upsertDestination` → `StopDao.update()` chain. These use an `upsertStop` helper that queries for an existing entity by type (order 0 or highest order), updates its fields, and persists via Room's `@Update`.

Intermediate stops lack this path. The existing `addIntermediateStop` only inserts new stops — there is no mechanism to update an existing intermediate stop by ID. The `StopSection` card for intermediate stops receives no `onSetStopClicked` callback, so tapping it is a no-op.

## Goals / Non-Goals

**Goals:**
- Enable tap-to-edit for intermediate stops, matching the existing UX for starting point and destination
- Preserve stop order and identity (ID, tripId, status) during edits
- Reuse the existing `SetStopDialog` composable for the edit dialog
- Leave a TODO marker for future leg invalidation (Epic 3)

**Non-Goals:**
- Modifying the existing starting point or destination edit flows (they already work)
- Implementing leg invalidation (Epic 3 scope)
- Inline editing or drag-to-edit UX — tap opens the same modal dialog
- Editing stop order through the edit dialog (order is managed by the existing reorder feature)

## Decisions

### 1. New `EditStopUseCase` vs. extending `SetStopUseCase`

**Decision:** Create a dedicated `EditStopUseCase`.

**Rationale:** `SetStopUseCase` dispatches by `StopType` and handles both insert and upsert semantics. Editing an intermediate stop by ID is a fundamentally different operation — we know the stop exists and just want to update its fields. Mixing this into `SetStopUseCase` would require adding an optional `stopId` parameter and conditional branching, blurring the contract. A separate use case keeps both clean and independently testable.

**Alternative considered:** Extend `SetStopUseCase` with an optional `stopId` parameter. Rejected because it would require the UI to know whether it's adding or editing, which is already separated at the intent level.

### 2. Repository method: `updateStop(stopId, ...)` with fetch-then-update

**Decision:** Add `updateStop` to `StopRepository` that takes a `stopId` and new field values.

**Rationale:** The existing `upsertStop` helper queries by type (starting point/destination), not by ID. For intermediate stops, multiple stops share the same type, so we need ID-based lookup. The new method fetches by ID, copies with new values (preserving `id`, `tripId`, `order`, `status`), and calls `StopDao.update()`. This mirrors the existing pattern but with ID-based resolution.

### 3. Reuse `SetStopDialog` composable

**Decision:** Reuse the existing `SetStopDialog` with a new dialog title string resource. Reuse the add-stop hint/error strings for the edit dialog fields.

**Rationale:** The dialog already supports `initialPlaceName`, `initialLatitude`, `initialLongitude` parameters for pre-population (used by starting point and destination editing). The only new string needed is the dialog title ("Edit stop"). This avoids duplicating the form validation logic and UI.

### 4. State tracking: `editingStop: Stop?` in `TripDetailUiState`

**Decision:** Store the full `Stop` object being edited in state, plus a `isEditStopDialogVisible` boolean.

**Rationale:** The dialog needs the stop's `id` (to identify which stop to update) and its current `placeName`, `latitude`, `longitude` (to pre-populate fields). Storing the full `Stop` provides all of this. The boolean guards dialog visibility, consistent with the existing pattern for other dialogs.

## Risks / Trade-offs

- **Risk: Stop deleted between tap and confirm** → The `updateStop` repository method will return `Result.failure` if `getStopById` returns null, and the ViewModel will show an error snackbar. Acceptable for a local-only app with no concurrent modification.
- **Trade-off: Separate use case adds a file** → One more file (`EditStopUseCase`) and its test, but keeps `SetStopUseCase` unchanged and the edit path independently testable.
- **Trade-off: Reusing add-stop hint strings for edit dialog** → The hints say "Place name", "Latitude", "Longitude" which are generic enough for both add and edit contexts. No user confusion expected.
