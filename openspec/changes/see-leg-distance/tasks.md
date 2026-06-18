## 1. LegSummary Compose UI Tests (BDD)

- [ ] 1.1 Write test: GIVEN a leg with distance 750m WHEN LegSummary is rendered THEN "750 m" is displayed — in `LegSummaryTest`
- [ ] 1.2 Write test: GIVEN a leg with distance exactly 1000m WHEN LegSummary is rendered THEN "1.0 km" is displayed — in `LegSummaryTest`
- [ ] 1.3 Write test: GIVEN a leg with distance 12500m WHEN LegSummary is rendered THEN "12.5 km" is displayed — in `LegSummaryTest`
- [ ] 1.4 Write test: GIVEN a leg with distance 5000m and duration 600s WHEN LegSummary is rendered THEN combined text with "5.0 km" and "10 min" is displayed — in `LegSummaryTest`
- [ ] 1.5 Run LegSummary tests and verify all pass (tests are GREEN immediately since production code exists)

## 2. TripDetailScreen Leg Display Tests (BDD)

- [ ] 2.1 Write test: GIVEN uiState with stops and corresponding legs WHEN TripDetailScreen is rendered THEN leg distance text is displayed between stops — in `TripDetailScreenTest`
- [ ] 2.2 Write test: GIVEN uiState with stops but empty legs list WHEN TripDetailScreen is rendered THEN no leg distance text is displayed — in `TripDetailScreenTest`
- [ ] 2.3 Run TripDetailScreen leg tests and verify all pass

## 3. Verification

- [ ] 3.1 Run full unit test suite (`./gradlew test`) — no regressions
- [ ] 3.2 Run full instrumented test suite (`./gradlew connectedDebugAndroidTest`) — all tests pass including new ones
- [ ] 3.3 Verify on device: leg distance is visible between stops after route calculation, and absent when no route has been calculated
