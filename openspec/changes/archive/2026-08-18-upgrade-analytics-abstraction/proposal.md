## Why

Venice's analytics layer (`AnalyticsTracker` / `AnalyticsProvider` / `AnalyticsEvent`,
`CompositeAnalyticsTracker`, `LogAnalyticsProvider`) works, but its naming diverges from
`docs/guidelines/guidelines-analytics.md` and its Logcat provider is registered in **every** build
type, including release — where Logcat is readable over ADB by anyone with physical access to the
device. Stories 9.1.2 (taxonomy) and 9.2.1 (Firebase provider) both need a `setUserProperty` API and
an exception channel that do not exist yet. This story is a structural upgrade only: no event is added,
removed, or reparameterised, so it stays reviewable as a pure diff, separate from the taxonomy change
in 9.1.2.

## What Changes

- **Package relocation** — all analytics code moves from `domain/analytics/` and `data/analytics/` to
  a new `core/analytics/` package, per the `core/` charter (cross-cutting, no business rules, consumed
  by more than one layer). `domain/analytics/` and `data/analytics/` are deleted. **BREAKING** (internal
  only — no public API is exposed outside the app module).
- **Renames** — `AnalyticsTracker` → `AnalyticsClient` (`.track()` → `.logEvent()`);
  `AnalyticsProvider.track()` → `.logEvent()`; `AnalyticsProvider.shouldTrack()` removed;
  `CompositeAnalyticsTracker` → `CompositeAnalyticsClient`; `LogAnalyticsProvider` →
  `DebugAnalyticsProvider`. **BREAKING** (internal only).
- **Shared tracking surface** — new `AnalyticsTracking` interface declares `logEvent`,
  `setUserProperty`, and `trackException` once; `AnalyticsClient` and `AnalyticsProvider` both extend
  it instead of redeclaring operations separately.
- **User property API** — `setUserProperty(property: AnalyticsUserProperty)` added to the tracking
  surface. `AnalyticsUserProperty` is a new, initially empty sealed class. Unused until 9.1.2.
- **Exception channel** — `trackException(throwable: Throwable, operation: AnalyticsOperation)` added
  as a channel separate from events, so crash reporting can carry a real stack trace that the
  enum-bounded event taxonomy cannot. Unused until 9.1.2.
- **`AnalyticsOperation` promoted to a proper enum** — the 7 existing operation string constants in
  `AnalyticsEvent.Companion` (`create_trip`, `set_stop`, `edit_stop`, `remove_stop`, `move_stop`,
  `mark_departed`, `calculate_route`) become a top-level enum with an explicit `value: String`. No new
  values are added — `search_place`, `resolve_place`, and `undo_mark_departed` are 9.1.2 scope.
  `AnalyticsEvent.OperationFailed` keeps taking a `String` for its `operation` parameter — the
  taxonomy is unchanged by this story — but `trackException` takes the new enum.
- **Debug-only registration** — the Logcat provider's DI binding moves to a new
  `app/src/debug/java/.../di/DebugAnalyticsModule.kt` so it is registered in debug builds only. The
  class stays in `main` so its unit test stays in the ordinary `src/test` source set. `AnalyticsModule`
  gains an explicit `@Multibinds` declaration so the release variant still compiles with zero
  `@IntoSet` contributions.
- **Log formatting extraction** — Logcat message formatting is extracted from `DebugAnalyticsProvider`
  into an `internal` function in a new `AnalyticsLogFormatter.kt`, per the utility-function-own-file
  guideline.
- **Minimal inline comments** — a warning comment on `DebugAnalyticsProvider` against release
  registration, and a comment on `CompositeAnalyticsClient` documenting the zero-provider silent
  no-op. No KDoc beyond that.
- **Call sites updated** — `TripListViewModel` and `TripDetailViewModel` rename their
  `analyticsTracker` field to `analyticsClient` and call `.logEvent(...)` instead of `.track(...)`.
- **Out of scope**: the `AnalyticsEvent` taxonomy itself (event names, parameters, `error_message`
  handling) is untouched — that is 9.1.2. The `@TestInstallIn` / `FakeAnalyticsModule` Hilt test
  double is deferred to story 9.1.4, since the project has no Hilt instrumented-test infrastructure
  yet and standing it up is out of scope for a structural rename.

## Capabilities

### New Capabilities
- `analytics-abstraction`: the provider-agnostic analytics delivery layer in `core/analytics/` — the
  `AnalyticsClient` / `AnalyticsProvider` / `AnalyticsTracking` contracts, the `AnalyticsEvent` and
  `AnalyticsUserProperty` taxonomy containers, the `CompositeAnalyticsClient` fan-out, the
  `DebugAnalyticsProvider` Logcat sink, and their debug-only DI wiring. This is the first OpenSpec
  capability for analytics; no existing spec covers it.

### Modified Capabilities
(none — no other capability's requirements change; this is a structural move of internal delivery
code with no observable product behaviour change beyond release builds no longer emitting to Logcat)

## Impact

- **Affected code**: `domain/analytics/*` (3 files, deleted), `data/analytics/*` (2 files, deleted),
  new `core/analytics/*` (9 files: 5 moved+renamed, 4 new), `di/AnalyticsModule.kt` (modified),
  new `app/src/debug/java/.../di/DebugAnalyticsModule.kt`, `ui/viewmodel/TripListViewModel.kt` and
  `ui/viewmodel/TripDetailViewModel.kt` (field rename + call-site update).
- **Tests**: 3 existing test files moved/renamed (`AnalyticsEventTest`, `CompositeAnalyticsTrackerTest`
  → `CompositeAnalyticsClientTest`, `LogAnalyticsProviderTest` → `DebugAnalyticsProviderTest`); 1 new
  test file (`AnalyticsLogFormatterTest`); new cases added to the composite/provider tests for user
  property and exception fan-out; `TripListViewModelTest` and `TripDetailViewModelTest` updated for the
  rename.
- **Build**: both `assembleDebug` and `assembleRelease` must be verified — the release variant is the
  one that exercises the new `@Multibinds` declaration and the debug-only binding.
- **No dependency changes.** No new third-party library is introduced.
- **No consumer-facing behaviour change**, except that release builds stop emitting anything to Logcat
  (previously they emitted every event).
