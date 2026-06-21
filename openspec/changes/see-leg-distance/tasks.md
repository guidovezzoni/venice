## 1. Prerequisites

- [ ] 1.1 Add string resource `trip_detail_leg_distance_miles` (`%.1f mi`) to `app/src/main/res/values/strings.xml`
- [ ] 1.2 Add Italian translation to `app/src/main/res/values-it/strings.xml`: `%.1f mi`
- [ ] 1.3 Add Spanish translation to `app/src/main/res/values-es-rES/strings.xml`: `%.1f mi`
- [ ] 1.4 In `LegSummary.kt`, define `private val IMPERIAL_UNIT_COUNTRY_CODES = setOf("US", "GB", "LR", "MM")` and `private const val METRES_PER_MILE = 1609.344`. Define `internal fun isImperialLocale(locale: Locale): Boolean` that uses `android.icu.util.LocaleData.getMeasurementSystem()` on API 28+ and falls back to checking `locale.country in IMPERIAL_UNIT_COUNTRY_CODES` on API 24-27

## 2. Locale-Aware Distance Formatting (BDD)

- [ ] 2.1 Write test: GIVEN distanceMetres=450 and a metric locale (e.g. Locale.ITALY) WHEN formatDistance is called THEN it returns "450 m" in `LegSummaryFormatDistanceTest`
- [ ] 2.2 Write test: GIVEN distanceMetres=12345 and a metric locale WHEN formatDistance is called THEN it returns "12.3 km" (standard rounding, not truncation) in `LegSummaryFormatDistanceTest`
- [ ] 2.3 Write test: GIVEN distanceMetres=999 and a metric locale WHEN formatDistance is called THEN it returns "999 m" (boundary just below 1000 m) in `LegSummaryFormatDistanceTest`
- [ ] 2.4 Write test: GIVEN distanceMetres=1000 and a metric locale WHEN formatDistance is called THEN it returns "1.0 km" (boundary exactly at 1000 m) in `LegSummaryFormatDistanceTest`
- [ ] 2.5 Write test: GIVEN distanceMetres=12545 and an imperial locale (Locale.US) WHEN formatDistance is called THEN it returns "7.8 mi" in `LegSummaryFormatDistanceTest`
- [ ] 2.6 Write test: GIVEN distanceMetres=483 (sub-mile) and an imperial locale (Locale.UK / country "GB") WHEN formatDistance is called THEN it returns "0.3 mi" (no feet-based formatting) in `LegSummaryFormatDistanceTest`
- [ ] 2.7 Write test: GIVEN distanceMetres=12545 and locale country "LR" (Liberia) WHEN formatDistance is called THEN it returns "7.8 mi" in `LegSummaryFormatDistanceTest`
- [ ] 2.8 Write test: GIVEN distanceMetres=12545 and locale country "MM" (Myanmar) WHEN formatDistance is called THEN it returns "7.8 mi" in `LegSummaryFormatDistanceTest`
- [ ] 2.9 Write test: GIVEN a metric locale (e.g. Locale.FRANCE) WHEN isImperialLocale is called THEN it returns false in `LegSummaryFormatDistanceTest`
- [ ] 2.10 Write test: GIVEN an imperial locale (Locale.US) WHEN isImperialLocale is called THEN it returns true in `LegSummaryFormatDistanceTest`
- [ ] 2.11 Implement: convert `formatDistance` in `LegSummary.kt` from a private `@Composable` function to an `internal` plain function `formatDistance(distanceMetres: Int, locale: Locale, resources: Resources): String` that branches on `isImperialLocale(locale)` (miles via `trip_detail_leg_distance_miles`) vs. the existing metric branch (metres/kilometres), resolving strings via `resources.getString(...)` instead of `stringResource()`
- [ ] 2.12 Implement: update the `LegSummary` composable to call `formatDistance(leg.distanceMetres, Locale.getDefault(), LocalContext.current.resources)`

## 3. LegSummary Compose UI Coverage (BDD)

- [ ] 3.1 Write test: GIVEN a LegSummary rendered with a Leg of distanceMetres=750 WHEN inspected THEN the distance text is visible between stops in `LegSummaryTest`
- [ ] 3.2 Write test: GIVEN a LegSummary rendered with a Leg WHEN inspected THEN the row exposes no clickable/tappable semantics (read-only, no interaction) in `LegSummaryTest`
- [ ] 3.3 Implement: confirm `LegSummary` composable requires no behavioural change beyond the `formatDistance` call-site update from task 2.12 (no clickable modifiers added); adjust only if test 3.2 reveals an issue

## 4. Preview Coverage

- [ ] 4.1 Add `PreviewLegSummaryImperialLocale` preview to `LegSummary.kt`, forcing an imperial locale (e.g. via `CompositionLocalProvider(LocalConfiguration provides ...)` or a `Locale.US`-driven preview configuration) showing a miles-formatted distance, wrapped in `HeadingToTheAlpsTheme`

## 5. Verification

- [ ] 5.1 Run `./gradlew check` and confirm all checks pass
- [ ] 5.2 Run `./gradlew connectedDebugAndroidTest` and confirm `LegSummaryTest` and existing `TripDetailScreenTest` pass
- [ ] 5.3 On-device verification: confirm distance renders correctly between stops in both a metric-locale and an imperial-locale device/emulator configuration
