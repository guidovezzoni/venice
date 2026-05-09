## ADDED Requirements

### Requirement: Trip list items show stop count
Each trip item in the list SHALL display the number of stops for that trip below the trip name, using correct singular/plural form.

#### Scenario: Item shows stop count
- **WHEN** the trip list is displayed
- **THEN** each item shows the trip name and a stop count label (e.g. "3 stops", "1 stop", "0 stops")

#### Scenario: Zero stops displayed before stops feature
- **WHEN** the stops table does not yet exist
- **THEN** each item shows "0 stops"

### Requirement: Tapping a trip item navigates to Trip Detail
The Trip List screen SHALL navigate to the Trip Detail screen when the user taps any trip item in the list.

#### Scenario: Tap navigates to detail
- **WHEN** the user taps a trip item in the list
- **THEN** the app navigates to the Trip Detail screen for that trip

#### Scenario: Navigation carries correct trip id
- **WHEN** the user taps a trip with id "abc-123"
- **THEN** the Trip Detail screen receives trip id "abc-123"

## MODIFIED Requirements

### Requirement: Trip List displays all saved trips
The Trip List screen SHALL display all trips persisted in the local database, ordered by creation date descending (newest first).

#### Scenario: Trips are displayed in order
- **WHEN** the user has created trips "A" (oldest) and "B" (newest)
- **THEN** trip "B" appears above trip "A" in the list

#### Scenario: Empty state with CTA
- **WHEN** no trips have been created yet
- **THEN** the Trip List screen displays a full-screen empty state with a descriptive message and a "Create your first trip" button that opens the create trip dialog

### Requirement: TripListViewModel exposes MVI contract
The `TripListViewModel` SHALL follow the MVI pattern:
- Expose `uiState: StateFlow<TripListUiState>` with the current trip list and dialog state.
- Expose `uiEffect: SharedFlow<TripListUiEffect>` for one-shot events (navigation, snackbar).
- Accept user actions via `fun onIntent(intent: TripListUiIntent)`.

#### Scenario: Intent to open dialog updates state
- **WHEN** `OnCreateTripClicked` intent is dispatched
- **THEN** `uiState.isCreateDialogVisible` becomes `true`

#### Scenario: Successful creation emits navigation effect
- **WHEN** `ConfirmCreateTrip` intent is dispatched with a valid name
- **THEN** the use case is invoked and on success, a `NavigateToTripDetail(tripId: String)` effect is emitted

#### Scenario: Failed creation emits error effect
- **WHEN** the use case returns failure
- **THEN** a `ShowError` effect with the error message is emitted

#### Scenario: Trip tap emits navigation effect
- **WHEN** `OnTripClicked(tripId)` intent is dispatched
- **THEN** a `NavigateToTripDetail(tripId)` effect is emitted with the same trip id
