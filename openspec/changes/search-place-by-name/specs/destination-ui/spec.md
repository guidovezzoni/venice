## MODIFIED Requirements

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

When initial values are provided, the corresponding fields SHALL be initialised with those values instead of empty strings.

When `isLoading` is `true`:
- The confirm button SHALL be disabled regardless of field validation state
- A `CircularProgressIndicator` SHALL be displayed next to the confirm button text
- The dismiss button SHALL remain enabled so the user can still close the dialog

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
