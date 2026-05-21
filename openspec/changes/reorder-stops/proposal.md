## Why

Users need to adjust their journey sequence after adding intermediate stops. Currently, stops are fixed in insertion order — if a user adds Florence after Rome but later realises they should visit Florence first, there is no way to correct this without removing and re-adding stops. Reordering is a natural expectation for trip planning.

## What Changes

- Each intermediate stop displays move-up and move-down icon buttons to shift its position relative to neighbouring intermediate stops.
- Tapping move-up/move-down swaps the `order` field of the moved stop with its neighbour atomically in a Room `@Transaction`.
- The starting point (order 0) and destination (highest order) remain pinned and cannot be reordered.
- Reorder buttons are hidden when there are fewer than 2 intermediate stops (nothing to swap with).
- The stop list updates immediately via the existing `Flow`-based observation.

## Capabilities

### New Capabilities

_None — reordering is an extension of existing stop management, not a new capability._

### Modified Capabilities
- `stop-management`: Add `swapStopOrder(tripId, fromOrder, toOrder)` repository method and a new `MoveStopUseCase` that validates the move and delegates the swap.
- `stop-persistence`: Add DAO queries `getStopByTripIdAndOrder` (lookup by position) and `updateStopOrder` (set new order for a stop).
- `trip-detail`: Add `OnMoveStopUp` / `OnMoveStopDown` intents; ViewModel handles them via `MoveStopUseCase`; `StopSection` renders move buttons for intermediate stops; string resources added for accessibility content descriptions.

## Impact

- **Data layer**: `StopDao` gains two new queries; `StopRepositoryImpl` gains a transactional swap method.
- **Domain layer**: New `MoveStopUseCase` class; `StopRepository` interface extended with `swapStopOrder`.
- **UI layer**: `TripDetailUiIntent`, `TripDetailViewModel`, `StopSection`, and `TripDetailScreen` modified; new strings in EN/IT/ES for move button content descriptions.
- **No new dependencies, migrations, or external API calls** — all operations are local Room database updates against the existing `stops` table schema.
