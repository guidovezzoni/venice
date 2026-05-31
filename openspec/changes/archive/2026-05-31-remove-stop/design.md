## Context

Venice's stop management supports creating, editing, and reordering stops within a trip. The missing CRUD operation is deletion. The codebase follows Clean Architecture with MVI in the UI layer. All stop mutations flow through `StopDao` → `StopRepositoryImpl` → use case → `TripDetailViewModel`. Existing transactional DAO methods (`shiftAndInsertStop`, `swapStopOrders`) and the dialog-based MVI pattern (`Clicked` → `Confirmed`/`Dismissed`) provide clear templates.

## Goals / Non-Goals

**Goals:**
- Allow deletion of any stop type (starting point, intermediate, destination)
- Maintain contiguous order indices after deletion via an atomic transaction
- Prevent accidental deletion with a confirmation dialog
- Support zero-stop trips

**Non-Goals:**
- Leg invalidation after stop removal (deferred to Epic 3 — route calculation)
- Undo/restore of deleted stops
- Batch deletion of multiple stops

## Decisions

### 1. Atomic delete-and-reorder in the DAO

The `deleteAndReorder` method combines deletion and order recalculation in a single `@Transaction`. This prevents inconsistent order gaps if the app crashes between operations.

**Alternative considered**: Performing delete and reorder as separate repository calls. Rejected because it requires manual transaction management at the repository level, and the existing pattern (`shiftAndInsertStop`, `swapStopOrders`) places transactional logic in the DAO.

### 2. Order recalculation via bulk UPDATE

After deleting the stop at order N, a single `UPDATE stops SET order = order - 1 WHERE tripId = :tripId AND order > :fromOrder` closes the gap. No application-level loop is needed.

**Alternative considered**: Loading all stops, recalculating orders in Kotlin, then batch-updating. Rejected as unnecessarily complex — the SQL approach is atomic, efficient, and matches the existing `incrementOrderFrom` pattern.

### 3. No special-case logic for starting point / destination promotion

The story specifies promotion rules (e.g., if starting point is removed, next stop becomes order 0). The `decrementOrderAbove` bulk UPDATE handles this implicitly — when order 0 is deleted, all remaining orders decrement by 1, so the former order-1 stop becomes order 0 (the new starting point). No explicit promotion code is needed.

### 4. Simple AlertDialog for confirmation

The removal confirmation uses a plain `AlertDialog` with the stop's place name — not the `SetStopDialog` used for create/edit. Deletion needs no input fields, so a simpler dialog is appropriate.

### 5. Delete button on all filled stop cards

The `onDelete` lambda is added to `StopSection` alongside the existing `onMoveUp`/`onMoveDown` lambdas. When non-null and the stop is filled, a delete `IconButton` renders at the end of the action row. All filled stop sections (starting point, intermediates, destination) receive this lambda.

## Risks / Trade-offs

- **Zero-stop trips**: After removing all stops, the trip detail screen shows only empty-state buttons. This is acceptable and consistent with the initial trip creation state. → No mitigation needed.
- **No undo**: Deletion is permanent. → Mitigated by the confirmation dialog.
- **Leg invalidation deferred**: Removing a stop with calculated legs will leave stale leg data until Epic 3. → Mitigated by a TODO comment in `RemoveStopUseCase`, matching the pattern in `EditStopUseCase`.
