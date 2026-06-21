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

### 1. Imperial locale detection: dual-strategy with API-level gating

**Decision**: Use `android.icu.util.LocaleData.getMeasurementSystem()` on API 28+ to detect imperial measurement systems. On API 24-27, fall back to a hardcoded country-code set `setOf("US", "GB", "LR", "MM")` checked against `locale.country`. Encapsulate this in a `fun isImperialLocale(locale: Locale): Boolean` function in `LegSummary.kt` (superseded by Decision 3 below, which relocates this function to `ui/util/DistanceFormatter.kt`).

**Rationale**: `getMeasurementSystem()` is the platform-canonical way to detect measurement systems and handles edge cases (e.g. user-overridden locale settings) that a static country list cannot. However, it requires API 28, while the app's minSdk is 24. The hardcoded fallback covers the gap for older devices, matching the four countries explicitly named in the acceptance criteria.

**Alternative considered**: Hardcoded country-code set only — simpler but ignores the system API when it is available, missing user-level locale overrides on API 28+ devices.

### 2. Formatter becomes a plain function taking `Locale`, owned by the ViewModel

**Decision**: Replace the private `@Composable fun formatDistance(distanceMetres: Int): String` with a top-level `fun formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String`, resolving strings via `Resources.getString(...)` instead of `stringResource()`. `TripDetailViewModel.buildFormattedDistances()` calls it with `application.resources` and `Locale.getDefault()`, producing a `Map<fromStopId, String>` that `TripDetailUiState` exposes; `LegSummary` receives the pre-computed `formattedDistance: String` as a plain parameter and performs no formatting or locale logic itself.

**Rationale**: Removing the `@Composable` annotation and the `stringResource()` call lets the function be unit-tested directly with JUnit (no Compose test rule, no Robolectric). Moving the call site into the ViewModel (rather than the composable) is required by the project's "composables are purely presentational" guideline (`docs/guidelines/guidelines-android.md`), which forbids locale detection and formatting computations inside composables — the original plan of calling `formatDistance` from inside `LegSummary` via `LocalContext.current.resources` would have violated that rule.

**Alternative considered**: Keep it `@Composable` and rely on Compose UI tests only. Rejected because it would push all boundary/rounding cases (metres/km/mi thresholds) into slower instrumented tests instead of fast JUnit unit tests, contradicting the project's unit-test-first testing guidelines.

### 3. Formatting function extracted to `ui/util/DistanceFormatter.kt`

**Decision**: Extract `formatDistance` and `isImperialLocale` into a new top-level utility file `app/src/main/java/com/guidovezzoni/venice/ui/util/DistanceFormatter.kt`, as public (non-`internal`) top-level functions, rather than keeping them in `LegSummary.kt`.

**Rationale**: Once the call site moved from the composable to `TripDetailViewModel` (Decision #2), the functions could no longer live in `LegSummary.kt` as file-private/internal helpers — they needed to be called from `ui/viewmodel/TripDetailViewModel.kt`, a different package. The project's "testable helper functions get their own file under `ui/util/`" guideline applies directly once the function has a call site outside the composable file. Visibility is left as the Kotlin default (public) since the single `:app` module has no module boundary requiring `internal`; this is a relaxation of the original `internal`-only intent, tracked as a non-blocking follow-up.

### 4. Miles conversion constant

**Decision**: Add `private const val METRES_PER_MILE = 1609.344` and compute `distanceMetres / METRES_PER_MILE` for the imperial branch.

**Rationale**: Standard international mile conversion factor, sufficient precision for one-decimal-place display.

### 5. Spec reconciliation approach

**Decision**: Use a `MODIFIED Requirements` delta against `route-display`'s "LegSummary composable displays leg information" requirement, rewriting it to match the actual `LegSummary(modifier: Modifier = Modifier, leg: Leg)` signature, the actual duration string format (`%1$dh %2$dmin`), and adding the new imperial-distance scenarios. The preview-coverage requirement gets a `MODIFIED` update to require the new imperial-locale preview. The string-resources requirement gets a `MODIFIED` update adding `trip_detail_leg_distance_miles`.

**Rationale**: All three existing requirements need their scenario/content updated (signature, format, and new resource) rather than being purely additive, so `MODIFIED` is the correct delta operation per OpenSpec conventions (vs. `ADDED`, which would duplicate/conflict with the existing requirement text at archive time).

## Risks / Trade-offs

- **[Risk] Public (non-`internal`) visibility on `formatDistance`/`isImperialLocale` widens accessibility beyond the original `internal`-only intent** → Acceptable for now: the single `:app` module has no module boundary to protect; tracked as a follow-up to revisit `internal` visibility once the formatter has a stable single call site in `TripDetailViewModel`.
- **[Risk] Hardcoded 4-country fallback (API 24-27) may miss other imperial-leaning locales** → Mitigated: on API 28+ the platform API handles this correctly. The hardcoded set only applies to the shrinking population of API 24-27 devices and covers the four most significant imperial countries.
- **[Trade-off] No feet-based sub-mile formatting** → Simpler implementation and consistent unit display; acceptable per user clarification (always show miles, e.g. "0.3 mi").
- **[Trade-off] Passing `Resources` instead of relying on `stringResource()`** → Slightly more verbose call site inside the composable, but unlocks fast JUnit coverage for all rounding/threshold cases.
