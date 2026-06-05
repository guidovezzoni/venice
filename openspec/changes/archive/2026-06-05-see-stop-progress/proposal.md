## Why

The trip detail screen currently provides minimal visual differentiation between departed, current, and upcoming stops — departed stops only fade in opacity, and the current stop only has a tinted icon. Users cannot quickly assess their trip progress at a glance, which is the core promise of the stop progress feature (Epic 1 / Feature 1.3). A progress summary and stronger visual cues complete the picture started in story 1.3.1.

## What Changes

- **Departed stops** gain a checkmark icon (replacing the default section icon) to reinforce their completed state alongside the existing 0.6f alpha fade
- **Current stop card** gains a highlighted border (primary colour, 2 dp) to draw attention to where the user is now
- **Upcoming stops** remain unchanged (default style, no modifications)
- **Trip progress summary** added at the top of the trip detail screen: displays "X of Y stops completed" text with a `LinearProgressIndicator`, visible when at least one stop exists
- New string resources added across all supported locales (en, it, es-ES)

## Capabilities

### New Capabilities
- `stop-progress-display`: Visual differentiation of stop states (checkmark for departed, border for current) and a trip progress summary bar in the trip detail screen

### Modified Capabilities

## Impact

- `ui/screens/tripdetail/StopSection.kt` — icon swap for departed, border for current
- `ui/screens/tripdetail/TripDetailScreen.kt` — progress count computation and summary placement
- New file: `ui/screens/tripdetail/TripProgressSummary.kt` — progress summary composable
- New file: `androidTest/.../TripProgressSummaryTest.kt` — Compose UI tests for progress summary
- `androidTest/.../TripDetailScreenTest.kt` — additional integration tests
- `res/values/strings.xml`, `res/values-it/strings.xml`, `res/values-es-rES/strings.xml` — new progress string
- No data or domain layer changes
- No new dependencies
