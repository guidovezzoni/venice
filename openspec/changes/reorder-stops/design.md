## Context

The app supports up to 25 stops per trip: a starting point (order 0), intermediate stops, and a destination (highest order). Stops are persisted in the `stops` table with an `order` column and observed reactively via `Flow`. Currently there is no way to change the order of intermediate stops after insertion — they are fixed in the position they were added.

The user story specifies move-up/move-down buttons (not drag-and-drop) because Compose `LazyColumn` does not natively support drag reordering without a third-party library or custom gesture code. Buttons are simple, accessible, and reliable.

## Goals / Non-Goals

**Goals:**
- Allow users to reorder intermediate stops via move-up/move-down buttons.
- Swap the `order` field of two adjacent stops atomically in a transaction.
- Keep the starting point and destination pinned at their positions.
- Update the UI reactively through the existing `Flow`-based observation.

**Non-Goals:**
- Drag-and-drop reordering — may be introduced as a future enhancement.
- Deleting or editing intermediate stops — separate story.
- Route recalculation after reorder — deferred to story 3.1.1.
- Reordering the starting point or destination.

## Decisions

### D1: Swap strategy (two targeted UPDATEs vs. full re-index)
**Decision:** Swap the `order` values of exactly two stops using two `UPDATE` statements inside a `@Transaction`. The DAO exposes `getStopByTripIdAndOrder` to look up each neighbour and `updateStopOrder` to set the new order.

**Alternatives considered:**
- *Full re-index of all stop orders*: Simpler conceptually but touches more rows than necessary. With a 25-stop cap the performance difference is negligible, but targeted swaps are semantically clearer.
- *Single raw SQL swap query*: Possible but harder to read and maintain; two explicit updates in a transaction are equally atomic and more transparent.

**Rationale:** Two updates in a transaction are O(1) in SQLite, easy to understand, and match the existing DAO pattern of fine-grained queries.

### D2: Validation in MoveStopUseCase
**Decision:** `MoveStopUseCase` accepts `tripId`, `fromOrder`, and direction (derived from the intent). It validates: (a) the target order is not the starting point (order 0) or destination (max order), (b) a stop exists at both the source and target positions. On validation failure, it returns `Result.failure`.

**Rationale:** Defence in depth — the UI hides buttons when a move is invalid, but the use case guards against edge cases (e.g., stale UI state after a concurrent modification).

### D3: Move-up/move-down buttons over drag-and-drop
**Decision:** Use `IconButton` with `Icons.Filled.KeyboardArrowUp` / `Icons.Filled.KeyboardArrowDown` placed inside each intermediate stop's `StopSection` card.

**Rationale:** As stated in the user story, Compose has no native `LazyColumn` drag-reorder support. Buttons are accessible (screen reader content descriptions), reliable, and require no third-party dependencies.

### D4: Button visibility rules
**Decision:** Move buttons are only shown for intermediate stops when there are 2 or more intermediates. The first intermediate hides move-up; the last intermediate hides move-down. Starting point and destination never show buttons.

**Rationale:** Prevents no-op moves and keeps the UI clean when reordering is impossible.

## Risks / Trade-offs

- **Stale UI triggering invalid swap** → Mitigated by validation in `MoveStopUseCase` and the transactional swap. If the target position is no longer valid, the use case returns failure and a snackbar is shown.
- **Two UPDATE statements without temporary order** → If two stops share an order momentarily during the transaction, uniqueness is not enforced by a DB constraint (there is no UNIQUE on `(tripId, order)`). This is safe because the transaction is atomic from the perspective of any reader. Mitigated by the single-writer guarantee of SQLite.
- **No migration needed** → The `stops` table already supports arbitrary order values. No schema changes required.
