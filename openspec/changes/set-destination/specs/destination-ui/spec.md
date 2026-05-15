## ADDED Requirements

### Requirement: DestinationSection composable
`DestinationSection` SHALL display:
- When `destination` is `null`: a placeholder card with a "Set destination" button.
- When `destination` is non-null: a card showing the place name, coordinates, and a `Place` icon with "Destination" label. Tapping it triggers `OnSetDestinationClicked`.

The section SHALL include a `contentDescription` on the button for accessibility.

#### Scenario: Empty state shown
- **WHEN** `destination` is `null`
- **THEN** a placeholder with "Set destination" button is displayed

#### Scenario: Filled state shown
- **WHEN** `destination` has `placeName = "Barcelona"` and coordinates
- **THEN** a card with "Barcelona", coordinates, and a "Destination" label is displayed

#### Scenario: Tapping filled state triggers change
- **WHEN** the user taps the destination card
- **THEN** `OnSetDestinationClicked` is triggered

### Requirement: SetDestinationDialog composable
`SetDestinationDialog` SHALL display an `AlertDialog` with:
- Title: "Set destination"
- Three `OutlinedTextField` inputs with labels: "Place name", "Latitude", "Longitude"
- Confirm and dismiss buttons
- Input validation: place name must be non-blank; latitude in `[-90, 90]`; longitude in `[-180, 180]`. Inline error messages shown on invalid input.

#### Scenario: Valid input enables confirmation
- **WHEN** all fields contain valid values
- **THEN** the confirm button triggers `OnDestinationConfirmed` with the entered values

#### Scenario: Invalid latitude shows error
- **WHEN** the user enters a latitude outside `[-90, 90]`
- **THEN** an inline error is shown on the latitude field

#### Scenario: Invalid longitude shows error
- **WHEN** the user enters a longitude outside `[-180, 180]`
- **THEN** an inline error is shown on the longitude field

#### Scenario: Blank place name shows error
- **WHEN** the user leaves the place name field blank
- **THEN** an inline error is shown on the place name field
