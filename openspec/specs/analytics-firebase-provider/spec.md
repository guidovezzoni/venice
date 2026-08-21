# analytics-firebase-provider Spec

## Purpose

Specifies the `FirebaseAnalyticsProvider` — the Firebase Analytics backend for Venice's
provider-agnostic analytics layer. Covers constructor-injection contract, event and user-property
mapping, platform-limit validation, thread safety, DI registration, and manifest configuration.

---

## Requirements

### Requirement: `FirebaseAnalyticsProvider` implements `AnalyticsProvider` with injectable `FirebaseAnalytics`
`FirebaseAnalyticsProvider` SHALL implement `AnalyticsProvider` and SHALL receive its `FirebaseAnalytics`
instance via constructor injection. It SHALL NOT call `FirebaseAnalytics.getInstance()` or the `Firebase.analytics`
KTX accessor internally, so the SDK dependency can be replaced with a test double in unit tests without
Robolectric or instrumentation.

#### Scenario: FirebaseAnalytics is constructor-injected
- **WHEN** `FirebaseAnalyticsProvider`'s constructor is inspected
- **THEN** it declares a `FirebaseAnalytics` parameter and performs no static SDK lookup in its body

### Requirement: Every `AnalyticsEvent` is logged to Firebase using its `name` and `properties`
`FirebaseAnalyticsProvider.logEvent` SHALL build a Firebase `Bundle` from `event.properties` and call
`FirebaseAnalytics.logEvent(event.name, bundle)` for every `AnalyticsEvent` subclass except
`ScreenViewed`, which is governed by a separate requirement below. Each entry in `properties` SHALL be
mapped by its runtime type: `String` values SHALL be added with `putString`; `Int` and `Long` values
SHALL be added with `putLong`; `Double` values SHALL be added with `putDouble`; `Boolean` values SHALL
be converted to the String `"true"` or `"false"` and added with `putString`.

#### Scenario: A String parameter is mapped with putString
- **GIVEN** an `AnalyticsEvent` whose `properties` contains a `String` value
- **WHEN** `logEvent` is called
- **THEN** the resulting `Bundle` contains that value as a String under the same key

#### Scenario: An Int parameter is mapped with putLong
- **GIVEN** an `AnalyticsEvent` whose `properties` contains an `Int` value
- **WHEN** `logEvent` is called
- **THEN** the resulting `Bundle` contains that value as a Long under the same key

#### Scenario: A Boolean parameter is converted to a String
- **GIVEN** `AnalyticsEvent.TripCreated(isFirstTrip = true)`
- **WHEN** `logEvent` is called
- **THEN** `FirebaseAnalytics.logEvent` is invoked with a `Bundle` containing `"is_first_trip"` mapped
  to the String `"true"`, not a Boolean

#### Scenario: An event's Firebase event name matches its domain name
- **GIVEN** any `AnalyticsEvent` other than `ScreenViewed`
- **WHEN** `logEvent` is called
- **THEN** `FirebaseAnalytics.logEvent` is invoked with `event.name` as the event name argument

### Requirement: `ScreenViewed` maps to Firebase's built-in `SCREEN_VIEW` event
`FirebaseAnalyticsProvider.logEvent` SHALL, when given an `AnalyticsEvent.ScreenViewed`, call
`FirebaseAnalytics.logEvent` with `FirebaseAnalytics.Event.SCREEN_VIEW` as the event name and a
`Bundle` containing the screen value under `FirebaseAnalytics.Param.SCREEN_NAME`, instead of logging a
custom `"screen_viewed"` event with a `"screen_name"` key.

#### Scenario: ScreenViewed uses the reserved SCREEN_VIEW event name
- **GIVEN** `AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_LIST)`
- **WHEN** `logEvent` is called
- **THEN** `FirebaseAnalytics.logEvent` is invoked with `FirebaseAnalytics.Event.SCREEN_VIEW`, not
  `"screen_viewed"`

#### Scenario: ScreenViewed's screen value is mapped to the reserved SCREEN_NAME parameter
- **GIVEN** `AnalyticsEvent.ScreenViewed(AnalyticsScreen.TRIP_DETAIL)`
- **WHEN** `logEvent` is called
- **THEN** the resulting `Bundle` contains `"trip_detail"` keyed by `FirebaseAnalytics.Param.SCREEN_NAME`

### Requirement: Both user properties are forwarded with a fixed name mapping
`FirebaseAnalyticsProvider.setUserProperty` SHALL call `FirebaseAnalytics.setUserProperty` with
`"trip_count_band"` and the property's `CountBand.value` for `AnalyticsUserProperty.TripCountBand`,
and with `"distance_unit"` and the property's `DistanceUnitParam.value` for
`AnalyticsUserProperty.DistanceUnit`. The mapping SHALL be an exhaustive `when` over the sealed
`AnalyticsUserProperty` interface.

#### Scenario: TripCountBand is forwarded with its band value
- **GIVEN** `AnalyticsUserProperty.TripCountBand(CountBand.RANGE_2_5)`
- **WHEN** `setUserProperty` is called
- **THEN** `FirebaseAnalytics.setUserProperty` is invoked with `"trip_count_band"` and `"2_5"`

#### Scenario: DistanceUnit is forwarded with its unit value
- **GIVEN** `AnalyticsUserProperty.DistanceUnit(DistanceUnitParam.IMPERIAL)`
- **WHEN** `setUserProperty` is called
- **THEN** `FirebaseAnalytics.setUserProperty` is invoked with `"distance_unit"` and `"imperial"`

### Requirement: `trackException` is a no-op
`FirebaseAnalyticsProvider.trackException` SHALL NOT call any method on `FirebaseAnalytics` and SHALL
NOT throw. Crash reporting is out of scope for this provider and is reserved for a future
Crashlytics-backed provider.

#### Scenario: trackException does not interact with FirebaseAnalytics
- **GIVEN** a `FirebaseAnalyticsProvider` with a mocked `FirebaseAnalytics`
- **WHEN** `trackException(RuntimeException("boom"), AnalyticsOperation.CREATE_TRIP)` is called
- **THEN** no method is invoked on the mocked `FirebaseAnalytics` instance and no exception propagates

### Requirement: Platform-limit violations are validated before every call and fail loudly in debug builds only
`FirebaseAnalyticsProvider` SHALL validate, before forwarding to `FirebaseAnalytics`, that: event and
parameter names are at most 40 characters, start with an alphabetic character, contain only
alphanumeric characters and underscores, and do not begin with `firebase_`, `google_`, or `ga_`; string
parameter values are at most 100 characters; an event carries at most 25 parameters; and user property
names are at most 24 characters under the same character and prefix rules. Whether a violation throws
SHALL be governed by an injected `Boolean` constructor parameter (defaulting to `BuildConfig.DEBUG`),
not by reading `BuildConfig.DEBUG` directly inside the validation function. When that flag is `true`,
a violation SHALL throw `IllegalStateException`. When it is `false`, a violation SHALL be logged via
`Log.e` and the call SHALL return without throwing and without forwarding the invalid data to
`FirebaseAnalytics`.

#### Scenario: An over-length event name throws in a debug-configured provider
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true`
- **WHEN** `logEvent` is called with an event whose `name` exceeds 40 characters
- **THEN** an `IllegalStateException` is thrown and `FirebaseAnalytics.logEvent` is not invoked

#### Scenario: An over-length event name is logged, not thrown, in a release-configured provider
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `false`
- **WHEN** `logEvent` is called with an event whose `name` exceeds 40 characters
- **THEN** no exception is thrown, `FirebaseAnalytics.logEvent` is not invoked, and an error is logged

#### Scenario: A reserved-prefix parameter name is rejected
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true`
- **WHEN** `logEvent` is called with an event whose `properties` map contains a key starting with
  `"firebase_"`, `"google_"`, or `"ga_"`
- **THEN** an `IllegalStateException` is thrown and `FirebaseAnalytics.logEvent` is not invoked

#### Scenario: A string value at exactly the 100-character boundary is accepted
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true`
- **WHEN** `logEvent` is called with a String parameter value of exactly 100 characters
- **THEN** no exception is thrown and `FirebaseAnalytics.logEvent` is invoked

#### Scenario: A string value one character over the 100-character boundary is rejected
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true`
- **WHEN** `logEvent` is called with a String parameter value of 101 characters
- **THEN** an `IllegalStateException` is thrown

#### Scenario: An over-length user property name throws in a debug-configured provider
- **GIVEN** a `FirebaseAnalyticsProvider` constructed with its debug-flag parameter set to `true`
- **WHEN** `setUserProperty` is called with a mapped property name exceeding 24 characters
- **THEN** an `IllegalStateException` is thrown and `FirebaseAnalytics.setUserProperty` is not invoked

### Requirement: The provider is safe to call from any thread
`FirebaseAnalyticsProvider` SHALL hold no mutable instance state, so `logEvent`, `setUserProperty`, and
`trackException` are safe to invoke concurrently from any thread or coroutine context.

#### Scenario: Concurrent calls from multiple threads do not corrupt provider state
- **GIVEN** a `FirebaseAnalyticsProvider` with a mocked `FirebaseAnalytics`
- **WHEN** `logEvent` is invoked concurrently from multiple threads with distinct valid events
- **THEN** every call reaches the mocked `FirebaseAnalytics` exactly once, with no shared mutable state
  read or written by the provider

### Requirement: The provider is registered only in the `main` source set
The `@Binds @IntoSet` binding for `FirebaseAnalyticsProvider` SHALL be declared in `AnalyticsModule.kt`
under `app/src/main/`, not under `app/src/debug/`, so the provider is present in every build variant
including release.

#### Scenario: The Firebase binding is present in a release-variant dependency graph
- **WHEN** the `main` source set's `AnalyticsModule` is inspected
- **THEN** it contains a `@Binds @IntoSet` function producing `FirebaseAnalyticsProvider` as an
  `AnalyticsProvider`

### Requirement: Firebase's automatic `screen_view` collection is disabled
The application manifest SHALL declare
`<meta-data android:name="google_analytics_automatic_screen_reporting_enabled" android:value="false" />`
inside the `<application>` element, so the manually-logged `SCREEN_VIEW` mapping (see the `ScreenViewed`
requirement above) is the only source of `screen_view` events reaching Firebase.

#### Scenario: The manifest disables automatic screen reporting
- **WHEN** `AndroidManifest.xml` is inspected
- **THEN** it contains a `google_analytics_automatic_screen_reporting_enabled` meta-data tag with
  `android:value="false"` inside `<application>`

### Requirement: No Firebase type appears outside `core/analytics/` and `di/`
No source file outside `app/src/*/java/.../core/analytics/` and `app/src/*/java/.../di/` SHALL import
any `com.google.firebase.*` class. This preserves the containment the analytics abstraction exists to
provide — no other layer becomes aware that Firebase is the backend in use.

#### Scenario: A repository-wide search finds no stray Firebase imports
- **WHEN** the codebase is searched for `import com.google.firebase` outside `core/analytics/` and `di/`
- **THEN** zero matches are found
