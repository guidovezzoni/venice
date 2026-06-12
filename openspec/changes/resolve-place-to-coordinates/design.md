## Context

Story 2.1.1 delivered the full place-search-to-coordinates pipeline: autocomplete suggestions, Place Details fetch, coordinate auto-fill in dialogs. The pipeline works correctly, but two UX gaps remain:

1. No loading feedback while the Place Details API call is in-flight after the user taps a suggestion.
2. On failure, the error is emitted as a `ShowError` effect (Snackbar) which renders behind the open dialog, invisible to the user.

The existing codebase already has inline loading/error patterns in the same dialog:
- `isSearchingPlaces` + spinner in `StopForm` for autocomplete search loading
- `searchError` + inline `Text` in `StopForm` for search errors
- `isLoading` + confirm button disable in `SetStopDialog` for save operations

## Goals / Non-Goals

**Goals:**
- Show a progress indicator inside the dialog while resolving a place to coordinates
- Disable the confirm button while resolving to prevent partial submissions
- Show the Place Details error inline in the dialog so the user can see it and retry
- Clear the error automatically when the user initiates a new search or taps another suggestion

**Non-Goals:**
- No minimum loading duration for `isResolvingPlace` (unlike the `isLoading`/`withMinimumDuration` pattern for save operations). The Place Details call is a fetch-then-populate flow; artificially extending it would delay the auto-fill.
- No new UiIntent or UiEffect — the change is entirely state-driven.
- No changes to the domain layer, data layer, or Place Details API call itself.

## Decisions

### 1. Separate `isResolvingPlace` flag (not reusing `isLoading`)

`isLoading` gates save operations at the screen level (spinner overlay, minimum 500ms duration, disables all buttons). Place resolution is a dialog-scoped fetch with different semantics: no minimum duration, only disables the confirm button. A separate boolean avoids coupling and keeps each concern testable independently.

**Alternative considered**: Reuse `isLoading` — rejected because it would show the screen-level overlay spinner and enforce the 500ms minimum, which is wrong for a lightweight fetch.

### 2. Inline error via `placeDetailError` state field (not `ShowError` effect)

The dialog is modal — any Snackbar at the screen level is hidden behind it. Moving the error to UiState as `placeDetailError: String?` lets the dialog render it inline, following the identical pattern used for `searchError`.

**Alternative considered**: Dismiss dialog on error, then show Snackbar — rejected because it breaks the user's context and forces them to re-open the dialog and re-search.

### 3. Resolving spinner placement: inside StopForm, after the suggestions list

The spinner appears in the same vertical zone where `isSearchingPlaces` already shows its spinner and where `searchError` shows its text — between the place name field and the coordinate fields. This is consistent with the existing layout and gives the user clear context: "I tapped a suggestion and something is happening."

### 4. Error clearing triggers

`placeDetailError` is cleared when:
- `OnSearchQueryChanged` is dispatched (user starts a new search)
- `OnSuggestionSelected` is dispatched (user taps another suggestion to retry)
- `clearSearchState()` is called (dialog dismiss)

This matches how `searchError` is already cleared and ensures stale errors don't persist.

## Risks / Trade-offs

**[Risk]** The `isResolvingPlace` flag could get stuck if the coroutine is cancelled mid-flight without resetting it → **Mitigation**: `clearSearchState()` is called on every dialog dismiss path, which resets `isResolvingPlace` to `false`. The ViewModel's `viewModelScope` also cancels coroutines on ViewModel destruction.

**[Trade-off]** Adding two more fields to an already large `TripDetailUiState` (26 → 28 fields) → Accepted; the story's TODO already notes the state should be broken into a sealed class, but that refactor is out of scope.
