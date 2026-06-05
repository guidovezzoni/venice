# Stop Progress Display

## Purpose

Defines how stops and trip progress are visually presented based on each stop's `StopDisplayState`, including icon overrides, border highlighting, opacity, and a trip-level progress summary.

## Requirements

### Requirement: Departed stops display a checkmark icon
When a stop's `StopDisplayState` is `DEPARTED`, the `StopSection` composable SHALL replace the caller-provided icon with `Icons.Filled.CheckCircle`. The existing 0.6f alpha on content elements SHALL be retained.

#### Scenario: Departed stop shows checkmark icon
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.DEPARTED`
- **THEN** the icon displayed SHALL be `Icons.Filled.CheckCircle` regardless of the `icon` parameter passed by the caller

#### Scenario: Departed stop retains reduced opacity
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.DEPARTED`
- **THEN** the content alpha SHALL remain 0.6f as established in story 1.3.1

### Requirement: Current stop card has a highlighted border
When a stop's `StopDisplayState` is `CURRENT`, the stop `Card` in `FilledStop` SHALL display a border with the primary colour at 2 dp width, using the Card's shape.

#### Scenario: Current stop shows primary border
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.CURRENT`
- **THEN** the `Card` SHALL have a `border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.shape)` applied

#### Scenario: Departed stop does not show border
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.DEPARTED`
- **THEN** the `Card` SHALL NOT have a highlighted border

#### Scenario: Upcoming stop does not show border
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.UPCOMING`
- **THEN** the `Card` SHALL NOT have a highlighted border

### Requirement: Upcoming stops remain in default style
When a stop's `StopDisplayState` is `UPCOMING`, no visual modifications SHALL be applied — the stop SHALL render in its default style with no icon override and no border.

#### Scenario: Upcoming stop uses caller-provided icon
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.UPCOMING`
- **THEN** the icon displayed SHALL be the `icon` parameter passed by the caller

#### Scenario: Upcoming stop has full opacity
- **WHEN** a `StopSection` is rendered with `stopDisplayState = StopDisplayState.UPCOMING`
- **THEN** the content alpha SHALL be 1.0f

### Requirement: Trip progress summary displays completed stop count
A `TripProgressSummary` composable SHALL display a text in the format "%1$d of %2$d stops completed" with a `LinearProgressIndicator` underneath. It SHALL be placed as the first item in the trip detail screen's `LazyColumn`, before the starting point section.

#### Scenario: Partial progress shown
- **WHEN** the trip has 6 stops and 3 have `StopStatus.VISITED`
- **THEN** the progress summary SHALL display "3 of 6 stops completed" and the `LinearProgressIndicator` progress SHALL be 0.5f

#### Scenario: No stops departed
- **WHEN** the trip has 4 stops and none have `StopStatus.VISITED`
- **THEN** the progress summary SHALL display "0 of 4 stops completed" and the `LinearProgressIndicator` progress SHALL be 0.0f

#### Scenario: All stops departed
- **WHEN** the trip has 6 stops and all 6 have `StopStatus.VISITED`
- **THEN** the progress summary SHALL display "6 of 6 stops completed" and the `LinearProgressIndicator` progress SHALL be 1.0f

### Requirement: Trip progress summary visibility
The `TripProgressSummary` SHALL only be visible when the total stop count is greater than zero. When no stops exist, no progress summary SHALL be rendered.

#### Scenario: Progress summary visible with stops
- **WHEN** at least one stop exists in the trip (starting point, intermediate, or destination)
- **THEN** the `TripProgressSummary` composable SHALL be displayed

#### Scenario: Progress summary hidden with no stops
- **WHEN** no stops exist in the trip (all slots are empty)
- **THEN** the `TripProgressSummary` composable SHALL NOT be rendered
