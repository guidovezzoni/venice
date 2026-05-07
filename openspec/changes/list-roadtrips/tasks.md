## 1. Domain & Data Layer

- [ ] 1.1 Add `stopCount: Int = 0` field to `domain/model/Trip.kt`
- [ ] 1.2 Update `data/database/mapper/TripMapper.kt` to map `stopCount = 0` with a `// TODO 1.2.x` comment

## 2. MVI — Intent & ViewModel

- [ ] 2.1 Add `data class OnTripClicked(val tripId: String) : TripListUiIntent()` to `ui/intent/TripListUiIntent.kt`
- [ ] 2.2 Handle `OnTripClicked` in `TripListViewModel.onIntent()` by emitting `TripListUiEffect.NavigateToTripDetail(intent.tripId)`

## 3. New Composables

- [ ] 3.1 Create `ui/screens/triplist/TripListItem.kt` — stateless composable with `tripName`, `stopCount`, `onClick` params; use Material 3 `ListItem` with `pluralStringResource` for stop count; add `PreviewTripListItemWithStops` and `PreviewTripListItemNoStops`
- [ ] 3.2 Create `ui/screens/triplist/TripEmptyState.kt` — stateless composable with `onCreateTripClicked` param; centred layout with title text and CTA button; add `PreviewTripEmptyState`

## 4. Strings

- [ ] 4.1 Remove `trip_list_empty` from `res/values/strings.xml`
- [ ] 4.2 Add `trip_list_empty_title`, `trip_list_empty_action` strings and `trip_list_stop_count` plural resource to `res/values/strings.xml`

## 5. Screen & Navigation Wiring

- [ ] 5.1 Update `TripListScreen` signature to add `onTripClicked: (tripId: String) -> Unit = {}` parameter
- [ ] 5.2 Replace inline `ListItem` in `TripListScreen` with `TripListItem`, passing `tripName`, `stopCount`, and `onClick`
- [ ] 5.3 Replace inline text empty state in `TripListScreen` with `TripEmptyState`, passing `onCreateTripClicked`
- [ ] 5.4 Wire `onTripClicked` in `MainScreen.kt` at the `TripListScreen` call site: `onTripClicked = { tripId -> viewModel.onIntent(TripListUiIntent.OnTripClicked(tripId)) }`

## 6. Tests

- [ ] 6.1 Add test `GIVEN a trip id WHEN OnTripClicked is dispatched THEN NavigateToTripDetail effect is emitted with that tripId` to `TripListViewModelTest`
- [ ] 6.2 Add test `GIVEN an empty trip list WHEN ViewModel is initialised THEN uiState.trips is empty` to `TripListViewModelTest`

## 7. Verification

- [ ] 7.1 Run `./gradlew check` — must pass with no warnings
- [ ] 7.2 Run `./gradlew test` — all tests must pass including the two new ones
