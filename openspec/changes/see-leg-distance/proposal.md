## Why

`LegSummary` (delivered in story 3.1.1) already shows each leg's distance, but only in metric units (metres/kilometres). Users in imperial-unit locales (US, UK, Liberia, Myanmar) expect distances in miles. Story 3.1.2 closes this gap with locale-aware formatting, completing the acceptance criteria for "see leg distance".

## What Changes

- `LegSummary.kt` gains a locale-aware distance formatter: imperial-unit locales (country code `US`, `GB`, `LR`, `MM`) display distance in miles with one decimal place (e.g. "7.8 mi"); all other locales keep the existing metric behaviour (metres below 1000 m, kilometres with one decimal place at or above 1000 m).
- The formatter becomes a plain (non-`@Composable`) top-level internal function taking `distanceMetres: Int` and `locale: Locale`, so it is unit-testable without a Compose test rule. The composable passes `Locale.getDefault()`.
- New string resource `trip_detail_leg_distance_miles` ("%.1f mi") added to `values/`, `values-it/`, `values-es-rES/` `strings.xml`.
- `LegSummary` previews extended with an imperial-locale variant.
- No domain or data layer changes; `Leg.distanceMetres: Int` and the "hide LegSummary when no Leg exists" behaviour (AC #5) are unchanged.

## Capabilities

### New Capabilities

### Modified Capabilities
- `route-display`: `LegSummary` requirement updated to (a) reconcile the documented composable signature (`leg: Leg`, not `distanceMetres`/`durationSeconds` directly) and duration format (`%1$dh %2$dmin`, not `%1$d h %2$d min`) with the actual implementation, and (b) add imperial-unit (miles) distance formatting for US/UK/Liberia/Myanmar locales alongside the existing metric formatting.

## Impact

- `app/src/main/java/com/guidovezzoni/venice/ui/screens/tripdetail/LegSummary.kt` — extract `formatDistance` to a non-Composable internal function taking `Locale`; add imperial branch; update previews
- `app/src/main/res/values/strings.xml`, `values-it/strings.xml`, `values-es-rES/strings.xml` — new `trip_detail_leg_distance_miles` string
- New test file: `app/src/test/java/com/guidovezzoni/venice/ui/screens/tripdetail/LegSummaryFormatDistanceTest.kt` — unit tests for metric/imperial/boundary formatting
- `app/src/androidTest/.../LegSummaryTest.kt` (new or extended) — Compose UI test verifying distance text renders between stops
- `openspec/specs/route-display/spec.md` — delta to reconcile signature/duration-format drift and add imperial formatting requirement
- No new dependencies, no domain/data layer changes
