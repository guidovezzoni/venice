## Context

Story 1.3.1 introduced `StopStatus` (PENDING/VISITED) in the domain layer and `StopDisplayState` (DEPARTED/CURRENT/UPCOMING) in the UI layer. The current trip detail screen applies minimal visual cues: 0.6f alpha for departed stops and primary-colour icon tint for the current stop. No progress summary exists.

All required data (`StopStatus` per stop) is already available through the existing `observeStopsUseCase` flow — no new data or domain changes are needed.

## Goals / Non-Goals

**Goals:**
- Provide clear, glanceable visual distinction between departed, current, and upcoming stops
- Show an aggregate progress indicator so users understand how far along their trip they are
- Maintain consistency with existing Material 3 styling and the app's composable patterns

**Non-Goals:**
- Timeline/vertical-dot progress visualisation (noted in original story as future consideration for Android Auto)
- Animating transitions between stop states
- Percentage-based progress or estimated time/distance calculations
- Changes to the domain or data layer

## Decisions

### 1. Icon override inside StopSection vs. caller-side

**Decision**: Override the icon inside `FilledStop` when `stopDisplayState == DEPARTED`, replacing whatever icon the caller passes with `Icons.Filled.CheckCircle`.

**Rationale**: Keeps all display-state visual logic co-located in `StopSection`. The caller already passes `stopDisplayState` — having it also adjust the icon would split the concern across two files.

**Alternative considered**: Passing the correct icon from `TripDetailScreen` based on display state. Rejected because it duplicates state-to-visual mapping and forces every call site to handle the logic.

### 2. Border approach for current stop

**Decision**: Apply `Modifier.border(2.dp, primary, shape)` to the existing `Card` composable in `FilledStop` when `stopDisplayState == CURRENT`. Use the Card's shape for the border so they align.

**Rationale**: The Card already handles the stop's clickable area and visual boundary. Adding a border modifier is non-invasive and uses standard Compose APIs.

### 3. TripProgressSummary as a standalone composable

**Decision**: Create `TripProgressSummary` as a stateless composable accepting `departedCount: Int` and `totalCount: Int`. Place it as the first `item` in `TripDetailScreen`'s `LazyColumn`.

**Rationale**: Follows the project's state-hoisting pattern. Makes the component independently previewable and testable. The progress computation (counting VISITED stops) stays in `TripDetailScreen` where the stop list is already assembled.

### 4. Progress count computation location

**Decision**: Compute `departedCount` and `totalCount` inside `TripDetailScreen` from the `allStops` list that is already built for `currentStopId` / `lastDepartedStopId` derivation.

**Rationale**: The `allStops` list already exists. Adding two `.count()` calls is trivial. No need to push this into the ViewModel or UiState — the data is already present in the composable scope.

## Risks / Trade-offs

- **[Risk] Checkmark icon for all departed stop types (start, intermediate, destination)** → Acceptable: the checkmark universally signals "done" regardless of stop role. The section title still distinguishes the stop type.
- **[Risk] Border on Card might conflict with Card's built-in elevation/shadow** → Low risk: Material 3 Card uses `CardDefaults.cardColors()` with no border by default, so a 2dp primary border adds cleanly. Verified in the current `StopSection` code that no custom shape or border is applied.
- **[Trade-off] Progress count not in UiState** → Keeps UiState unchanged (no new fields to maintain/test) at the cost of recomputing counts on each recomposition. Acceptable given the max stop count is 25.
