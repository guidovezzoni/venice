## ADDED Requirements

### Requirement: Trip List screen is the main screen
The Trip List screen SHALL be the app's main entry point, displayed when the app launches.

#### Scenario: App launch shows trip list
- **WHEN** the user opens the app
- **THEN** the Trip List screen is displayed

### Requirement: Trip List displays all saved trips
The Trip List screen SHALL display all trips persisted in the local database, ordered by creation date descending (newest first).

#### Scenario: Trips are displayed in order
- **WHEN** the user has created trips "A" (oldest) and "B" (newest)
- **THEN** trip "B" appears above trip "A" in the list

#### Scenario: Empty state
- **WHEN** no trips have been created yet
- **THEN** the Trip List screen displays an empty state (no items)

### Requirement: FAB for trip creation
The Trip List screen SHALL display a Floating Action Button (FAB) that triggers the create trip dialog.

#### Scenario: FAB is visible and tappable
- **WHEN** the Trip List screen is displayed
- **THEN** a FAB labelled "Create roadtrip" is visible and responds to taps

#### Scenario: Tapping FAB opens create dialog
- **WHEN** the user taps the FAB
- **THEN** the create trip dialog opens

### Requirement: Navigation to Trip Detail after creation
After a trip is successfully created, the system SHALL navigate the user to the Trip Detail screen for the newly created trip. The navigation SHALL be one-shot (not re-triggered on recomposition).

#### Scenario: Successful creation navigates to detail
- **WHEN** the user creates a trip named "Summer Drive"
- **THEN** the app navigates to the Trip Detail screen with the new trip's UUID

#### Scenario: Back navigation shows updated list
- **WHEN** the user navigates back from the Trip Detail screen
- **THEN** the Trip List screen shows the newly created trip in the list

### Requirement: New trip has empty route data
A newly created trip SHALL have no stops, no legs, and zero total distance and duration.

#### Scenario: New trip defaults
- **WHEN** a trip is created
- **THEN** its stops list is empty, legs list is empty, total distance is 0, and total duration is 0

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
