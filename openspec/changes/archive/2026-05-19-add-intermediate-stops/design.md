## Context

The app currently supports two fixed stops per trip: a starting point (order 0) and a destination (highest order). The existing `stops` table, `StopDao`, `StopRepository`, and `TripDetailScreen` are built around this two-stop model. The user story requires extending this to support up to 25 total stops (start + 23 intermediate + destination), using the same `SetStopDialog` pattern already in place.

The `stops` table schema already has an `order` column with no upper-bound constraint, so no database migration is needed. The reactive `observeByTripId` Flow already returns all stops ordered by `order`, so intermediate stops will flow through the existing observation pipeline automatically.

## Goals / Non-Goals

**Goals:**
- Allow users to add intermediate stops between starting point and destination.
- Maintain correct stop ordering when inserting at arbitrary positions.
- Enforce the 25-stop limit (Google Directions API waypoint constraint).
- Reuse existing UI patterns (`SetStopDialog`, `StopSection`) for consistency.

**Non-Goals:**
- Reordering existing stops (drag-and-drop) — separate story.
- Deleting or editing intermediate stops — separate story.
- Route recalculation after adding stops — deferred to story 3.1.1.
- Place search / autocomplete — current `SetStopDialog` uses manual entry.

## Decisions

### D1: Order-shifting strategy for insertion
**Decision:** Use a single SQL `UPDATE` to increment `order` by 1 for all stops at or above the insertion position, then insert the new stop at the target order. Both operations are wrapped in a `@Transaction`.

**Alternatives considered:**
- *Sparse ordering (e.g., order by 10s)*: Reduces shifts but adds complexity for order normalisation; unnecessary with a 25-stop cap.
- *Re-index all orders on every insert*: Simpler logic but touches more rows; less efficient for append-near-end cases.

**Rationale:** With a maximum of 25 stops, the shift operation is trivially fast. A single `UPDATE ... SET order = order + 1 WHERE tripId = ? AND order >= ?` is atomic and simple.

### D2: Destination order tracking
**Decision:** The destination is always the stop with the maximum `order` value. When inserting an intermediate stop, the destination's order is also shifted up (it is at or above any valid insertion point). This preserves the existing invariant that `getDestination` returns the stop with the highest order.

**Rationale:** This avoids introducing a separate `stopType` column or special-casing the destination during inserts. The existing `getDestination` query (`MAX(order) WHERE order > 0`) continues to work correctly.

### D3: Validation split between use case and UI
**Decision:** `AddIntermediateStopUseCase` validates: non-blank place name, coordinates in range, order within valid range (> 0, < destination order), and total stop count < 25. The UI hides the "Add Stop" button when the limit is reached, providing a first line of defence.

**Rationale:** Defence in depth — the use case catches edge cases (e.g., race conditions with concurrent inserts) while the UI provides immediate feedback.

### D4: New stop insertion position
**Decision:** New intermediate stops are inserted immediately before the destination (at the destination's current order). The user does not choose an arbitrary position in this story — the "Add Stop" button always adds at the end of the intermediate list.

**Rationale:** Simplifies the UI (single button placement, no drag-target indicators). Arbitrary reordering is a separate story.

### D5: Reuse SetStopDialog for intermediate stops
**Decision:** The existing `SetStopDialog` composable is reused as-is for adding intermediate stops. No new dialog composable is needed.

**Rationale:** The dialog already handles place name, latitude, and longitude input with validation. Reusing it maintains UI consistency and avoids duplication.

## Risks / Trade-offs

- **Order shifting under concurrent access** → Mitigated by wrapping shift + insert in a `@Transaction`. Room enforces single-writer on SQLite, so concurrent writes are serialised.
- **Destination order assumption** → The `getDestination` query relies on `MAX(order)`. If a bug inserts a stop with an order above the destination, the invariant breaks. Mitigated by validating insertion order in the use case.
- **No migration needed** → Low risk. The `stops` table already supports arbitrary order values. Verified by inspecting the existing schema.
