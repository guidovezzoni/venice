## ADDED Requirements

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
