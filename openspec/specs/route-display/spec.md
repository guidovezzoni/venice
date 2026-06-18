# Route Display

## Purpose

Defines the UI composables and string resources for displaying route calculation results (distance and duration) between consecutive stops on the trip detail screen.

## Requirements

### Requirement: LegSummary composable displays leg information
The UI layer SHALL define a `LegSummary` composable in `ui/screens/tripdetail/LegSummary.kt` that:
- Accepts `modifier: Modifier = Modifier`, `distanceMetres: Int`, and `durationSeconds: Int`.
- Displays a formatted distance: metres if < 1000, otherwise kilometres with one decimal place (e.g., "450 m" or "12.3 km").
- Displays a formatted duration: minutes if < 60 minutes, otherwise hours and minutes (e.g., "45 min" or "1 h 30 min").
- Uses a compact, visually lightweight style (smaller text, secondary colour) to sit naturally between stop cards.

#### Scenario: Short distance display
- **WHEN** `LegSummary` is rendered with `distanceMetres = 450` and `durationSeconds = 300`
- **THEN** the distance is displayed as "450 m" and the duration as "5 min"

#### Scenario: Long distance display
- **WHEN** `LegSummary` is rendered with `distanceMetres = 12345` and `durationSeconds = 5400`
- **THEN** the distance is displayed as "12.3 km" and the duration as "1 h 30 min"

#### Scenario: Duration less than 1 minute
- **WHEN** `LegSummary` is rendered with `durationSeconds = 45`
- **THEN** the duration is displayed as "1 min" (rounded up to minimum 1 minute)

#### Scenario: Duration exactly 60 minutes
- **WHEN** `LegSummary` is rendered with `durationSeconds = 3600`
- **THEN** the duration is displayed as "1 h 0 min"

### Requirement: LegSummary preview coverage
`LegSummary` SHALL have previews covering:
- Short distance (metres) with short duration (minutes only)
- Long distance (kilometres) with long duration (hours and minutes)

#### Scenario: Previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** at least two preview variants are visible

### Requirement: Route calculation string resources
The app SHALL define the following string resources:
- `trip_detail_calculate_route`: "Calculate route" — button label
- `trip_detail_calculating_route`: "Calculating route…" — loading description
- `trip_detail_route_error`: "Route calculation failed: %1$s" — error message template
- `trip_detail_leg_distance_metres`: "%1$d m" — distance in metres
- `trip_detail_leg_distance_kilometres`: "%1$.1f km" — distance in kilometres
- `trip_detail_leg_duration_minutes`: "%1$d min" — duration in minutes only
- `trip_detail_leg_duration_hours_minutes`: "%1$d h %2$d min" — duration in hours and minutes

#### Scenario: String resources defined
- **WHEN** the app is built
- **THEN** all route calculation string resources resolve without errors

### Requirement: LegSummary Compose UI test coverage
The project SHALL have a Compose UI test file `LegSummaryTest` in `app/src/androidTest/.../ui/screens/tripdetail/` that verifies distance formatting and composable rendering.

#### Scenario: Short distance displays in metres
- **WHEN** `LegSummary` is rendered with a `Leg` where `distanceMetres = 750`
- **THEN** the text "750 m" is displayed

#### Scenario: Boundary distance displays in kilometres
- **WHEN** `LegSummary` is rendered with a `Leg` where `distanceMetres = 1000`
- **THEN** the text "1.0 km" is displayed

#### Scenario: Long distance displays in kilometres with one decimal
- **WHEN** `LegSummary` is rendered with a `Leg` where `distanceMetres = 12500`
- **THEN** the text "12.5 km" is displayed

#### Scenario: Combined distance and duration text is displayed
- **WHEN** `LegSummary` is rendered with a `Leg` where `distanceMetres = 5000` and `durationSeconds = 600`
- **THEN** text containing both "5.0 km" and "10 min" is displayed

### Requirement: TripDetailScreen leg display test coverage
The project SHALL have Compose UI tests in `TripDetailScreenTest` that verify leg display integration on the trip detail screen.

#### Scenario: Legs displayed when route data exists
- **WHEN** `TripDetailScreen` is rendered with a `uiState` containing stops and corresponding legs
- **THEN** the leg distance text is displayed between the connected stops

#### Scenario: Legs absent when no route calculated
- **WHEN** `TripDetailScreen` is rendered with a `uiState` containing stops but an empty `legs` list
- **THEN** no leg distance text is displayed
