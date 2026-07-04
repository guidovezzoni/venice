## Context

Story 3.1.2 already moved distance formatting out of `LegSummary` into `ui/util/DistanceFormatter.kt`, computed once per leg in `TripDetailViewModel.buildFormattedDistances()` and exposed via `TripDetailUiState.formattedLegDistances`. Duration formatting was left behind as a private `@Composable formatDuration(durationSeconds: Int): String` function inside `LegSummary.kt`, using `stringResource(...)` directly and two file-level constants (`SECONDS_PER_MINUTE`, `MINUTES_PER_HOUR`). This is the last piece of formatting logic still living inside a composable in this screen, and it blocks unit-testing the duration arithmetic in isolation.

The existing composable logic (`minutes = durationSeconds / 60`; hours format if `minutes >= 60`) already truncates rather than rounds, so no behavioural change is needed — only a structural one: extract, pre-compute, inject.

## Goals / Non-Goals

**Goals:**
- Extract duration formatting into a plain (non-`@Composable`) top-level function `formatDuration(durationSeconds: Int, resources: Resources): String` in `ui/util/DurationFormatter.kt`, unit-testable via mocked `Resources` (mirroring `formatDistance`).
- Pre-compute a `Map<fromStopId, String>` of formatted durations in `TripDetailViewModel`, exposed via a new `TripDetailUiState.formattedLegDurations` field.
- Make `LegSummary` receive `formattedDuration: String` as a plain parameter, with zero formatting/locale/resource logic inside the composable.
- Preserve the exact current arithmetic (truncate to whole minutes, then branch on the 60-minute threshold) so there is no user-visible behavioural change.

**Non-Goals:**
- No change to the visual layout, icon, or text style of `LegSummary`.
- No change to distance formatting (already compliant from 3.1.2).
- No locale-sensitive duration formatting — time units (`min`, `h`) are treated as universal, per AC 7 of the story.
- No new string resources — `trip_detail_leg_duration_minutes` and `trip_detail_leg_duration_hours_minutes` already exist in all three locale files.

## Decisions

**1. Function signature: `formatDuration(durationSeconds: Int, resources: Resources): String` — no `Locale` parameter.**
Unlike `formatDistance`, which needs `Locale` to decide metric vs. imperial units, duration is expressed uniformly as hours/minutes regardless of locale (AC 7). Adding an unused `Locale` parameter would be dead weight and would misleadingly imply the output varies by locale. Alternative considered: match `formatDistance`'s signature exactly for consistency — rejected because it would carry an unused parameter, which the Kotlin guidelines' "no dead code" spirit discourages.

**2. Truncate-then-branch order preserved exactly.**
The current composable does `minutes = durationSeconds / SECONDS_PER_MINUTE` (integer division, truncates) and only then checks `minutes >= MINUTES_PER_HOUR`. This means 3599 seconds → 59 minutes → "59 min" (not "0h 59min" and not "1h 0min"), and 89 seconds → 1 minute → "1 min". The new utility function must replicate this exact order of operations — truncate first, branch second — rather than e.g. computing hours and minutes via `durationSeconds / 3600` and `(durationSeconds % 3600) / 60`, which would produce identical results for whole-hour-aligned inputs but was explicitly confirmed by the user as the correct approach to avoid subtle rounding differences at boundary values.

**3. Constants move with the logic.**
`SECONDS_PER_MINUTE` and `MINUTES_PER_HOUR` move from `LegSummary.kt` to `DurationFormatter.kt` as private top-level constants, since they are now solely used by `formatDuration`. `LegSummary.kt` no longer needs them once its private `formatDuration` composable is deleted.

**4. ViewModel wiring mirrors `buildFormattedDistances` exactly.**
`buildFormattedDurations(legs: List<Leg>): Map<String, String>` follows the identical shape: `legs.associate { leg -> leg.fromStopId to formatDuration(leg.durationSeconds, application.resources) }`. It is called from the same `observeLegsUseCase(tripId).onEach { legs -> ... }` block that already computes `formattedLegDistances`, so both maps are recomputed together whenever legs change — no new Flow collection or lifecycle concern introduced.

**5. Test file placement: `ui/util/DurationFormatterTest.kt`.**
The existing `formatDistance` test (`LegSummaryFormatDistanceTest.kt`) lives under `ui/screens/tripdetail/` rather than mirroring its source file's `ui/util/` location — an inconsistency from 3.1.2. Per explicit user clarification, the new test is placed at the location matching its source file (`ui/util/DurationFormatterTest.kt`), correcting this placement going forward rather than perpetuating the mismatch. This is a one-off deviation from copy-the-existing-pattern in favour of correctness.

## Risks / Trade-offs

- **[Risk] Unmocked MockK resource-string calls in `TripDetailViewModelTest`** → Once `buildFormattedDurations` is wired into the ViewModel's `init` block, any existing test that emits non-empty legs through `observeLegsUseCase` will invoke the now-unmocked duration string resources and throw. **Mitigation**: add `every { mockResources.getString(R.string.trip_detail_leg_duration_minutes, any()) }` and the two-arg hours-minutes variant to `TripDetailViewModelTest.setUp()` before wiring, per the same pattern already used for the three distance string IDs.
- **[Risk] Preview coverage gap** → Adding `formattedLegDurations` to `TripDetailUiState` requires at least one non-default value across the screen's previews per the project's UiState preview coverage rule. **Mitigation**: update `PreviewTripDetailScreenWithLegs` (or equivalent) to pass a non-empty `formattedLegDurations` map alongside the existing `formattedLegDistances`.
- **[Risk] Androidtest breakage** → `LegSummaryTest.kt` (androidTest) currently asserts on text produced by the composable's internal formatting (`"750 m · 9 min"`) without passing a duration explicitly. Removing the internal formatting will break this test's `setContent` helper signature. **Mitigation**: update the test's `setContent` to accept and pass `formattedDuration` explicitly, matching the existing `formattedDistance` parameter pattern.

## Migration Plan

No data migration required — this is a UI-layer refactor with no persistence or API changes. Rollout is a single-branch code change with no phased steps; no rollback beyond a normal revert is needed since behaviour is unchanged.

## Open Questions

None — all ambiguities were resolved via user clarification before this design was written (function signature, truncation order, zero-duration handling, test file placement).
