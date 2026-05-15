## ADDED Requirements

### Requirement: StopSection composable (consolidated)
`StopSection` SHALL be a single parameterised composable that replaces the separate `StartingPointSection` and `DestinationSection`. It accepts:
- `icon: ImageVector` — the icon to display in the filled state
- `@StringRes titleRes` — the section title
- `@StringRes setButtonTextRes` — the empty-state button label
- `@StringRes changeDescriptionRes` — the accessibility description for the filled card
- `@StringRes filledLabelRes` — the label shown above the place name in the filled state
- `stop: Stop?` — the stop to display, or null for empty state
- `onSetStopClicked: () -> Unit` — callback when the user taps to set/change

When `stop` is `null`: a placeholder card with an outlined button is displayed.
When `stop` is non-null: a card showing the place name, coordinates, icon, and label. Tapping it triggers the callback.

The section SHALL include a `contentDescription` on the button/card for accessibility.

#### Scenario: Empty state shown for starting point
- **WHEN** `stop` is `null` and `setButtonTextRes` is the "Set starting point" resource
- **THEN** a placeholder with "Set starting point" button is displayed

#### Scenario: Empty state shown for destination
- **WHEN** `stop` is `null` and `setButtonTextRes` is the "Set destination" resource
- **THEN** a placeholder with "Set destination" button is displayed

#### Scenario: Filled state shown for destination
- **WHEN** `stop` has `placeName = "Barcelona"` and coordinates, with `Place` icon and "Destination" label
- **THEN** a card with "Barcelona", coordinates, and a "Destination" label is displayed

#### Scenario: Tapping filled state triggers callback
- **WHEN** the user taps the stop card
- **THEN** `onSetStopClicked` is triggered

### Requirement: SetStopDialog composable (consolidated)
`SetStopDialog` SHALL be a single parameterised composable that replaces the separate `SetStartingPointDialog` and `SetDestinationDialog`. It accepts 7 `@StringRes` parameters for: dialog title, place name hint, place name error, latitude hint, latitude error, longitude hint, longitude error.

It SHALL display an `AlertDialog` with:
- Title from the provided string resource
- Three `OutlinedTextField` inputs with labels from the provided hint resources
- Confirm and dismiss buttons
- Input validation: place name must be non-blank; latitude in `[-90, 90]`; longitude in `[-180, 180]`. Inline error messages from the provided error resources shown on invalid input.

#### Scenario: Valid input enables confirmation
- **WHEN** all fields contain valid values
- **THEN** the confirm button triggers `onConfirm` with the entered values

#### Scenario: Invalid latitude shows error
- **WHEN** the user enters a latitude outside `[-90, 90]`
- **THEN** an inline error is shown on the latitude field using the provided latitude error resource

#### Scenario: Invalid longitude shows error
- **WHEN** the user enters a longitude outside `[-180, 180]`
- **THEN** an inline error is shown on the longitude field using the provided longitude error resource

#### Scenario: Blank place name shows error
- **WHEN** the user leaves the place name field blank
- **THEN** an inline error is shown on the place name field using the provided place name error resource
