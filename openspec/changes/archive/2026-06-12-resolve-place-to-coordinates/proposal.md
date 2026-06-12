## Why

When a user taps an autocomplete suggestion, the Place Details API call fires with no visual feedback in the dialog — on slow connections the UI appears unresponsive. If the call fails, the error is shown as a Snackbar behind the open dialog, making it invisible to the user. These two UX gaps undermine confidence in the place selection flow introduced in 2.1.1.

## What Changes

- Add a `isResolvingPlace` loading flag to `TripDetailUiState`, toggled by the ViewModel around the `getPlaceDetailUseCase` call, and rendered as a progress indicator inside `SetStopDialog` with the confirm button disabled while resolving.
- Replace the `ShowError` effect on Place Details failure with a `placeDetailError` inline error field in `TripDetailUiState`, displayed inside the dialog (following the existing `searchError` pattern). The error is cleared automatically when the user types a new query or taps another suggestion.
- Update all four dialog call sites in `TripDetailScreen` to pass the two new fields.
- Add new unit tests for ViewModel resolving states and Compose UI tests for dialog resolving/error states.

## Capabilities

### New Capabilities

_(none — all changes fit within existing capabilities)_

### Modified Capabilities

- `trip-detail`: Add `isResolvingPlace` and `placeDetailError` to the UiState contract, update ViewModel `OnSuggestionSelected` handling and `clearSearchState`, update `SetStopDialog` and `TripDetailScreen` wiring.

## Impact

- **UI state**: `TripDetailUiState` gains two new fields (`isResolvingPlace: Boolean`, `placeDetailError: String?`)
- **ViewModel**: `OnSuggestionSelected` handler changes from emitting a `ShowError` effect to setting inline state; `clearSearchState()` clears the new fields; `OnSearchQueryChanged` clears `placeDetailError`
- **Composables**: `SetStopDialog` and `StopForm` gain new parameters; `TripDetailScreen` passes them through
- **Strings**: One new string resource for the place detail error message
- **Tests**: 5 new ViewModel unit tests, 3 new Compose UI tests, 1 existing test updated
