## Why

The Trip List screen introduced in 1.1.1 is incomplete: trip items show only a name, tapping them does nothing, and the empty state is a plain text label with no action. This story closes those gaps so the list screen is fully functional before deeper features (stops, routing) are built on top of it.

## What Changes

- Each trip list item gains a **stop count** sub-label (hardcoded to 0 until the stops table exists in 1.2.x).
- Tapping a trip item navigates to the Trip Detail screen for that trip (reusing the existing `NavigateToTripDetail` effect).
- The empty state is replaced by a dedicated composable with a descriptive message and a "Create your first trip" CTA button.
- Two new stateless composables are extracted: `TripListItem` and `TripEmptyState`.
- The old `trip_list_empty` string is removed; new strings (`trip_list_empty_title`, `trip_list_empty_action`) and a stop-count plural resource are added.

## Capabilities

### New Capabilities

None — no new capability is being introduced. All changes are enhancements to an existing capability.

### Modified Capabilities

- `trip-list`: Adding stop count display, item tap navigation, and a proper empty state with CTA. These are new requirements on the existing Trip List spec.

## Impact

- **Domain model**: `Trip` gains `stopCount: Int = 0`.
- **Data layer**: `TripMapper` maps `stopCount = 0` (with a `TODO 1.2.x` marker).
- **MVI**: New `OnTripClicked(tripId)` intent added to `TripListUiIntent`; handled in `TripListViewModel`.
- **UI**: `TripListScreen` updated; `TripListItem` and `TripEmptyState` composables created.
- **Navigation**: `MainScreen` wires the new intent to the existing `NavigateToTripDetail` effect handler (no handler changes needed).
- **Strings**: `strings.xml` updated; one string removed, two added, one plural resource added.
- **Tests**: Two new ViewModel test cases added to `TripListViewModelTest`.
