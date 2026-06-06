# Trip List

## Purpose

Defines the requirements for the Trip List screen — the app's main entry point — covering how trips are displayed, the FAB-triggered creation flow, post-creation navigation, and the MVI ViewModel contract.

## Requirements

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

#### Scenario: Empty state with CTA
- **WHEN** no trips have been created yet
- **THEN** the Trip List screen displays a full-screen empty state with a descriptive message and a "Create your first trip" button that opens the create trip dialog

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
> **Deferred — intentionally out of scope for this change.** Stops, legs, route calculation, and map integration are non-goals per `design.md`. The `Trip` domain model does not include these fields yet; this requirement will be addressed in a future change.

~~A newly created trip SHALL have no stops, no legs, and zero total distance and duration.~~

#### ~~Scenario: New trip defaults~~
- ~~**WHEN** a trip is created~~
- ~~**THEN** its stops list is empty, legs list is empty, total distance is 0, and total duration is 0~~

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
- **THEN** `isLoading` is set to `true`, the use case is invoked via `withMinimumDuration { ... }` (defaults to 500 ms), and on success `isLoading` is set to `false` and a `NavigateToTripDetail(tripId: String)` effect is emitted

#### Scenario: Failed creation emits error effect
- **WHEN** the use case returns failure
- **THEN** `isLoading` is set to `false` and a `ShowError` effect with the error message is emitted

#### Scenario: Trip tap emits navigation effect
- **WHEN** `OnTripClicked(tripId)` intent is dispatched
- **THEN** a `NavigateToTripDetail(tripId)` effect is emitted with the same trip id

### Requirement: TripListScreen passes loading state to CreateTripDialog
`TripListScreen` SHALL pass `uiState.isLoading` to `CreateTripDialog` when displaying the create trip dialog.

#### Scenario: Loading state passed to dialog
- **WHEN** `isCreateDialogVisible` is `true` and `isLoading` is `true`
- **THEN** `CreateTripDialog` receives `isLoading = true`

#### Scenario: Non-loading state passed to dialog
- **WHEN** `isCreateDialogVisible` is `true` and `isLoading` is `false`
- **THEN** `CreateTripDialog` receives `isLoading = false`

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
