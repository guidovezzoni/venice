## 1. Prerequisites — Domain & Data

- [x] 1.1 Add `stopCount: Int = 0` field to `domain/model/Trip.kt`
- [x] 1.2 Update `data/database/mapper/TripMapper.kt` to map `stopCount = 0` with a `// TODO 1.2.x` comment

## 2. ViewModel — OnTripClicked (BDD)

- [x] 2.1 Write test: `GIVEN a trip id WHEN OnTripClicked is dispatched THEN NavigateToTripDetail effect is emitted with that tripId` in `TripListViewModelTest`
- [x] 2.2 Add `data class OnTripClicked(val tripId: String) : TripListUiIntent()` to `TripListUiIntent.kt`
- [x] 2.3 Handle `OnTripClicked` in `TripListViewModel.onIntent()` by emitting `NavigateToTripDetail(intent.tripId)`

## 3. ViewModel — Empty List State (BDD)

- [x] 3.1 Write test: `GIVEN an empty trip list WHEN ViewModel is initialised THEN uiState.trips is empty` in `TripListViewModelTest`

## 4. New Composables

- [x] 4.1 Create `ui/screens/triplist/TripListItem.kt` — stateless composable with `tripName`, `stopCount`, `onClick` params; use Material 3 `ListItem` with `pluralStringResource` for stop count; add `PreviewTripListItemWithStops` and `PreviewTripListItemNoStops`
- [x] 4.2 Create `ui/screens/triplist/TripEmptyState.kt` — stateless composable with `onCreateTripClicked` param; centred layout with title text and CTA button; add `PreviewTripEmptyState`

## 5. Strings & Resources

- [x] 5.1 Remove `trip_list_empty` from `res/values/strings.xml`
- [x] 5.2 Add `trip_list_empty_title`, `trip_list_empty_action` strings and `trip_list_stop_count` plural resource to `res/values/strings.xml`

## 6. Screen & Navigation Wiring

- [x] 6.1 Update `TripListScreen` signature to add `onTripClicked: (tripId: String) -> Unit = {}` parameter
- [x] 6.2 Replace inline `ListItem` in `TripListScreen` with `TripListItem`, passing `tripName`, `stopCount`, and `onClick`
- [x] 6.3 Replace inline text empty state in `TripListScreen` with `TripEmptyState`, passing `onCreateTripClicked`
- [x] 6.4 Wire `onTripClicked` in `MainScreen.kt` at the `TripListScreen` call site: `onTripClicked = { tripId -> viewModel.onIntent(TripListUiIntent.OnTripClicked(tripId)) }`

## 7. Verification

- [x] 7.1 Run `./gradlew check` — must pass with no warnings
- [x] 7.2 Run `./gradlew test` — all tests must pass including the two new ones
