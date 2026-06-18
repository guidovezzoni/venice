## Why

User story 3.1.2 ("See Distance for Each Leg") requires test coverage to satisfy its Definition of Done. The production code — domain model, database entity, API mapper, LegSummary composable with `formatDistance()`, string resources, and TripDetailScreen integration — was fully implemented in story 3.1.1. However, no Compose UI tests exist for `LegSummary`, and `TripDetailScreenTest` has no tests verifying leg display. The DoD requires unit tests for distance formatting and Compose UI tests for LegSummary appearance.

## What Changes

- Add a new `LegSummaryTest` Compose UI test file covering distance formatting thresholds (below 1000 m, exactly 1000 m, above 1000 m) and composable rendering
- Add leg display tests to the existing `TripDetailScreenTest` (legs shown when route data exists, legs absent when no route calculated)

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `route-display`: Adding test coverage requirements for LegSummary composable and leg display in TripDetailScreen

## Impact

- New test file: `app/src/androidTest/.../ui/screens/tripdetail/LegSummaryTest.kt`
- Modified test file: `app/src/androidTest/.../ui/screens/tripdetail/TripDetailScreenTest.kt`
- No production code changes
- No dependency changes
