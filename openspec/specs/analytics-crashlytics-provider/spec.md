## Purpose

Specifies the `CrashlyticsAnalyticsProvider` — the analytics backend that routes failure diagnostics
to Firebase Crashlytics. It implements `AnalyticsProvider` and is registered alongside
`FirebaseAnalyticsProvider` via Hilt multibinding, forwarding exceptions with rich context while
keeping Crashlytics types strictly contained to `core/analytics/` and `di/`.

## Requirements

### Requirement: `CrashlyticsAnalyticsProvider` implements `AnalyticsProvider` with injectable `FirebaseCrashlytics`
`CrashlyticsAnalyticsProvider` SHALL implement `AnalyticsProvider` and SHALL receive its
`FirebaseCrashlytics` instance via constructor injection. It SHALL NOT call
`FirebaseCrashlytics.getInstance()` internally, so the SDK dependency can be replaced with a test
double in unit tests without Robolectric or instrumentation.

#### Scenario: FirebaseCrashlytics is constructor-injected
- **WHEN** `CrashlyticsAnalyticsProvider`'s constructor is inspected
- **THEN** it declares a `FirebaseCrashlytics` parameter and performs no static SDK lookup in its body

### Requirement: `trackException` sets the operation custom key before recording the throwable
`CrashlyticsAnalyticsProvider.trackException(throwable, operation)` SHALL first call
`FirebaseCrashlytics.setCustomKey("operation", operation.value)`, then call
`FirebaseCrashlytics.recordException(throwable)` with the unmodified throwable it was given.

#### Scenario: The operation key is set before recordException is called
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `trackException(RuntimeException("boom"), AnalyticsOperation.CREATE_TRIP)` is called
- **THEN** `setCustomKey("operation", "create_trip")` is invoked before `recordException` is invoked

#### Scenario: The real throwable is passed to recordException
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `trackException` is called with a specific `Throwable` instance
- **THEN** `FirebaseCrashlytics.recordException` is invoked with that exact instance

### Requirement: `logEvent` sets the error_type custom key only for OperationFailed and never records an exception
`CrashlyticsAnalyticsProvider.logEvent` SHALL, when given `AnalyticsEvent.OperationFailed(operation,
errorType)`, call `FirebaseCrashlytics.setCustomKey("error_type", errorType.value)`. For every other
`AnalyticsEvent` subtype, `logEvent` SHALL be a no-op — no custom key is set and no other
`FirebaseCrashlytics` method is invoked. `logEvent` SHALL NEVER call
`FirebaseCrashlytics.recordException` under any circumstances; a Crashlytics non-fatal SHALL only ever
originate from `trackException`.

#### Scenario: OperationFailed sets the error_type custom key
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `logEvent(AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK))` is called
- **THEN** `setCustomKey("error_type", "network")` is invoked

#### Scenario: OperationFailed never triggers recordException
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `logEvent(AnalyticsEvent.OperationFailed(AnalyticsOperation.CALCULATE_ROUTE, AnalyticsErrorType.NETWORK))` is called
- **THEN** `recordException` is never invoked on the mocked `FirebaseCrashlytics`

#### Scenario: A non-OperationFailed event is a complete no-op
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `logEvent` is called with an `AnalyticsEvent` that is not `OperationFailed` (e.g. `TripCreated`)
- **THEN** no method is invoked on the mocked `FirebaseCrashlytics`

### Requirement: Both user properties are forwarded as custom keys with a fixed name mapping
`CrashlyticsAnalyticsProvider.setUserProperty` SHALL call
`FirebaseCrashlytics.setCustomKey("trip_count_band", band.value)` for
`AnalyticsUserProperty.TripCountBand`, and `FirebaseCrashlytics.setCustomKey("distance_unit",
unit.value)` for `AnalyticsUserProperty.DistanceUnit`. The mapping SHALL be an exhaustive `when` over
the sealed `AnalyticsUserProperty` interface.

#### Scenario: TripCountBand is forwarded as a custom key
- **GIVEN** `AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5)`
- **WHEN** `setUserProperty` is called
- **THEN** `FirebaseCrashlytics.setCustomKey` is invoked with `"trip_count_band"` and `"2_5"`

#### Scenario: DistanceUnit is forwarded as a custom key
- **GIVEN** `AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL)`
- **WHEN** `setUserProperty` is called
- **THEN** `FirebaseCrashlytics.setCustomKey` is invoked with `"distance_unit"` and `"imperial"`

### Requirement: Only bounded enum values ever reach a custom key
Every value passed to `FirebaseCrashlytics.setCustomKey` by `CrashlyticsAnalyticsProvider` SHALL be a
`.value: String` from a bounded enum (`AnalyticsOperation`, `AnalyticsErrorType`, `CountBand`,
`DistanceUnitParam`). No raw identifier, free text, coordinate, or place name SHALL ever be passed as a
custom key value.

#### Scenario: Custom key values are drawn only from enum value strings
- **GIVEN** every call site within `CrashlyticsAnalyticsProvider` that invokes `setCustomKey`
- **WHEN** the source is inspected
- **THEN** every value argument is a `.value` reference on `AnalyticsOperation`, `AnalyticsErrorType`,
  `CountBand`, or `DistanceUnitParam` — never a free-form string, raw identifier, or throwable message

### Requirement: The provider is safe to call from any thread
`CrashlyticsAnalyticsProvider` SHALL hold no mutable instance state, so `logEvent`, `setUserProperty`,
and `trackException` are safe to invoke concurrently from any thread or coroutine context.

#### Scenario: Concurrent calls from multiple threads do not corrupt provider state
- **GIVEN** a `CrashlyticsAnalyticsProvider` with a mocked `FirebaseCrashlytics`
- **WHEN** `trackException` is invoked concurrently from multiple threads with distinct throwables and operations
- **THEN** every call reaches the mocked `FirebaseCrashlytics` exactly once, with no shared mutable
  state read or written by the provider

### Requirement: The provider is registered independently of `FirebaseAnalyticsProvider`
`CrashlyticsAnalyticsProvider` SHALL be registered via its own `@Binds @IntoSet` function in
`AnalyticsModule.kt`, distinct from the `FirebaseAnalyticsProvider` binding, so either provider can be
added, removed, or gated independently without touching the other.

#### Scenario: The Crashlytics binding exists independently of the Firebase Analytics binding
- **WHEN** `AnalyticsModule.kt` is inspected
- **THEN** it contains a `@Binds @IntoSet` function producing `CrashlyticsAnalyticsProvider` as an
  `AnalyticsProvider`, separate from the function producing `FirebaseAnalyticsProvider`

### Requirement: `setCrashlyticsCollectionEnabled` is not called
`CrashlyticsAnalyticsProvider` and its DI wiring SHALL NOT call
`FirebaseCrashlytics.setCrashlyticsCollectionEnabled` anywhere. Consent-gated collection is deferred to
a future story.

#### Scenario: No collection-enabled toggle is present
- **WHEN** `CrashlyticsAnalyticsProvider.kt` and `di/FirebaseModule.kt` are inspected
- **THEN** neither calls `setCrashlyticsCollectionEnabled`

### Requirement: No Crashlytics type appears outside `core/analytics/` and `di/`
No source file outside `app/src/*/java/.../core/analytics/` and `app/src/*/java/.../di/` SHALL import
any `com.google.firebase.crashlytics.*` class. This preserves the containment the analytics
abstraction exists to provide — no other layer becomes aware that Crashlytics is a backend in use.

#### Scenario: A repository-wide search finds no stray Crashlytics imports
- **WHEN** the codebase is searched for `import com.google.firebase.crashlytics` outside
  `core/analytics/` and `di/`
- **THEN** zero matches are found
