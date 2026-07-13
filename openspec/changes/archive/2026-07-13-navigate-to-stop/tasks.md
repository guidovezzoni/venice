## 1. Prerequisites (MVI contracts)

- [x] 1.1 Add `OnNavigateToStopClicked(val stopId: String)` to `TripDetailUiIntent.kt` (`app/src/main/java/com/guidovezzoni/venice/ui/intent/TripDetailUiIntent.kt`)
- [x] 1.2 Add `LaunchNavigation(val latitude: Double, val longitude: Double, val placeName: String)` and `ShowNavigationError(val message: String)` to `TripDetailUiEffect.kt` (`app/src/main/java/com/guidovezzoni/venice/ui/effect/TripDetailUiEffect.kt`)

## 2. NavigationUriBuilder Utility (BDD)

- [x] 2.1 Write test: GIVEN valid latitude, longitude, and a simple place name WHEN `buildGeoUri` is called THEN the result matches `geo:{lat},{lon}?q={lat},{lon}({placeName})` exactly, in `NavigationUriBuilderTest` (new file `app/src/test/java/com/guidovezzoni/venice/ui/util/NavigationUriBuilderTest.kt`)
- [x] 2.2 Write test: GIVEN a place name containing spaces and parentheses (e.g. `"Sant'Angelo (Old Town)"`) WHEN `buildGeoUri` is called THEN the place name segment is percent-encoded and the overall URI remains well-formed, in `NavigationUriBuilderTest`
- [x] 2.3 Implement `NavigationUriBuilder.kt` (new file `app/src/main/java/com/guidovezzoni/venice/ui/util/NavigationUriBuilder.kt`) with a pure `fun buildGeoUri(latitude: Double, longitude: Double, placeName: String): String` function that percent-encodes `placeName` (e.g. via `Uri.encode`) and produces the fixed-format geo URI string, to make 2.1 and 2.2 pass

## 3. ViewModel Navigation Handling (BDD)

- [x] 3.1 Write test: GIVEN a loaded trip with a `PENDING` stop at a known id WHEN `OnNavigateToStopClicked(stopId)` is dispatched THEN `TripDetailUiEffect.LaunchNavigation` is emitted with that stop's persisted `latitude`, `longitude`, and `placeName`, in `TripDetailViewModelTest` (`app/src/test/java/com/guidovezzoni/venice/ui/viewmodel/TripDetailViewModelTest.kt`)
- [x] 3.2 Write test: GIVEN `OnNavigateToStopClicked` is dispatched with a `stopId` that does not match any stop in the current state WHEN the intent is processed THEN no `LaunchNavigation` effect is emitted (no crash, no-op), in `TripDetailViewModelTest`
- [x] 3.3 Implement `OnNavigateToStopClicked` handling in `TripDetailViewModel.onIntent` (`app/src/main/java/com/guidovezzoni/venice/ui/viewmodel/TripDetailViewModel.kt`): resolve the stop by id from current state's stop list and emit `TripDetailUiEffect.LaunchNavigation(latitude, longitude, placeName)` via the existing effect channel, to make 3.1 and 3.2 pass

## 4. Stop Card Navigate Button Visibility and Enablement (BDD)

- [x] 4.1 Write test: GIVEN a stop with `status = StopStatus.PENDING` WHEN `StopSection`/`FilledStop` is rendered with a non-null `onNavigate` lambda THEN the Navigate icon button is displayed, in `StopSectionTest` (`app/src/androidTest/java/com/guidovezzoni/venice/ui/screens/tripdetail/StopSectionTest.kt`)
- [x] 4.2 Write test: GIVEN `onNavigate` is `null` (as computed for a `VISITED` stop) WHEN `StopSection`/`FilledStop` is rendered THEN no Navigate icon button is displayed, in `StopSectionTest`
- [x] 4.3 Write test: GIVEN `isLoading = true` and a non-null `onNavigate` lambda WHEN the Navigate button is rendered THEN the button is disabled; and GIVEN `isLoading = false` THEN the button is enabled, in `StopSectionTest`
- [x] 4.4 Write test: GIVEN a stop with `placeName = "Milan"` and a non-null `onNavigate` lambda WHEN the Navigate button is rendered THEN its content description equals `"Navigate to Milan"`, in `StopSectionTest`
- [x] 4.5 Write test: GIVEN a non-null `onNavigate` lambda WHEN the user taps the Navigate button THEN the lambda is invoked exactly once, in `StopSectionTest`
- [x] 4.6 Implement `onNavigate: (() -> Unit)? = null` parameter on `StopSection` and `FilledStop` composables (`app/src/main/java/com/guidovezzoni/venice/ui/screens/tripdetail/StopSection.kt`), rendering an `IconButton` with `Icons.Filled.Navigation`, `enabled = !isLoading`, and a content description built from the string resource + `placeName`, following the existing `onDelete`/`onMoveUp` null-hides-button convention, to make 4.1–4.5 pass

## 5. Trip Detail Screen Wiring (BDD)

- [x] 5.1 Write test: GIVEN a stop with `status = StopStatus.PENDING` rendered via `TripDetailScreen` WHEN the Navigate button is tapped THEN `TripDetailUiIntent.OnNavigateToStopClicked(stop.id)` is captured by the `onIntent` callback, in `TripDetailScreenTest` (extend existing file `app/src/androidTest/java/com/guidovezzoni/venice/ui/screens/tripdetail/TripDetailScreenTest.kt`)
- [x] 5.2 Write test: GIVEN a stop with `status = StopStatus.VISITED` rendered via `TripDetailScreen` WHEN the screen is composed THEN the Navigate button is not present for that stop's card, in `TripDetailScreenTest`

- [x] 5.3 Implement `onNavigate` wiring at all three `StopSection` call sites in `TripDetailScreen.kt` (`app/src/main/java/com/guidovezzoni/venice/ui/screens/TripDetailScreen.kt`): pass a non-null lambda dispatching `OnNavigateToStopClicked(stop.id)` when `stop.status == StopStatus.PENDING`, and `null` otherwise, to make 5.1 and 5.2 pass

## 6. MainScreen Effect Handling (BDD)

- [x] 6.1 Write test: GIVEN a `TripDetailUiEffect.LaunchNavigation` effect is collected and an activity can resolve the built `ACTION_VIEW` + `geo:` intent WHEN the effect is handled THEN `startActivity` is invoked with an intent whose data URI matches `NavigationUriBuilder.buildGeoUri(...)` for the given coordinates and place name, in `MainScreenTest` (`app/src/androidTest/java/com/guidovezzoni/venice/ui/MainScreenTest.kt` — extend existing file, or create if not present)
- [x] 6.2 Write test: GIVEN a `TripDetailUiEffect.LaunchNavigation` effect is collected and no activity can resolve the intent WHEN the effect is handled THEN a user-friendly error (snackbar) is shown and no `ActivityNotFoundException` propagates, in `MainScreenTest`
- [x] 6.3 Write test: GIVEN a `TripDetailUiEffect.ShowNavigationError` effect is collected WHEN the effect is handled THEN the snackbar displays the effect's `message` text, in `MainScreenTest`
- [x] 6.4 Implement effect handling in `MainScreen.kt`'s effect-collection `LaunchedEffect` (`app/src/main/java/com/guidovezzoni/venice/ui/MainScreen.kt`): for `LaunchNavigation`, build the geo URI via `NavigationUriBuilder.buildGeoUri`, construct `Intent(Intent.ACTION_VIEW, Uri.parse(uri))`, check resolvability via the package manager, and either `startActivity` or show the localized "no app available" error via snackbar; for `ShowNavigationError`, show the given message via snackbar (reusing the existing `ShowError` snackbar path), to make 6.1–6.3 pass

## 7. Integration and Non-Testable Tasks

- [x] 7.1 Add `<queries>` block to `AndroidManifest.xml` (`app/src/main/AndroidManifest.xml`) declaring an `<intent>` with `action android:name="android.intent.action.VIEW"` and `data android:scheme="geo"`, required for `resolveActivity`/`queryIntentActivities` to see navigation apps on API 30+ (targetSdk 36)
- [x] 7.2 Add new string resources to `values/strings.xml`: Navigate content description template (e.g. `nav_action_navigate_content_description` = "Navigate to %1$s") and no-app-available error message (e.g. `nav_action_no_app_available_error`)
- [x] 7.3 Translate the new strings into `values-it/strings.xml`
- [x] 7.4 Translate the new strings into `values-es/strings.xml`
- [x] 7.5 Add/update `@Preview` composables in `StopSection.kt` to cover the new `onNavigate` parameter (non-null on a pending stop preview, null/absent on a visited stop preview), per the project's UiState/parameter preview coverage guideline

## 8. Verification

- [x] 8.1 Run `./gradlew test` and confirm `NavigationUriBuilderTest` and the new `TripDetailViewModelTest` cases pass with the rest of the unit test suite green
- [x] 8.2 Run `./gradlew connectedDebugAndroidTest` and confirm the new `StopSectionTest`, `TripDetailScreenTest`, and `MainScreenTest` cases pass on a connected device/emulator
- [x] 8.3 On a connected device or emulator running API 30+, install the app, open a trip with at least one `PENDING` stop, tap Navigate, and confirm the system app picker (or the single installed navigation app) launches with the correct destination pre-filled — verifying the `<queries>` manifest entry is effective in practice, not just in code
- [x] 8.4 On the same device, verify a `VISITED` stop shows no Navigate button, and that the Navigate button is visibly disabled while `isLoading` is `true` (e.g. during another in-flight action)
- [x] 8.5 Run `./gradlew check` to confirm static analysis and full verification pass
