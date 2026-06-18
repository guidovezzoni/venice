## Context

User story 3.1.2 requires test coverage for the leg distance display feature. All production code was delivered in story 3.1.1: `LegSummary` composable with `formatDistance()`, string resources, and `TripDetailScreen` integration. The Definition of Done requires Compose UI tests for distance formatting and LegSummary appearance, which do not yet exist.

`formatDistance()` is a `private @Composable` function inside `LegSummary.kt` that calls `stringResource()`, so it cannot be tested with pure JUnit unit tests. It must be exercised through Compose UI tests that render the composable.

## Goals / Non-Goals

**Goals:**
- Compose UI tests for `LegSummary` covering distance formatting at key thresholds (< 1000 m, = 1000 m, > 1000 m)
- Compose UI tests for leg display in `TripDetailScreen` (present with route data, absent without)
- Satisfy DoD items 2 and 3 of user story 3.1.2

**Non-Goals:**
- Production code changes (none needed)
- Duration formatting tests (already covered by 3.1.1 scope; can be added separately)
- Imperial locale support (explicitly out of scope per user story)

## Decisions

### Test location: androidTest (Compose UI tests)

`formatDistance()` uses `stringResource()` which requires a Compose context. Tests go in `app/src/androidTest/` using `createComposeRule()`, not `app/src/test/`.

**Alternative considered:** Extract formatting logic to a pure function and unit-test it. Rejected because this would require refactoring production code solely for testability, which is unnecessary when Compose UI tests can verify the rendered output directly.

### New test file for LegSummary

`LegSummaryTest.kt` is created as a new file in `app/src/androidTest/.../ui/screens/tripdetail/` following the project convention of one test file per composable. Tests render `LegSummary` in isolation with different `Leg` values and assert the formatted text output.

### Additions to existing TripDetailScreenTest

Leg display integration tests are added to the existing `TripDetailScreenTest.kt` as a new `// region Leg display` section, following the established pattern in that file. Tests verify that `LegSummary` text appears when `uiState.legs` is populated and is absent when `legs` is empty.

## Risks / Trade-offs

- **Instrumented test execution time**: Adding Compose UI tests increases the androidTest suite runtime. Mitigated by keeping tests focused and minimal. The tests are small and render lightweight composables.
- **String resource dependency**: Tests depend on the actual string resource values. If string formats change, tests will need updating. This is acceptable — tests should verify the real output.
