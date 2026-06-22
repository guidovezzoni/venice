## Why

`LegSummary` (delivered in story 3.1.1) already shows each leg's distance, but only in metric units (metres/kilometres). Users in imperial-unit locales (US, UK, Liberia, Myanmar) expect distances in miles. Story 3.1.2 closes this gap with locale-aware formatting, completing the acceptance criteria for "see leg distance".

## What Changes

- A new `ui/util/DistanceFormatter.kt` gains a locale-aware distance formatter: imperial-unit locales display distance in miles with one decimal place (e.g. "7.8 mi"); all other locales keep the existing metric behaviour (metres below 1000 m, kilometres with one decimal place at or above 1000 m). Imperial detection uses `android.icu.util.LocaleData.getMeasurementSystem()` on API 28+, with a hardcoded country-code fallback (`US`, `GB`, `LR`, `MM`) on API 24-27.
- The formatter is a plain (non-`@Composable`) top-level function taking `distanceMetres: Int` and `locale: Locale`, so it is unit-testable without a Compose test rule. `TripDetailViewModel.buildFormattedDistances()` calls it with `Locale.getDefault()` and exposes the result via `TripDetailUiState`; `LegSummary` receives the pre-computed string as a plain parameter, keeping the composable purely presentational per project guidelines.
- New string resource `trip_detail_leg_distance_miles` ("%.1f mi") added to `values/`, `values-it/`, `values-es-rES/` `strings.xml`.
- `LegSummary` previews extended with an imperial-locale variant.
- No domain or data layer changes; `Leg.distanceMetres: Int` and the "hide LegSummary when no Leg exists" behaviour (AC #5) are unchanged.

## Capabilities

### New Capabilities

### Modified Capabilities
- `route-display`: `LegSummary` requirement updated to (a) reconcile the documented composable signature (`leg: Leg`, not `distanceMetres`/`durationSeconds` directly) and duration format (`%1$dh %2$dmin`, not `%1$d h %2$d min`) with the actual implementation, and (b) add imperial-unit (miles) distance formatting for US/UK/Liberia/Myanmar locales alongside the existing metric formatting.

## Impact

- New file: `app/src/main/java/com/guidovezzoni/venice/ui/util/DistanceFormatter.kt` — `formatDistance` and `isImperialLocale` top-level functions taking `Locale`; imperial branch added
- `app/src/main/java/com/guidovezzoni/venice/ui/screens/tripdetail/LegSummary.kt` — composable now accepts a pre-computed `formattedDistance: String` parameter instead of calling `formatDistance` itself; previews updated
- `app/src/main/java/com/guidovezzoni/venice/ui/viewmodel/TripDetailViewModel.kt` — adds `buildFormattedDistances()`, calling `formatDistance` per leg and exposing the result via `TripDetailUiState`
- `app/src/main/java/com/guidovezzoni/venice/ui/state/TripDetailUiState.kt` — adds `formattedLegDistances: Map<String, String>` carrying the per-leg formatted distance, keyed by `fromStopId`
- `app/src/main/res/values/strings.xml`, `values-it/strings.xml`, `values-es-rES/strings.xml` — new `trip_detail_leg_distance_miles` string
- New test file: `app/src/test/java/com/guidovezzoni/venice/ui/screens/tripdetail/LegSummaryFormatDistanceTest.kt` — unit tests for metric/imperial/boundary formatting
- `app/src/androidTest/.../LegSummaryTest.kt` (new or extended) — Compose UI test verifying distance text renders between stops
- `openspec/specs/route-display/spec.md` — delta to reconcile signature/duration-format drift and add imperial formatting requirement
- No new dependencies, no domain/data layer changes
