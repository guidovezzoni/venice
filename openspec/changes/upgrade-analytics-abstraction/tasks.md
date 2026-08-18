## 1. Prerequisites — Relocate files and shared contracts

Non-testable: file moves, interface/contract declarations, and empty/flat data types carry no
executable logic (Kover naturally exempts interfaces and empty sealed classes; a flat enum with no
mapping/classification logic needs no boundary tests per the analytics guidelines).

- [ ] 1.1 `git mv app/src/main/java/com/guidovezzoni/venice/domain/analytics/AnalyticsEvent.kt app/src/main/java/com/guidovezzoni/venice/core/analytics/AnalyticsEvent.kt`; update only the `package` declaration to `com.guidovezzoni.venice.core.analytics` — no other content change (event names, parameters, and the 7 `OPERATION_*` companion constants stay exactly as-is)
- [ ] 1.2 Create `core/analytics/AnalyticsTracking.kt`: `interface AnalyticsTracking` declaring `fun logEvent(event: AnalyticsEvent)`, `fun setUserProperty(property: AnalyticsUserProperty)`, `fun trackException(throwable: Throwable, operation: AnalyticsOperation)`
- [ ] 1.3 Create `core/analytics/AnalyticsUserProperty.kt`: `sealed class AnalyticsUserProperty` with no concrete subclasses yet (populated by story 9.1.2)
- [ ] 1.4 Create `core/analytics/AnalyticsOperation.kt`: `enum class AnalyticsOperation(val value: String)` with exactly the 7 values already in use — `CREATE_TRIP("create_trip")`, `SET_STOP("set_stop")`, `EDIT_STOP("edit_stop")`, `REMOVE_STOP("remove_stop")`, `MOVE_STOP("move_stop")`, `MARK_DEPARTED("mark_departed")`, `CALCULATE_ROUTE("calculate_route")`. Do not add `search_place`, `resolve_place`, or `undo_mark_departed` — out of scope for this story
- [ ] 1.5 `git mv app/src/main/java/com/guidovezzoni/venice/domain/analytics/AnalyticsTracker.kt app/src/main/java/com/guidovezzoni/venice/core/analytics/AnalyticsClient.kt`; rename the interface to `AnalyticsClient : AnalyticsTracking`, remove the now-inherited `logEvent` redeclaration, update package
- [ ] 1.6 `git mv app/src/main/java/com/guidovezzoni/venice/domain/analytics/AnalyticsProvider.kt app/src/main/java/com/guidovezzoni/venice/core/analytics/AnalyticsProvider.kt`; rename the interface to `AnalyticsProvider : AnalyticsTracking`, remove `shouldTrack` and the now-inherited `logEvent`/`track` redeclarations, update package
- [ ] 1.7 `git mv app/src/test/java/com/guidovezzoni/venice/domain/analytics/AnalyticsEventTest.kt app/src/test/java/com/guidovezzoni/venice/core/analytics/AnalyticsEventTest.kt`; update only the `package` declaration and imports — test content and assertions stay as-is, since `AnalyticsEvent` is unchanged
- [ ] 1.8 Create the debug source-set directory `app/src/debug/java/com/guidovezzoni/venice/di/` (currently only `app/src/debug/res/` exists)

## 2. CompositeAnalyticsClient — event fan-out (BDD)

- [ ] 2.1 Write test: GIVEN two registered `AnalyticsProvider`s WHEN `CompositeAnalyticsClient.logEvent(event)` is called THEN both providers receive `logEvent(event)` with the same event instance, in `CompositeAnalyticsClientTest`
- [ ] 2.2 Implement: `git mv app/src/main/java/com/guidovezzoni/venice/data/analytics/CompositeAnalyticsTracker.kt app/src/main/java/com/guidovezzoni/venice/core/analytics/CompositeAnalyticsClient.kt`; rename class to `CompositeAnalyticsClient : AnalyticsClient`, inject `Set<@JvmSuppressWildcards AnalyticsProvider>`, implement `logEvent` to forward unconditionally to every provider (no `shouldTrack` gate), update package

## 3. CompositeAnalyticsClient — user property fan-out (BDD)

- [ ] 3.1 Write test: GIVEN two registered `AnalyticsProvider`s WHEN `CompositeAnalyticsClient.setUserProperty(property)` is called THEN both providers receive `setUserProperty(property)` with the same property instance, in `CompositeAnalyticsClientTest`
- [ ] 3.2 Implement: `CompositeAnalyticsClient.setUserProperty` forwards to every provider in the injected set

## 4. CompositeAnalyticsClient — exception fan-out (BDD)

- [ ] 4.1 Write test: GIVEN two registered `AnalyticsProvider`s WHEN `CompositeAnalyticsClient.trackException(throwable, operation)` is called THEN both providers receive `trackException(throwable, operation)` with the same throwable and operation, in `CompositeAnalyticsClientTest`
- [ ] 4.2 Implement: `CompositeAnalyticsClient.trackException` forwards to every provider in the injected set

## 5. CompositeAnalyticsClient — zero-provider no-op (BDD)

- [ ] 5.1 Write test: GIVEN zero registered providers WHEN `logEvent`, `setUserProperty`, and `trackException` are each called on `CompositeAnalyticsClient` THEN no exception is thrown and no provider interaction occurs (verify with an empty injected set), in `CompositeAnalyticsClientTest`
- [ ] 5.2 Implement: add an inline comment on `CompositeAnalyticsClient` documenting that an empty provider set makes every operation a silent no-op — not an error, not a crash — and that this is the state a release build is in until story 9.2.1 wires a real backend

## 6. AnalyticsLogFormatter — event message formatting (BDD)

- [ ] 6.1 Write test: GIVEN an `AnalyticsEvent` with a name and properties WHEN the internal event-formatting function is called THEN the returned string identifies it as an event and includes the event's name and properties, in `AnalyticsLogFormatterTest`
- [ ] 6.2 Implement: create `core/analytics/AnalyticsLogFormatter.kt` with an `internal fun formatEventLog(event: AnalyticsEvent): String` producing the message currently built inline in `LogAnalyticsProvider.track()`

## 7. AnalyticsLogFormatter — user property message formatting (BDD)

- [ ] 7.1 Write test: GIVEN an `AnalyticsUserProperty` WHEN the internal user-property-formatting function is called THEN the returned string identifies it as a user property and is visually distinguishable from the event-format string, in `AnalyticsLogFormatterTest`
- [ ] 7.2 Implement: add `internal fun formatUserPropertyLog(property: AnalyticsUserProperty): String` to `AnalyticsLogFormatter.kt`

## 8. AnalyticsLogFormatter — exception message formatting (BDD)

- [ ] 8.1 Write test: GIVEN a `Throwable` and an `AnalyticsOperation` WHEN the internal exception-formatting function is called THEN the returned string identifies it as an exception, includes the operation, and is visually distinguishable from the event- and user-property-format strings, in `AnalyticsLogFormatterTest`
- [ ] 8.2 Implement: add `internal fun formatExceptionLog(throwable: Throwable, operation: AnalyticsOperation): String` to `AnalyticsLogFormatter.kt`

## 9. DebugAnalyticsProvider — event logging (BDD)

- [ ] 9.1 Write test: GIVEN an `AnalyticsEvent` WHEN `DebugAnalyticsProvider.logEvent(event)` is called THEN `Log.d` is invoked with the tag and the string produced by `formatEventLog(event)`, in `DebugAnalyticsProviderTest`
- [ ] 9.2 Implement: `git mv app/src/main/java/com/guidovezzoni/venice/data/analytics/LogAnalyticsProvider.kt app/src/main/java/com/guidovezzoni/venice/core/analytics/DebugAnalyticsProvider.kt`; rename class to `DebugAnalyticsProvider : AnalyticsProvider`, implement `logEvent` by delegating to `formatEventLog` and `Log.d`, remove `shouldTrack`, update package; add a warning comment stating the class must only ever be registered via a debug-source-set DI module and never in release
- [ ] 9.3 `git mv app/src/test/java/com/guidovezzoni/venice/data/analytics/LogAnalyticsProviderTest.kt app/src/test/java/com/guidovezzoni/venice/core/analytics/DebugAnalyticsProviderTest.kt`; rename test class to `DebugAnalyticsProviderTest`, update package/imports, remove the now-obsolete `shouldTrack` test case

## 10. DebugAnalyticsProvider — user property logging (BDD)

- [ ] 10.1 Write test: GIVEN an `AnalyticsUserProperty` WHEN `DebugAnalyticsProvider.setUserProperty(property)` is called THEN `Log.d` is invoked with the tag and the string produced by `formatUserPropertyLog(property)`, in `DebugAnalyticsProviderTest`
- [ ] 10.2 Implement: `DebugAnalyticsProvider.setUserProperty` delegates to `formatUserPropertyLog` and `Log.d`

## 11. DebugAnalyticsProvider — exception logging (BDD)

- [ ] 11.1 Write test: GIVEN a `Throwable` and an `AnalyticsOperation` WHEN `DebugAnalyticsProvider.trackException(throwable, operation)` is called THEN `Log.d` is invoked with the tag and the string produced by `formatExceptionLog(throwable, operation)`, in `DebugAnalyticsProviderTest`
- [ ] 11.2 Implement: `DebugAnalyticsProvider.trackException` delegates to `formatExceptionLog` and `Log.d`

## 12. DI wiring — debug-only provider registration

Not independently testable: `di/` is excluded from Kover coverage and Hilt-generated bindings are not
unit-tested directly; correctness is proven by the build tasks in section 15.

- [ ] 12.1 Modify `di/AnalyticsModule.kt`: rebind `CompositeAnalyticsClient` → `AnalyticsClient` (`@Singleton`), remove the `LogAnalyticsProvider`/`DebugAnalyticsProvider` `@IntoSet` binding, add an explicit `@Multibinds abstract fun bindAnalyticsProviderSet(): Set<AnalyticsProvider>` declaration, update imports to `core.analytics`
- [ ] 12.2 Create `app/src/debug/java/com/guidovezzoni/venice/di/DebugAnalyticsModule.kt`: `@Module @InstallIn(SingletonComponent::class) abstract class DebugAnalyticsModule` with `@Binds @IntoSet abstract fun bindDebugAnalyticsProvider(implementation: DebugAnalyticsProvider): AnalyticsProvider`

## 13. Call site updates — ViewModels

Not independently BDD-testable as new behaviour: the analytics calls themselves are unchanged
(same events, same trigger points); only the field name and method name change, verified by updating
the existing ViewModel tests' mocks and verifications.

- [ ] 13.1 Modify `ui/viewmodel/TripListViewModel.kt`: rename `analyticsTracker: AnalyticsTracker` to `analyticsClient: AnalyticsClient`, change all `.track(...)` calls to `.logEvent(...)`, update imports to `core.analytics`
- [ ] 13.2 Modify `ui/viewmodel/TripDetailViewModel.kt`: same rename, method change, and import update as 13.1
- [ ] 13.3 Modify `app/src/test/.../ui/viewmodel/TripListViewModelTest.kt`: mock `AnalyticsClient` instead of `AnalyticsTracker`, rename the field/mock to `analyticsClient`, change `.track(...)` verifications to `.logEvent(...)`, update imports
- [ ] 13.4 Modify `app/src/test/.../ui/viewmodel/TripDetailViewModelTest.kt`: same updates as 13.3

## 14. Cleanup and documentation

- [ ] 14.1 Verify `domain/analytics/` and `data/analytics/` are empty and delete the directories
- [ ] 14.2 Verify `core/` contains only the `analytics/` package (no other directory was created under `core/`)
- [ ] 14.3 Grep the codebase for `AnalyticsTracker`, `shouldTrack`, `shouldLog`, and `.track(` (excluding git history) and confirm zero matches remain
- [ ] 14.4 Update `AGENTS.md` (via the `CLAUDE.md` symlink) only if it references the old `domain/analytics`/`data/analytics` paths or omits `core/` from any directory listing — otherwise no change needed

## 15. Final verification

- [ ] 15.1 Run `./gradlew clean assembleDebug assembleRelease` and confirm both variants build with zero errors (the release variant is what proves the `@Multibinds` declaration and debug-only binding are correct)
- [ ] 15.2 Run `./gradlew test` and confirm all unit tests pass, including the moved/renamed `AnalyticsEventTest`, `CompositeAnalyticsClientTest`, `DebugAnalyticsProviderTest`, `AnalyticsLogFormatterTest`, `TripListViewModelTest`, `TripDetailViewModelTest`
- [ ] 15.3 Run `./gradlew detektDebug` and confirm zero findings
- [ ] 15.4 Run `./gradlew koverVerify` and confirm the 95% bound is met
- [ ] 15.5 On a connected device, install the debug build, exercise a trip flow, and confirm via `adb logcat -s Analytics` that events log exactly as before this change (same events, same parameters), apart from the changed log tag if applicable
- [ ] 15.6 On a connected device, install a release build and confirm via `adb logcat -s Analytics` that no analytics output is produced
- [ ] 15.7 Run `git log --follow` against at least one moved file's new path (e.g. `core/analytics/AnalyticsClient.kt`) and confirm history from its pre-move path is shown
