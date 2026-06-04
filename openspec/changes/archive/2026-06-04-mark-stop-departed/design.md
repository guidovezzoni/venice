## Context

The Venice app currently persists a `StopStatus` enum (`PENDING`/`VISITED`) on every stop but the UI makes no use of it — all stops render identically. The trip detail screen shows stops in order but provides no way to track progress. The reactive observation pipeline (`StopDao.observeByTripId` → Flow → ViewModel → Compose) is already in place, so any status change in the DB will automatically propagate to the UI.

## Goals / Non-Goals

**Goals:**
- Allow the user to mark the current stop as departed (PENDING → VISITED)
- Allow the user to undo the last departure (VISITED → PENDING)
- Visually differentiate departed, current, and upcoming stops
- Derive "current" from persisted data (first PENDING by order) — no additional stored state

**Non-Goals:**
- GPS-based automatic departure detection (Level 2, future)
- Trip completion indicator or special "all departed" UI
- Restricting edit/reorder/remove on departed stops
- Batch mark/unmark operations
- Notification or sound feedback on departure

## Decisions

### 1. Targeted `updateStopStatus` query vs. full entity update

**Decision:** Add a dedicated `@Query("UPDATE stops SET status = :status WHERE id = :stopId")` in `StopDao`.

**Rationale:** The existing `update(stop: StopEntity)` requires fetching the full entity first, then copying it with the new status, then writing it back. A targeted query is a single DB round-trip and avoids accidental field overwrites. It also makes the intent explicit at the DAO level.

**Alternative considered:** Reusing `StopDao.update()` — rejected because it couples the status update to full entity reads, is less efficient, and obscures intent.

### 2. Use case validates "current stop" constraint

**Decision:** `MarkStopDepartedUseCase` queries all stops for the trip, determines the current stop (first PENDING by order), and validates that the requested `stopId` matches it. If not, it returns `Result.failure`.

**Rationale:** The UI will only show the button on the current stop, but the use case must still enforce the invariant in case of race conditions or future API callers. The validation requires knowing the trip's stops, so the use case accepts both `tripId` and `stopId`.

**Alternative considered:** Validating only in the UI (simpler use case) — rejected because domain invariants should be enforced in the domain layer.

### 3. Undo targets the last departed stop by order

**Decision:** `UndoMarkStopDepartedUseCase` finds the VISITED stop with the highest order in the trip and reverts it to PENDING.

**Rationale:** This guarantees undo always reverts in reverse chronological order of the trip route. The UI shows the undo button only on this specific stop for consistency.

### 4. UI visual differentiation approach

**Decision:** Pass the stop's computed display state (departed/current/upcoming) from the ViewModel to the composable and style accordingly:
- **Departed:** Muted icon tint (`onSurfaceVariant`), reduced card opacity/alpha
- **Current:** Primary color highlight, "Mark as departed" action button
- **Upcoming:** Default styling (as today)

**Rationale:** Minimal visual change for departed stops keeps the UI readable. Highlighting the current stop draws attention to the actionable item. The ViewModel computes the display state so composables stay stateless.

### 5. Intent carries `stopId` only

**Decision:** `OnMarkStopDepartedClicked(stopId: String)` and `OnUndoMarkStopDepartedClicked(stopId: String)` include only the stop ID.

**Rationale:** The ViewModel already knows the `tripId` from `SavedStateHandle`. Keeping intents minimal follows the established pattern (see `OnRemoveStopClicked`, `OnEditStopClicked`).

## Risks / Trade-offs

- **Race condition:** If two rapid taps fire before the first completes, the second may try to depart an already-departed stop. → Mitigated by the use case validation returning failure; the ViewModel can ignore or show an error.
- **Undo granularity:** Only the most recent departure can be undone (single-level undo). → Acceptable for Level 1; multi-level undo is a future enhancement.
- **Visual debt:** The `StopSection` composable is growing in complexity with conditional buttons. → Acceptable; further decomposition can happen in a refactoring story if needed.
