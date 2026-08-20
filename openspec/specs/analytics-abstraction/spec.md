# Analytics Abstraction Specification

## Purpose

Defines the structural architecture of the analytics layer in `core/analytics/`. Governs where
analytics code lives, how the shared tracking surface is declared, how the composite client fans out
calls, how the debug provider behaves, and how DI bindings are wired — without specifying the
event taxonomy (see the tracking plan).

## Requirements

### Requirement: Analytics code lives entirely in `core/analytics/`
All analytics contracts, taxonomy containers, and implementations SHALL reside under
`core/analytics/`. Neither `domain/` nor `data/` SHALL contain analytics types or reference
`core/analytics/` types, since analytics carries no business rules and is not a data source. Only
`ui/viewmodel/` and `di/` (including the debug source set's `di/`) MAY reference `core/analytics/`
types. `core/` SHALL contain no package other than `analytics/` as a result of this change.

#### Scenario: Domain layer has no analytics reference
- **WHEN** the codebase is searched for imports of `core.analytics` under `domain/`
- **THEN** zero matches are found

#### Scenario: Data layer has no analytics reference
- **WHEN** the codebase is searched for imports of `core.analytics` under `data/`
- **THEN** zero matches are found

#### Scenario: Legacy packages no longer exist
- **WHEN** the codebase is inspected after the change
- **THEN** `domain/analytics/` and `data/analytics/` do not exist

#### Scenario: File history is preserved through the move
- **WHEN** `git log --follow` is run against any moved file's new path (e.g.
  `core/analytics/AnalyticsClient.kt`)
- **THEN** the commit history from its pre-move path is shown

### Requirement: A single shared tracking surface declares every tracking operation once
An `AnalyticsTracking` interface SHALL declare `logEvent(event: AnalyticsEvent)`,
`setUserProperty(property: AnalyticsUserProperty)`, and
`trackException(throwable: Throwable, operation: AnalyticsOperation)`. It SHALL also provide a
default `trackFailure(operation: AnalyticsOperation, errorType: AnalyticsErrorType, throwable:
Throwable)` method that calls `logEvent(AnalyticsEvent.OperationFailed(operation, errorType))`
followed by `trackException(throwable, operation)` — encoding the dual-channel failure contract once
so call sites cannot accidentally fire one without the other. `AnalyticsClient` and
`AnalyticsProvider` SHALL both extend `AnalyticsTracking` and SHALL NOT redeclare any of its
operations.

#### Scenario: AnalyticsClient exposes the shared surface without redeclaring it
- **WHEN** `AnalyticsClient`'s source is inspected
- **THEN** it extends `AnalyticsTracking` and declares no additional abstract members of its own

#### Scenario: AnalyticsProvider exposes the shared surface without redeclaring it
- **WHEN** `AnalyticsProvider`'s source is inspected
- **THEN** it extends `AnalyticsTracking` and declares no additional abstract members of its own

### Requirement: The per-provider event filter is removed
`AnalyticsProvider` SHALL NOT expose a `shouldTrack` (or equivalently named) filtering method. A
provider that needs to ignore certain events, user properties, or exceptions SHALL implement that
behaviour inside its own `logEvent` / `setUserProperty` / `trackException` implementation.

#### Scenario: No shouldTrack method exists anywhere in the codebase
- **WHEN** the codebase is searched for `shouldTrack` or `shouldLog`
- **THEN** zero matches are found outside of documentation

### Requirement: The composite client fans out every tracking call to every registered provider
`CompositeAnalyticsClient` SHALL implement `AnalyticsClient` by injecting
`Set<@JvmSuppressWildcards AnalyticsProvider>` and, for each of `logEvent`, `setUserProperty`, and
`trackException`, invoking the corresponding method on every provider in the set unconditionally
(no filtering at the composite level). With an empty provider set, every operation SHALL be a silent
no-op — no exception, no crash — and this behaviour SHALL be documented with an inline comment on
`CompositeAnalyticsClient`.

#### Scenario: An event is fanned out to every provider
- **GIVEN** two providers are registered
- **WHEN** `CompositeAnalyticsClient.logEvent(event)` is called
- **THEN** both providers receive `logEvent(event)`

#### Scenario: A user property is fanned out to every provider
- **GIVEN** two providers are registered
- **WHEN** `CompositeAnalyticsClient.setUserProperty(property)` is called
- **THEN** both providers receive `setUserProperty(property)`

#### Scenario: An exception is fanned out to every provider
- **GIVEN** two providers are registered
- **WHEN** `CompositeAnalyticsClient.trackException(throwable, operation)` is called
- **THEN** both providers receive `trackException(throwable, operation)`

#### Scenario: Zero providers is a silent no-op for every operation
- **GIVEN** no providers are registered
- **WHEN** `logEvent`, `setUserProperty`, or `trackException` is called on `CompositeAnalyticsClient`
- **THEN** the call returns normally with no exception thrown and no observable side effect

### Requirement: The debug provider logs events, user properties, and exceptions distinguishably
`DebugAnalyticsProvider` SHALL implement `AnalyticsProvider` by writing a distinct, recognisable
Logcat message for each of an event, a user property, and an exception, so the three can be told
apart during on-device verification. The message content SHALL be produced by an `internal` function
in a dedicated `AnalyticsLogFormatter.kt` file, not built inline inside the provider. A throwable
passed to `trackException` SHALL NOT be surfaced through `logEvent` or become part of any event's
`properties` map.

#### Scenario: An event produces a recognisable log line
- **WHEN** `DebugAnalyticsProvider.logEvent(event)` is called
- **THEN** a Logcat message is written that identifies it as an event and includes the event's name
  and properties

#### Scenario: A user property produces a recognisable, distinct log line
- **WHEN** `DebugAnalyticsProvider.setUserProperty(property)` is called
- **THEN** a Logcat message is written that identifies it as a user property, distinguishable from
  the event message format

#### Scenario: An exception produces a recognisable, distinct log line
- **WHEN** `DebugAnalyticsProvider.trackException(throwable, operation)` is called
- **THEN** a Logcat message is written that identifies it as an exception, distinguishable from the
  event and user-property message formats

#### Scenario: A throwable never reaches an event parameter
- **WHEN** `trackException` is invoked
- **THEN** no `AnalyticsEvent.properties` map anywhere in the codebase contains the throwable or its
  message

### Requirement: The debug provider's DI binding is registered in debug builds only
`DebugAnalyticsProvider`'s `@Binds @IntoSet` binding to `AnalyticsProvider` SHALL live in a Hilt
module located in the `debug` source set (`app/src/debug/java/.../di/DebugAnalyticsModule.kt`), not
in `main`. `AnalyticsModule` (in `main`) SHALL declare `Set<AnalyticsProvider>` via `@Multibinds` so
that a variant with zero `@IntoSet` contributions still compiles.

#### Scenario: Debug builds register the debug provider
- **WHEN** a debug variant is built and run
- **THEN** `DebugAnalyticsProvider` is present in the injected `Set<AnalyticsProvider>`

#### Scenario: Release builds compile with an empty provider set
- **WHEN** `./gradlew assembleRelease` is run
- **THEN** the build succeeds, with `Set<AnalyticsProvider>` resolving to an empty set at runtime

#### Scenario: Release builds emit nothing to Logcat
- **WHEN** a release build is installed and exercised on a device
- **THEN** no analytics-related Logcat output is produced

### Requirement: User property and exception APIs exist but are not yet called
`AnalyticsUserProperty` SHALL be a sealed class in `core/analytics/`, initially with no concrete
subclasses. The `setUserProperty` and `trackException` members of `AnalyticsTracking` SHALL exist and
be implemented by `CompositeAnalyticsClient` and `DebugAnalyticsProvider`, but no call site outside
`core/analytics/` SHALL invoke either method in this change.

#### Scenario: No call site outside core/analytics invokes setUserProperty
- **WHEN** the codebase is searched for calls to `.setUserProperty(` outside `core/analytics/`
- **THEN** zero matches are found

#### Scenario: No call site outside core/analytics invokes trackException directly
- **WHEN** the codebase is searched for calls to `.trackException(` outside `core/analytics/`
- **THEN** zero matches are found — all failure paths call `.trackFailure(` instead, and the
  default `trackFailure` implementation in `AnalyticsTracking` calls `trackException` internally
  within `core/analytics/`

### Requirement: Operation identifiers are a typed, bounded enum
`AnalyticsOperation` SHALL be a top-level `enum class` in `core/analytics/`, each constant carrying an
explicit `value: String` in `snake_case`. It SHALL contain exactly the 10 operation values in use as
of this change: `create_trip`, `set_stop`, `edit_stop`, `remove_stop`, `move_stop`, `mark_departed`,
`undo_mark_departed`, `calculate_route`, `search_place`, `resolve_place`. The `trackException` member
of `AnalyticsTracking` SHALL take an `AnalyticsOperation` as its `operation` parameter.

#### Scenario: AnalyticsOperation carries exactly the ten documented values
- **WHEN** `AnalyticsOperation`'s constants are enumerated
- **THEN** they are exactly `CREATE_TRIP`, `SET_STOP`, `EDIT_STOP`, `REMOVE_STOP`, `MOVE_STOP`,
  `MARK_DEPARTED`, `UNDO_MARK_DEPARTED`, `CALCULATE_ROUTE`, `SEARCH_PLACE`, and `RESOLVE_PLACE`, each
  with a `value` matching its documented snake_case string

