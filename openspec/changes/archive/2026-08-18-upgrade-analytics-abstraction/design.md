## Context

Venice's analytics layer currently straddles two layers it does not belong in:
`domain/analytics/AnalyticsTracker.kt` / `AnalyticsProvider.kt` / `AnalyticsEvent.kt`, and
`data/analytics/CompositeAnalyticsTracker.kt` / `LogAnalyticsProvider.kt`, wired by
`di/AnalyticsModule.kt`. Analytics carries no business rules — no use case or repository references
it — so `domain/` is wrong per the `core/` charter in `guidelines-android.md`. The giveaway is that a
future `StopTypeParam` serialisation helper would have to sit in `domain/analytics/` purely to adapt a
domain enum for a third-party SDK.

The Logcat provider (`LogAnalyticsProvider`) is currently bound in the `main` source set, so it is
registered in **every** build variant, including release. Logcat is readable over ADB by anyone with
physical access to an unlocked device, so this is a live, if low-severity, information-disclosure
issue today.

Two upcoming stories need capabilities this abstraction does not yet expose:
- **9.1.2** (taxonomy) needs a `setUserProperty` call and a way to record failures without smuggling
  free text into an event parameter.
- **9.2.1** (Firebase provider) and later a Crashlytics provider need the same two hooks.

This story is scoped to the abstraction only. It intentionally does not touch which events exist or
what they contain — `AnalyticsEvent`'s 10 subclasses and their parameters are byte-for-byte unchanged,
only relocated. That keeps this change reviewable as a structural diff, separate from 9.1.2's
taxonomy diff.

## Goals / Non-Goals

**Goals:**
- Relocate all analytics code to `core/analytics/`, leaving `domain/` and `data/` with zero analytics
  references.
- Rename the abstraction to match `guidelines-analytics.md` and Firebase's own `logEvent` naming, so
  the 9.2.1 Firebase provider is a drop-in implementation of an already-familiar method name.
- Collapse the currently-duplicated tracking surface (client and provider each declaring their own
  operations) into one shared interface, `AnalyticsTracking`.
- Add the `setUserProperty` and `trackException` hooks 9.1.2 and 9.2.1 need, as inert API surface only
  — nothing calls them in this story.
- Make "debug provider in release" structurally impossible rather than a runtime check, by moving its
  DI binding into the `debug` source set.
- Promote the 7 existing operation string constants to a real `AnalyticsOperation` enum, since
  `trackException` needs a typed operation parameter and stringly-typed values would defeat the point
  of introducing the channel.

**Non-Goals:**
- No change to `AnalyticsEvent`'s event names, parameters, or the data they carry — that is 9.1.2.
- No new operation values (`search_place`, `resolve_place`, `undo_mark_departed`) — 9.1.2 scope.
- No Hilt instrumented-test double (`FakeAnalyticsModule`, `@TestInstallIn`) — deferred to 9.1.4, since
  the project has no Hilt instrumented-test infrastructure at all yet (no `hilt-android-testing`
  dependency, no `HiltTestRunner`, no `@HiltAndroidTest` consumers), and standing that up is materially
  larger than this rename.
- No wiring of a real backend (Firebase, Crashlytics) — that is 9.2.1 / 9.2.2.
- No consent gating — that is 9.3.1.

## Decisions

### 1. Package destination: `core/analytics/`, not a split `core`/`data`

**Decision**: contracts (`AnalyticsClient`, `AnalyticsProvider`, `AnalyticsTracking`) and
implementations (`CompositeAnalyticsClient`, `DebugAnalyticsProvider`) all live together in
`core/analytics/`.

**Alternative considered**: interfaces in `core/analytics/`, implementations in `data/analytics/`,
mirroring the repository pattern used elsewhere in the codebase.

**Rejected because**: the repository split exists to separate a domain-facing contract from a
data-source-facing implementation that maps DTOs. `CompositeAnalyticsClient` is not a repository — it
fans out to providers, it maps nothing — and `AnalyticsProvider` implementations own no DTO mapping
either. Splitting one cohesive concern across two packages for a pattern that does not apply here would
cost readability for no benefit. This mirrors the explicit call-out in `guidelines-analytics.md`
("Contracts and implementations stay together, because analytics is one cohesive concern").

### 2. Shared `AnalyticsTracking` supertype

**Decision**: introduce `interface AnalyticsTracking { fun logEvent(...); fun setUserProperty(...); fun trackException(...) }`.
`AnalyticsClient : AnalyticsTracking` and `AnalyticsProvider : AnalyticsTracking` both extend it and
declare nothing of their own beyond what they inherit. (`AnalyticsProvider` previously also declared
`shouldTrack`; that method is removed entirely per AC3 — a provider that wants to ignore an event now
no-ops its own `logEvent`.)

**Alternative considered**: keep `AnalyticsClient` and `AnalyticsProvider` as two independently
declared interfaces, as today.

**Rejected because**: today's duplication is exactly the drift risk the guidelines call out — every
operation added to one has to be remembered on the other, a cost the project already paid twice while
designing this epic (the user-property and exception hooks were designed once but would need declaring
in two places). A shared supertype makes the surface a compile-time-enforced single source.

### 3. `shouldTrack` removed rather than kept as a provider-level filter

**Decision**: drop `shouldTrack(event): Boolean` entirely. A provider that wants to selectively ignore
events (e.g. a future crash-reporting provider that only cares about `OperationFailed`) does so inside
its own `logEvent` implementation.

**Alternative considered**: keep `shouldTrack` as part of `AnalyticsProvider` (not part of the shared
`AnalyticsTracking` supertype, so `AnalyticsClient` is unaffected).

**Rejected because**: the acceptance criteria (AC3) explicitly call for its removal, and
`guidelines-analytics.md`'s own reference skeleton for `AnalyticsProvider` no longer lists a separate
filter method — filtering folds into `logEvent` itself. Keeping it as a vestigial always-`true` method
(as `LogAnalyticsProvider` does today) adds an interface method with exactly one implementation and no
call site that varies it.

### 4. `AnalyticsOperation` is a new top-level enum, populated with only the 7 existing values

**Decision**: `AnalyticsOperation` is an `enum class` with an explicit `value: String` in `snake_case`
(per the guideline's enum convention), holding exactly the 7 values currently expressed as string
constants in `AnalyticsEvent.Companion`: `create_trip`, `set_stop`, `edit_stop`, `remove_stop`,
`move_stop`, `mark_departed`, `calculate_route`.

**Alternative considered A**: also add the 3 new values (`search_place`, `resolve_place`,
`undo_mark_departed`) the tracking plan already documents.

**Rejected because**: those values have no call site until 9.1.2 wires the place-search and
place-detail failure paths. Adding unused enum constants ahead of their consumer violates the same
"don't add analytics surface before it's needed" instinct the guidelines apply to events themselves,
and it would blur this story's diff with 9.1.2's.

**Alternative considered B**: leave `trackException`'s `operation` parameter as a `String` for this
story, and promote to an enum only when 9.1.2 wires real call sites.

**Rejected because**: `trackException` is part of this story's new API surface, and a stringly-typed
parameter on a brand-new method is exactly the anti-pattern the guidelines warn against ("Event names
as `const val` in each ViewModel" — the same drift risk applies to operation identifiers). Since the
enum has to exist eventually, and the values are already known and stable, creating it now with the
values that are already confirmed to exist costs nothing and avoids a second migration.

**Note**: `AnalyticsEvent.OperationFailed.operation` keeps its existing `String` type in this story —
`AnalyticsEvent` is explicitly untouched (Non-Goal). Only the new `trackException` signature uses the
enum. 9.1.2 is free to migrate `OperationFailed` onto the same enum type, unifying both, but that is
its own decision to make against its own diff.

### 5. Debug-only registration via source-set-scoped DI binding, not a `BuildConfig` flag

**Decision**: `DebugAnalyticsProvider` (the class) stays in `main`; only its `@Binds @IntoSet` binding
moves to a new `DebugAnalyticsModule` in `app/src/debug/java/.../di/`. `AnalyticsModule` (in `main`)
gains an explicit `@Multibinds` declaration for `Set<AnalyticsProvider>`.

**Alternative considered**: keep the binding in `main`, and gate it with
`if (BuildConfig.DEBUG) { Log.d(...) }` inside `DebugAnalyticsProvider.logEvent`.

**Rejected because**: `guidelines-analytics.md` is explicit that a runtime flag "is *not* the
mechanism — a second knob only adds a way to get it wrong." A source-set-scoped binding is enforced by
the compiler and R8: the release variant never even sees a constructor call for the debug provider, so
there's no flag to misconfigure and nothing to strip at runtime.

**Consequence requiring explicit handling**: with the debug binding removed from `main`, a release
build has **zero** `@IntoSet` contributions to `Set<AnalyticsProvider>`. Dagger does not synthesise an
empty multibound set without an explicit `@Multibinds` declaration — omitting it means the release
variant fails to compile, not just fails to log. This is called out as the single most likely thing to
be missed, and is a first-class task in `tasks.md` with its own verification step
(`./gradlew assembleRelease`).

### 6. Log formatting extracted to its own file, not left inline

**Decision**: `AnalyticsLogFormatter.kt` holds an `internal` function that builds the Logcat message
string; `DebugAnalyticsProvider` calls it.

**Rejected alternative**: keep formatting inline in `DebugAnalyticsProvider.logEvent`, as today.

**Rejected because**: per `guidelines-android.md`'s utility-function-own-file rule, a testable helper
belongs in its own file even with a single call site, and formatting for three now-distinct message
shapes (event, user property, exception) benefits from direct unit tests without instantiating the
provider or mocking `android.util.Log`.

## Risks / Trade-offs

- **[Risk]** Missing the `@Multibinds` declaration silently breaks the release build only, while
  `assembleDebug` continues to pass. → **Mitigation**: `tasks.md` requires both
  `./gradlew assembleDebug` and `./gradlew assembleRelease` to be run and verified green before the
  story is considered done; this is called out explicitly rather than left to "run the tests."
- **[Risk]** A wide rename touching two ViewModels and ~17 call sites is mechanical but error-prone by
  hand (a missed `.track(` → `.logEvent(` breaks compilation, which is at least safe; a missed import
  path does too). → **Mitigation**: the compiler catches every missed reference — there is no
  runtime-only failure mode here, since Kotlin will not compile a call to a method that no longer
  exists on the injected type.
- **[Risk]** `git mv` combined with a content edit (renaming the class/interface inside the moved file)
  can register as a delete+add instead of a rename if the diff is too large, weakening `git log
  --follow`. → **Mitigation**: perform the `git mv` as a separate step from the rename edit where
  practical (move first, then edit in place), and verify with `git log --follow <path>` after the
  move, per the story's Definition of Done.
- **[Risk]** Introducing `AnalyticsUserProperty` as an empty sealed class produces a `when` with no
  branches anywhere it's exhaustively matched, which is fine for now but easy to forget is
  intentionally inert. → **Mitigation**: this is inherent to shipping the API ahead of its first
  consumer (9.1.2); no code branches on `AnalyticsUserProperty` in this story, so there's no exhaustive
  `when` to go stale. The zero-provider / zero-property state is explicitly documented on
  `CompositeAnalyticsClient` per AC19.
- **[Trade-off]** `AnalyticsOperation` (typed) and `AnalyticsEvent.OperationFailed.operation` (still
  `String`) coexist for the duration between this story and 9.1.2, which is a visible inconsistency in
  the codebase. → **Accepted**: unifying them now would require touching `AnalyticsEvent`, which is
  explicitly out of scope (Non-Goal) — the inconsistency is temporary and resolved by 9.1.2's own
  taxonomy pass.

## Migration Plan

1. Move the 5 existing source files with `git mv` first (no content edit in the same step where
   avoidable), then apply the renames/method changes in place, so history tracking survives the move.
2. Create the 4 new files (`AnalyticsTracking.kt`, `AnalyticsUserProperty.kt`,
   `AnalyticsOperation.kt`, `AnalyticsLogFormatter.kt`) directly in `core/analytics/`.
3. Update `di/AnalyticsModule.kt` in place (rename bound types, add `@Multibinds`).
4. Create `app/src/debug/java/com/guidovezzoni/venice/di/DebugAnalyticsModule.kt` (new source-set
   directory).
5. Update the two ViewModels' field name and call sites.
6. Move the 3 existing test files with `git mv`, then edit in place for the renames; add the 1 new
   test file and new test cases for user-property/exception fan-out.
7. Delete the now-empty `domain/analytics/` and `data/analytics/` directories.
8. Verify: `./gradlew assembleDebug assembleRelease`, `./gradlew test`, `./gradlew detektDebug`,
   `./gradlew koverVerify`, on-device Logcat check for both build types.

No rollback beyond `git revert` is needed — this is a same-module structural change with no data
migration, no schema change, and no external API dependency.

## Open Questions

None. All ambiguities (AC 11 deferral, `AnalyticsOperation` value scope, Kover exemption for
interfaces/empty sealed classes) were resolved during story refinement and are recorded in the story
document; nothing here requires a decision before implementation.
