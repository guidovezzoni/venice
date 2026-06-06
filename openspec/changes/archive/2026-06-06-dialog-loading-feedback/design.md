## Context

Both ViewModels already declare `isLoading` in their UI state, and the pattern for wrapping async operations (set `true` before, `false` after on success/failure) is established by `setStop()` in `TripDetailViewModel` and `createTrip()` in `TripListViewModel`. However, five operations in `TripDetailViewModel` skip this pattern, and neither screen passes the flag to composables. Users can currently double-tap actions during processing with no visual feedback.

## Goals / Non-Goals

**Goals:**
- Every async DB operation in both ViewModels sets `isLoading = true` before and `false` after (success or failure)
- Dialog confirm buttons and `StopSection` action buttons are disabled while loading
- A `CircularProgressIndicator` is visible while loading
- Loading state has preview and test coverage

**Non-Goals:**
- Per-operation loading flags (a single `isLoading` covers all operations — they are mutually exclusive in practice)
- Skeleton/shimmer loading states — a simple spinner is sufficient for these sub-second DB ops
- Loading state for the initial data fetch via `ObserveStopsUseCase` (it uses Flow and the list just appears)

## Decisions

### 1. Single `isLoading` flag for all operations
The existing single `isLoading: Boolean` in both `UiState` classes is sufficient. All async operations are user-initiated and effectively sequential — the user cannot trigger two DB operations simultaneously in normal usage. Adding per-operation flags would complicate the state for no practical benefit.

**Alternative considered**: Per-operation loading enum (e.g. `LoadingOperation.EDITING`, `LoadingOperation.REMOVING`). Rejected because it adds complexity with no UX benefit — all operations disable the same buttons.

### 2. Disable buttons via `enabled` parameter, not by removing lambdas
`StopSection` currently shows/hides buttons by passing `null` vs non-null lambdas. For loading state, buttons should remain visible but greyed out (disabled) to communicate "processing" rather than "unavailable". This means adding an `isLoading: Boolean = false` parameter to `StopSection` and using it in the `enabled` property of `IconButton` and `OutlinedButton`.

**Alternative considered**: Passing `null` lambdas during loading to hide buttons entirely. Rejected because it causes visual layout shift and doesn't communicate loading state.

### 3. Spinner placement in dialogs
A small `CircularProgressIndicator` will be shown inline next to the confirm button (inside the `confirmButton` slot) when loading. This keeps the dialog compact and clearly associates the spinner with the action being processed.

### 4. Screen-level spinner
`TripDetailScreen` and `TripListScreen` will show a `CircularProgressIndicator` within a `Box` overlay when `isLoading = true` for operations not initiated from a dialog (e.g. moveStop, removeStop confirmation, markDeparted). The dialog-initiated operations already have their own spinner inside the dialog.

### 5. Remove-stop confirmation dialog disabling
The remove-stop confirmation dialog in `TripDetailScreen` is a plain `AlertDialog` (not `SetStopDialog`). Its confirm button will also be disabled during loading, consistent with the other dialogs.

### 6. Minimum spinner duration of 500 ms
Room DB operations complete in sub-100ms, making the spinner barely perceptible. A minimum 500ms display time ensures the user sees feedback and prepares the UX for future networking where latency will be higher.

Implementation uses a `withMinimumDuration` suspend utility that runs the operation and a `delay(minimumMillis)` in parallel via `coroutineScope + async`, returning only when both complete:

```kotlin
private const val MINIMUM_LOADING_DURATION_MILLIS = 500L

suspend fun <T> withMinimumDuration(
    minimumMillis: Long = MINIMUM_LOADING_DURATION_MILLIS,
    block: suspend () -> T,
): T = coroutineScope {
    val deferredDelay = async { delay(minimumMillis) }
    val result = block()
    deferredDelay.await()
    result
}
```

If the operation takes 50ms, the function waits until 500ms. If it takes 3 seconds, it returns immediately after the operation. The `MINIMUM_LOADING_DURATION_MILLIS` constant lives in the utility file as a file-level private constant, so callers simply write `withMinimumDuration { useCase(...) }` without specifying the duration.

**Alternative considered**: UI-layer approach using `LaunchedEffect` with a delay to hold the spinner. Rejected because it mixes timing concerns into composables and doesn't compose cleanly with the ViewModel's `isLoading` flag lifecycle.

**Testing**: `UnconfinedTestDispatcher` eagerly completes `delay()`, so existing ViewModel tests are unaffected. A dedicated unit test for `withMinimumDuration` using `StandardTestDispatcher` with `advanceTimeBy` verifies the timing guarantee.

## Risks / Trade-offs

- **[Low] Operations already fast**: Room operations are typically sub-100ms. The spinner may barely be visible. This is acceptable — the primary benefit is preventing double-submission, not showing a long loading state.
- **[Low] Test timing with UnconfinedTestDispatcher**: Since the test dispatcher completes coroutines eagerly, `isLoading` transitions to `true` and back to `false` within the same test frame. Tests will need to use a `StandardTestDispatcher` or verify the call sequence via `coVerify` ordering rather than capturing the intermediate `isLoading = true` state. Alternatively, the existing `UnconfinedTestDispatcher` tests can verify that `isLoading` is `false` after completion (confirming the reset path works).
