## Context

The Trip List screen was scaffolded in story 1.1.1 with a working MVI stack: `TripListUiState`, `TripListUiIntent`, `TripListUiEffect` (including `NavigateToTripDetail`), `TripListViewModel`, and a `LazyColumn`-based `TripListScreen`. Navigation to the Trip Detail screen is already wired in `MainScreen` via a `LaunchedEffect` that collects `TripListUiEffect`. The screen currently shows trip names only, does not respond to item taps, and shows a plain-text empty state with no action.

## Goals / Non-Goals

**Goals:**
- Add stop count sub-label to each list item (hardcoded `0` for now)
- Enable item tap → navigate to Trip Detail for that trip
- Replace text-only empty state with a composable that includes a CTA button
- Extract `TripListItem` and `TripEmptyState` as separate, testable, previewable composables

**Non-Goals:**
- Stops table or route data (deferred to 1.2.x)
- Real stop count derived from DB (deferred to 1.2.x)
- Trip editing or deletion from the list
- Swipe-to-delete or reorder

## Decisions

### 1. Reuse `NavigateToTripDetail` for item tap
The existing effect and its handler in `MainScreen` already navigate to the detail screen. Adding a new `OnTripClicked` intent that emits this same effect costs one branch in the ViewModel `when` block and zero changes to `MainScreen`.

**Alternative considered**: A dedicated `NavigateToExistingTripDetail` effect to distinguish creation vs. tap. Rejected — the destination and the navigation call are identical; distinguishing them adds complexity with no benefit.

### 2. Hardcode `stopCount = 0` in mapper, keep DAO unchanged
The stops table does not exist yet. The story's alternative — a subquery in Room that counts from a non-existent table — would require a new `TripWithStopCount` data class, a separate DAO method, and a more complex mapper, all only to return `0`. Hardcoding in the mapper is the simpler, safer choice and is clearly marked `TODO 1.2.x`.

### 3. Empty state CTA button alongside FAB
The empty state composable includes a "Create your first trip" button in addition to the FAB. This is a deliberate UX decision: on an empty screen the FAB alone can be easy to miss. The button provides a prominent, contextual prompt. Both call the same `onCreateTripClicked` lambda.

### 4. Extract `TripListItem` and `TripEmptyState` as separate composables
Extracting keeps `TripListScreen` focused on layout/scaffolding and makes each component independently previewable and testable. Both are stateless, receiving only display data and lambdas.

## Risks / Trade-offs

- **Stop count always 0**: Until 1.2.x, every trip shows "0 stops". This is visible to users if the app ships between stories. → Acceptable; clearly documented with a TODO.
- **Plural string dependency**: `pluralStringResource` for stop count requires the `<plurals>` resource to be defined before composable compilation. → Low risk; the string is added in the same change.
- **Redundant CTA + FAB**: Two UI elements triggering the same action could feel noisy. → Common pattern in well-known apps (Google Drive, Keep); monitored for UX feedback in later iterations.

## Migration Plan

No database migrations required. The `stopCount` field is added to the domain model only (not persisted until 1.2.x). No breaking changes to public APIs or navigation routes.
