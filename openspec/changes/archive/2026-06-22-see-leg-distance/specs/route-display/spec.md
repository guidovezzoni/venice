## MODIFIED Requirements

### Requirement: LegSummary composable displays leg information
The UI layer SHALL define a `LegSummary` composable in `ui/screens/tripdetail/LegSummary.kt` that:
- Accepts `modifier: Modifier = Modifier`, `leg: Leg`, and `formattedDistance: String`.
- Renders `formattedDistance` as-is alongside a formatted duration; it performs no locale detection, unit conversion, or string-resource resolution itself, per the project's "composables are purely presentational" guideline.
- Displays a formatted duration derived from `leg.durationSeconds`: minutes if < 60 minutes, otherwise hours and minutes (e.g., "5 min" or "1h 30min").
- Uses a compact, visually lightweight style (smaller text, secondary colour) to sit naturally between stop cards.

The UI layer SHALL define `ui/util/DistanceFormatter.kt` with two top-level functions, called from `TripDetailViewModel.buildFormattedDistances()` (not from the composable):
- `formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String`, which derives a formatted distance from `distanceMetres` and `locale`:
  - For metric-unit locales: metres if < 1000, otherwise kilometres with one decimal place (e.g., "450 m" or "12.3 km").
  - For imperial-unit locales: miles with one decimal place, regardless of distance magnitude (e.g., "7.8 mi" or "0.3 mi").
- `isImperialLocale(locale: Locale): Boolean`, which uses `android.icu.util.LocaleData.getMeasurementSystem()` on API 28+ and falls back to a hardcoded country-code set (`US`, `GB`, `LR`, `MM`) on API 24-27.

`TripDetailViewModel.buildFormattedDistances()` SHALL compute a `Map<fromStopId, String>` by calling `formatDistance(leg.distanceMetres, Locale.getDefault(), application.resources)` for each leg, and expose it via `TripDetailUiState.formattedLegDistances` so `TripDetailScreen` can pass the pre-computed string into `LegSummary`.

#### Scenario: Short distance display (metric)
- **WHEN** `formatDistance` is called with `distanceMetres = 450` and a metric-unit locale, and the result is passed as `formattedDistance` with `leg.durationSeconds = 300`
- **THEN** `LegSummary` displays the distance as "450 m" and the duration as "5 min"

#### Scenario: Long distance display (metric)
- **WHEN** `formatDistance` is called with `distanceMetres = 12345` and a metric-unit locale, and the result is passed as `formattedDistance` with `leg.durationSeconds = 5400`
- **THEN** `LegSummary` displays the distance as "12.3 km" and the duration as "1h 30min"

#### Scenario: Imperial-locale distance display
- **WHEN** `formatDistance` is called with `distanceMetres = 12545` and `isImperialLocale` returns `true` for the locale
- **THEN** it returns "7.8 mi"

#### Scenario: Imperial-locale short distance display
- **WHEN** `formatDistance` is called with `distanceMetres = 483` (sub-mile) and `isImperialLocale` returns `true` for the locale
- **THEN** it returns "0.3 mi" (miles are always used for imperial locales; no feet-based formatting)

#### Scenario: Distance rounding
- **WHEN** `formatDistance` is called with `distanceMetres = 12345` (metric) or an equivalent imperial value
- **THEN** the returned value uses standard rounding to one decimal place (e.g. "12.3 km", not "12 km")

#### Scenario: Duration less than 1 minute
- **WHEN** `LegSummary` is rendered with `leg.durationSeconds = 45`
- **THEN** the duration is displayed as "1 min" (rounded up to minimum 1 minute)

#### Scenario: Duration exactly 60 minutes
- **WHEN** `LegSummary` is rendered with `leg.durationSeconds = 3600`
- **THEN** the duration is displayed as "1h 0min"

### Requirement: LegSummary preview coverage
`LegSummary` SHALL have previews covering:
- Short distance (metres) with short duration (minutes only), metric locale
- Long distance (kilometres) with long duration (hours and minutes), metric locale
- An imperial-unit locale variant showing distance in miles

#### Scenario: Previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** at least three preview variants are visible, including one imperial-locale variant

### Requirement: Route calculation string resources
The app SHALL define the following string resources:
- `trip_detail_calculate_route`: "Calculate route" — button label
- `trip_detail_calculating_route`: "Calculating route…" — loading description
- `trip_detail_route_error`: error message shown when route calculation fails
- `trip_detail_leg_distance_metres`: "%1$d m" — distance in metres
- `trip_detail_leg_distance_kilometres`: "%.1f km" — distance in kilometres
- `trip_detail_leg_distance_miles`: "%.1f mi" — distance in miles, for imperial-unit locales
- `trip_detail_leg_duration_minutes`: "%1$d min" — duration in minutes only
- `trip_detail_leg_duration_hours_minutes`: "%1$dh %2$dmin" — duration in hours and minutes

#### Scenario: String resources defined
- **WHEN** the app is built
- **THEN** all route calculation string resources, including `trip_detail_leg_distance_miles`, resolve without errors in all supported locales (English, Italian, Spanish)
