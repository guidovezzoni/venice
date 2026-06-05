## 1. Prerequisites

- [x] 1.1 Add string resource `trip_detail_progress_summary` (`%1$d of %2$d stops completed`) to `res/values/strings.xml`
- [x] 1.2 Add Italian translation to `res/values-it/strings.xml`: `%1$d di %2$d tappe completate`
- [x] 1.3 Add Spanish translation to `res/values-es-rES/strings.xml`: `%1$d de %2$d paradas completadas`

## 2. TripProgressSummary Composable (BDD)

- [x] 2.1 Write test: GIVEN a TripProgressSummary with departedCount=3 and totalCount=6 WHEN rendered THEN displays "3 of 6 stops completed" and progress indicator at 0.5f in `TripProgressSummaryTest`
- [x] 2.2 Write test: GIVEN a TripProgressSummary with departedCount=0 and totalCount=4 WHEN rendered THEN displays "0 of 4 stops completed" and progress indicator at 0.0f in `TripProgressSummaryTest`
- [x] 2.3 Write test: GIVEN a TripProgressSummary with departedCount=6 and totalCount=6 WHEN rendered THEN displays "6 of 6 stops completed" and progress indicator at 1.0f in `TripProgressSummaryTest`
- [x] 2.4 Implement `TripProgressSummary` composable in `ui/screens/tripdetail/TripProgressSummary.kt` with text, `LinearProgressIndicator`, and previews for all states (zero, partial, complete progress)

## 3. StopSection Departed Checkmark Icon (BDD)

- [x] 3.1 Write test: GIVEN a StopSection with stopDisplayState=DEPARTED WHEN rendered THEN displays a checkmark icon (content description "Departed") in `StopSectionTest`
- [x] 3.2 Write test: GIVEN a StopSection with stopDisplayState=UPCOMING WHEN rendered THEN displays the caller-provided icon (no checkmark) in `StopSectionTest`
- [x] 3.3 Implement checkmark icon override in `FilledStop` in `StopSection.kt`: when `stopDisplayState == DEPARTED`, replace the icon with `Icons.Filled.CheckCircle`

## 4. StopSection Current Stop Border (BDD)

- [x] 4.1 Write test: GIVEN a StopSection with stopDisplayState=CURRENT WHEN rendered THEN the Card has a primary-colour border in `StopSectionTest`
- [x] 4.2 Write test: GIVEN a StopSection with stopDisplayState=UPCOMING WHEN rendered THEN the Card has no highlighted border in `StopSectionTest`
- [x] 4.3 Implement primary border on Card in `FilledStop` in `StopSection.kt`: apply `Modifier.border(2.dp, primary, CardDefaults.shape)` when `stopDisplayState == CURRENT`

## 5. TripDetailScreen Progress Summary Integration (BDD)

- [x] 5.1 Write test: GIVEN a TripDetailScreen with 4 stops and 1 departed WHEN rendered THEN progress summary displays "1 of 4 stops completed" in `TripDetailScreenTest`
- [x] 5.2 Write test: GIVEN a TripDetailScreen with no stops WHEN rendered THEN progress summary is not displayed in `TripDetailScreenTest`
- [x] 5.3 Implement progress summary integration in `TripDetailScreen.kt`: compute departedCount/totalCount from allStops, add `TripProgressSummary` as first LazyColumn item when totalCount > 0

## 6. StopSection Preview Updates

- [x] 6.1 Update `StopSection.kt` previews to cover the new visual states: departed with checkmark, current with border

## 7. Verification

- [x] 7.1 Run `./gradlew check` and confirm all checks pass
- [x] 7.2 Run Compose UI tests on device with `./gradlew connectedDebugAndroidTest`
- [x] 7.3 On-device verification: visually confirm departed checkmark, current border, and progress summary render correctly across all stop states
