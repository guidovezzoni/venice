## Context

`LegSummary` (story 3.1.1) renders a compact `Icon + "<distance> · <duration>"` row between consecutive stops, sourced from `TripDetailUiState.legs` via `ObserveLegsUseCase`. The current `formatDistance()` is a private `@Composable` function inside `LegSummary.kt` that resolves string resources directly — it has no metric/imperial branch and cannot be unit-tested without a Compose rule (it calls `stringResource()`).

There is no locale-detection utility anywhere in the codebase; this is a green-field addition scoped entirely to `LegSummary.kt`. No domain or data model change is required: `Leg.distanceMetres: Int` already carries everything needed.

## Goals / Non-Goals

**Goals:**
- Display distance in miles (one decimal place) for imperial-unit locales: US, UK, Liberia, Myanmar.
- Keep existing metric behaviour unchanged for all other locales (metres < 1000 m, kilometres with one decimal place >= 1000 m).
- Make the distance-formatting logic unit-testable in isolation from Compose.
- Reconcile the `route-display` spec, which currently documents a stale `LegSummary(distanceMetres: Int, durationSeconds: Int)` signature and a stale duration string format, with the real implementation.

**Non-Goals:**
- No change to `Leg` domain model or `LegEntity` persistence (`distanceMetres` stays in metres regardless of display locale).
- No "unavailable distance" placeholder — AC #5 keeps the current behaviour of hiding `LegSummary` entirely when no `Leg` exists for a `fromStopId`.
- No feet-based short-distance formatting — imperial locales always show miles, even for sub-mile distances (e.g. "0.3 mi").
- No dynamic/user-selectable unit preference — units are derived solely from device locale.
- No change to duration formatting beyond what's needed to reconcile spec drift (no behavioural change to duration display).

## Decisions

### 1. Imperial locale detection: hardcoded country-code set

**Decision**: Define `private val IMPERIAL_UNIT_COUNTRY_CODES = setOf("US", "GB", "LR", "MM")` in `LegSummary.kt` and check `locale.country` against it.

**Rationale**: Simple, deterministic, works from API 24 onward (no dependency on `LocaleData`/`UnitSystem` APIs that require higher API levels). Matches the four countries explicitly named in the acceptance criteria (US, UK, Liberia, Myanmar).

**Alternative considered**: `android.icu.util.LocaleData.getMeasurementSystem()` — rejected because it requires API 28+ and the app's min SDK is 24.

### 2. Formatter becomes a plain function taking `Locale`, not `@Composable`

**Decision**: Replace the private `@Composable fun formatDistance(distanceMetres: Int): String` with `internal fun formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String`, resolving strings via `Resources.getString(...)` instead of `stringResource()`. The composable calls it with `LocalContext.current.resources` and `Locale.getDefault()`.

**Rationale**: Removing the `@Composable` annotation and the `stringResource()` call lets the function be unit-tested directly with JUnit (no Compose test rule, no Robolectric). This matches the project's general preference for testable, side-effect-light functions and the user's clarification that the function should take `Locale` as a parameter for testability.

**Alternative considered**: Keep it `@Composable` and rely on Compose UI tests only. Rejected because it would push all boundary/rounding cases (metres/km/mi thresholds) into slower instrumented tests instead of fast JUnit unit tests, contradicting the project's unit-test-first testing guidelines.

### 3. Formatting function stays in `LegSummary.kt`, not a separate utility file

**Decision**: Keep `formatDistance` as a top-level `internal` function in `LegSummary.kt` (per user clarification), rather than extracting a `DistanceFormatter` utility class/file.

**Rationale**: The function is small, has a single call site, and extracting it would add an extra file/abstraction for no current reuse benefit. `internal` visibility (instead of `private`) is required so the new unit test class — which lives in the same module but a different file — can call it directly.

### 4. Miles conversion constant

**Decision**: Add `private const val METRES_PER_MILE = 1609.344` and compute `distanceMetres / METRES_PER_MILE` for the imperial branch.

**Rationale**: Standard international mile conversion factor, sufficient precision for one-decimal-place display.

### 5. Spec reconciliation approach

**Decision**: Use a `MODIFIED Requirements` delta against `route-display`'s "LegSummary composable displays leg information" requirement, rewriting it to match the actual `LegSummary(modifier: Modifier = Modifier, leg: Leg)` signature, the actual duration string format (`%1$dh %2$dmin`), and adding the new imperial-distance scenarios. The preview-coverage requirement gets a `MODIFIED` update to require the new imperial-locale preview. The string-resources requirement gets a `MODIFIED` update adding `trip_detail_leg_distance_miles`.

**Rationale**: All three existing requirements need their scenario/content updated (signature, format, and new resource) rather than being purely additive, so `MODIFIED` is the correct delta operation per OpenSpec conventions (vs. `ADDED`, which would duplicate/conflict with the existing requirement text at archive time).

## Risks / Trade-offs

- **[Risk] `internal` visibility on `formatDistance` slightly widens its accessibility beyond the file** → Acceptable: `internal` is module-scoped, not public API, and is the minimum visibility needed for direct unit testing without Compose.
- **[Risk] Hardcoded 4-country imperial set may miss other imperial-leaning locales (e.g. Caribbean nations)** → Acceptable per explicit acceptance criteria scope (US, UK, Liberia, Myanmar only); documented as a hardcoded set so future maintainers can extend it deliberately.
- **[Trade-off] No feet-based sub-mile formatting** → Simpler implementation and consistent unit display; acceptable per user clarification (always show miles, e.g. "0.3 mi").
- **[Trade-off] Passing `Resources` instead of relying on `stringResource()`** → Slightly more verbose call site inside the composable, but unlocks fast JUnit coverage for all rounding/threshold cases.
