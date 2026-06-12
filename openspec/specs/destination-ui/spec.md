# Destination UI

## Purpose

Defines the consolidated, parameterised UI composables for displaying and editing stops (starting point and destination) in a trip.

## Requirements

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

It SHALL also accept three optional initial-value parameters:
- `initialPlaceName: String = ""` — pre-populates the place name field
- `initialLatitude: String = ""` — pre-populates the latitude field
- `initialLongitude: String = ""` — pre-populates the longitude field

It SHALL also accept a loading parameter:
- `isLoading: Boolean = false` — when `true`, the confirm button SHALL be disabled and a `CircularProgressIndicator` SHALL be displayed inline in the confirm button slot

It SHALL also accept place search parameters:
- `suggestions: List<PlaceSuggestion> = emptyList()` — autocomplete suggestions to display below the place name field
- `isSearchingPlaces: Boolean = false` — when `true`, a small `CircularProgressIndicator` SHALL be displayed near the place name field
- `searchError: String? = null` — when non-null, an inline message SHALL be displayed below the place name field (e.g. "No results found" or "Search unavailable")
- `selectedPlaceDetail: PlaceDetail? = null` — when non-null, the composable SHALL update the place name, latitude, and longitude local state from this value and set coordinates to read-only
- `onSearchQueryChanged: (String) -> Unit = {}` — callback invoked when the place name text changes, used to trigger autocomplete search
- `onSuggestionSelected: (PlaceSuggestion) -> Unit = {}` — callback invoked when the user taps a suggestion

It SHALL also accept place-resolution feedback parameters:
- `isResolvingPlace: Boolean = false` — when `true`, a `CircularProgressIndicator` SHALL be displayed inside the form body with semantics content description "Resolving place", and the confirm button SHALL be disabled
- `placeDetailError: String? = null` — when non-null, the error text SHALL be displayed inline in the form body using `MaterialTheme.colorScheme.error` styling, following the same pattern as `searchError`

When initial values are provided, the corresponding fields SHALL be initialised with those values instead of empty strings.

When `isLoading` is `true`:
- The confirm button SHALL be disabled regardless of field validation state
- A `CircularProgressIndicator` SHALL be displayed next to the confirm button text
- The dismiss button SHALL remain enabled so the user can still close the dialog

The confirm button SHALL also be disabled when `isResolvingPlace` is `true`.

It SHALL display an `AlertDialog` with:
- Title from the provided string resource
- Three `OutlinedTextField` inputs with labels from the provided hint resources, pre-populated with any provided initial values
- A suggestion list below the place name field when `suggestions` is not empty
- Confirm and dismiss buttons
- Input validation: place name must be non-blank; latitude in `[-90, 90]`; longitude in `[-180, 180]`. Inline error messages from the provided error resources shown on invalid input.

#### Scenario: Dialog pre-populated when editing existing stop
- **WHEN** the dialog is opened with `initialPlaceName = "Rome"`, `initialLatitude = "41.9028"`, `initialLongitude = "12.4964"`
- **THEN** the place name field shows "Rome", latitude field shows "41.9028", and longitude field shows "12.4964"

#### Scenario: Dialog empty when creating new stop
- **WHEN** the dialog is opened with default (empty) initial values
- **THEN** all fields are empty

#### Scenario: Valid input enables confirmation
- **WHEN** all fields contain valid values and `isLoading` is `false`
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

#### Scenario: Confirm button disabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the confirm button is disabled regardless of field validation state

#### Scenario: Spinner visible while loading
- **WHEN** `isLoading` is `true`
- **THEN** a `CircularProgressIndicator` is displayed in the confirm button slot

#### Scenario: Dismiss button enabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the dismiss button remains enabled

#### Scenario: Suggestion list displayed when suggestions are available
- **WHEN** `suggestions` contains items
- **THEN** a list of suggestions is displayed below the place name field, each showing primary text and secondary text

#### Scenario: Suggestion list hidden when no suggestions
- **WHEN** `suggestions` is empty
- **THEN** no suggestion list is displayed

#### Scenario: Search loading indicator shown while searching
- **WHEN** `isSearchingPlaces` is `true`
- **THEN** a small `CircularProgressIndicator` is displayed near the place name field

#### Scenario: Search error displayed inline
- **WHEN** `searchError` is non-null (e.g. "No results found")
- **THEN** the error message is displayed below the place name field

#### Scenario: Tapping a suggestion invokes callback
- **WHEN** the user taps a suggestion item
- **THEN** `onSuggestionSelected` is called with the tapped `PlaceSuggestion`

#### Scenario: Place name change invokes search callback
- **WHEN** the user types in the place name field
- **THEN** `onSearchQueryChanged` is called with the current text AND the local `placeName` state is updated

#### Scenario: Resolving place shows indicator and disables confirm
- **WHEN** `isResolvingPlace` is `true`
- **THEN** a progress indicator with content description "Resolving place" is visible and the confirm button is disabled

#### Scenario: Place detail error shown inline
- **WHEN** `placeDetailError` is set to a non-null value
- **THEN** the error message is displayed inline in the dialog using error colour styling

#### Scenario: Default state shows no resolving indicator or error
- **WHEN** `isResolvingPlace` is `false` and `placeDetailError` is `null`
- **THEN** no resolving indicator or place detail error message is shown

#### Scenario: Selected place detail populates fields
- **WHEN** `selectedPlaceDetail` transitions from `null` to a non-null `PlaceDetail(name = "Colosseum", latitude = 41.8902, longitude = 12.4922)`
- **THEN** the place name field is updated to "Colosseum", latitude to "41.8902", longitude to "12.4922"

#### Scenario: Coordinates become read-only after selection
- **WHEN** a `selectedPlaceDetail` has been applied
- **THEN** the latitude and longitude `OutlinedTextField` fields are set to `readOnly = true`

#### Scenario: Coordinates revert to editable when place name changes
- **WHEN** the user manually changes the place name text after a place was selected
- **THEN** the latitude and longitude fields revert to editable (`readOnly = false`) and the suggestion list clears

#### Scenario: Suggestion list dismissed after selection
- **WHEN** a suggestion is tapped and `selectedPlaceDetail` is applied
- **THEN** the suggestion list is no longer displayed

#### Scenario: Manual entry still works without autocomplete
- **WHEN** the user types a place name and coordinates manually without selecting a suggestion
- **THEN** the confirm button works as before, submitting the manually entered values

### Requirement: StopSection accepts loading state
`StopSection` SHALL accept an `isLoading: Boolean = false` parameter. When `isLoading` is `true`, all action buttons within the section SHALL be disabled:
- `IconButton` for move up SHALL be disabled (rendered but not clickable)
- `IconButton` for move down SHALL be disabled (rendered but not clickable)
- `IconButton` for delete SHALL be disabled (rendered but not clickable)
- `OutlinedButton` for mark departed SHALL be disabled
- `OutlinedButton` for undo departed SHALL be disabled

The buttons SHALL remain visible (not hidden) to communicate that the action exists but is temporarily unavailable.

#### Scenario: Action buttons disabled while loading
- **WHEN** `StopSection` is rendered with `isLoading = true` and all action lambdas are non-null
- **THEN** move up, move down, delete, mark departed, and undo departed buttons are all visible but disabled

#### Scenario: Action buttons enabled when not loading
- **WHEN** `StopSection` is rendered with `isLoading = false` and all action lambdas are non-null
- **THEN** move up, move down, delete, mark departed, and undo departed buttons are all enabled
