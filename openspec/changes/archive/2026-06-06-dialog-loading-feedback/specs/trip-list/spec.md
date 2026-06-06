## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: TripListScreen passes loading state to CreateTripDialog
`TripListScreen` SHALL pass `uiState.isLoading` to `CreateTripDialog` when displaying the create trip dialog.

#### Scenario: Loading state passed to dialog
- **WHEN** `isCreateDialogVisible` is `true` and `isLoading` is `true`
- **THEN** `CreateTripDialog` receives `isLoading = true`

#### Scenario: Non-loading state passed to dialog
- **WHEN** `isCreateDialogVisible` is `true` and `isLoading` is `false`
- **THEN** `CreateTripDialog` receives `isLoading = false`
