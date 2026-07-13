## Why

Users currently have no way to get turn-by-turn directions to a stop without leaving Venice, manually copying coordinates, and pasting them into a separate maps app. Every Android device already has at least one navigation app installed; the platform's implicit intent mechanism lets Venice hand off navigation to the user's preferred app instead of building routing UI itself.

## What Changes

- Add a "Navigate" icon button (`Icons.Filled.Navigation`) to each stop card on the trip detail screen.
- The button is shown only for stops with `StopStatus.PENDING`; it is hidden for `StopStatus.VISITED` stops.
- The button is disabled while `TripDetailUiState.isLoading` is `true`, consistent with other stop actions.
- Tapping the button dispatches `TripDetailUiIntent.OnNavigateToStopClicked(stopId)`. The ViewModel resolves the stop's persisted `latitude`/`longitude`/`placeName` and emits `TripDetailUiEffect.LaunchNavigation(latitude, longitude, placeName)`.
- `MainScreen.kt` consumes `LaunchNavigation` by building an `ACTION_VIEW` intent with a `geo:{lat},{lon}?q={lat},{lon}({placeName})` URI, resolving it via the package manager, and starting the activity. This surfaces the system app picker when multiple navigation apps are installed.
- If no app can resolve the intent, the ViewModel-provided localized error is shown via `TripDetailUiEffect.ShowNavigationError(message)`, rendered as a snackbar the same way `ShowError` is today.
- Add an `AndroidManifest.xml` `<queries>` block declaring the `geo:` scheme intent filter, required for package visibility on API 30+ (targetSdk 36); without it `resolveActivity()` returns null even when a capable app is installed.
- Extract geo URI construction into a new, independently testable `ui/util/NavigationUriBuilder.kt`, following the existing `CoordinateFormatter`/`DistanceFormatter` pattern.
- Add new string resources (`global_` prefix not applicable — feature-specific strings) for the navigate content description and the no-app-available error, translated for EN/IT/ES.

## Capabilities

### New Capabilities
- `stop-navigation`: Navigate action on stop cards that launches an external navigation app via an implicit `geo:` intent, including visibility rules, URI construction, error handling, and accessibility labelling.

### Modified Capabilities
(none — no existing capability's requirements change; `stop-progress-display` visuals are unaffected, this only adds a new action alongside existing ones)

## Impact

- **UI**: `StopSection.kt` (`FilledStop` gains an `onNavigate` action lambda parameter), `TripDetailScreen.kt` (wires `onNavigate` at the three `StopSection` call sites, computed from `StopStatus`), `MainScreen.kt` (handles two new effect types, builds and launches the intent).
- **ViewModel/MVI contracts**: `TripDetailUiIntent.kt` (+`OnNavigateToStopClicked`), `TripDetailUiEffect.kt` (+`LaunchNavigation`, +`ShowNavigationError`), `TripDetailViewModel.kt` (+intent handling).
- **New utility**: `ui/util/NavigationUriBuilder.kt` + unit test.
- **Manifest**: `AndroidManifest.xml` gains a `<queries>` block — required for correct behaviour on API 30+, otherwise navigation silently fails.
- **Resources**: `strings.xml` for `values/`, `values-it/`, `values-es/`.
- **No new dependencies, no schema/persistence changes, no permissions required.**
