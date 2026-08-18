## ADDED Requirements

### Requirement: `TripCreated` carries an aggregate first-trip flag, never a trip identifier
`AnalyticsEvent.TripCreated` SHALL have exactly one parameter, `is_first_trip: Boolean`, and SHALL NOT
carry a trip identifier or any other field.

#### Scenario: TripCreated carries only is_first_trip
- **WHEN** `AnalyticsEvent.TripCreated(isFirstTrip = true)` is constructed
- **THEN** its `name` is `"trip_created"` and its `properties` map is exactly
  `{"is_first_trip": true}`

### Requirement: `TripOpened` carries stop count and route state, never a trip identifier
`AnalyticsEvent.TripOpened` SHALL have exactly two parameters: `stop_count: Int` and
`route_state: String` restricted to the values `"none"` and `"complete"` (the value `"stale"` is
reserved in the vocabulary but currently unreachable — see the `analytics-instrumentation`
capability). It SHALL NOT carry a trip identifier.

#### Scenario: TripOpened carries stop_count and route_state
- **WHEN** `AnalyticsEvent.TripOpened(stopCount = 3, routeState = "complete")` is constructed
- **THEN** its `name` is `"trip_opened"` and its `properties` map is exactly
  `{"stop_count": 3, "route_state": "complete"}`

### Requirement: `StopAdded` carries stop type and stop count, never a trip or stop identifier
`AnalyticsEvent.StopAdded` SHALL have exactly two parameters: `stop_type: StopTypeParam` and
`stop_count: Int`. The `stop_type` value in the emitted `properties` map SHALL be
`StopTypeParam.value`, never `StopTypeParam.name`. This event replaces the legacy `StopSet` event and
carries the same product meaning (a starting point, destination, or intermediate stop is persisted)
under a past-tense name.

#### Scenario: StopAdded emits the enum's value, not its name
- **WHEN** `AnalyticsEvent.StopAdded(stopType = StopTypeParam.STARTING_POINT, stopCount = 1)` is
  constructed
- **THEN** its `name` is `"stop_added"` and its `properties` map is exactly
  `{"stop_type": "starting_point", "stop_count": 1}`

### Requirement: `RouteCalculated` carries bounded counts and bands, never raw distance or duration
`AnalyticsEvent.RouteCalculated` SHALL have exactly four parameters: `stop_count: Int`,
`leg_count: Int`, `distance_band: DistanceBand`, and `duration_band: DurationBand`. It SHALL NOT carry
raw metres, raw seconds, or a trip identifier. Banded values in `properties` SHALL be the enum's
`value`, never its `name`.

#### Scenario: RouteCalculated emits bands, not raw metres or seconds
- **WHEN** `AnalyticsEvent.RouteCalculated(stopCount = 3, legCount = 2, distanceBand =
  DistanceBand.UNDER_50KM, durationBand = DurationBand.UNDER_1H)` is constructed
- **THEN** its `properties` map is exactly `{"stop_count": 3, "leg_count": 2, "distance_band":
  "under_50km", "duration_band": "under_1h"}`, with no raw metres or seconds value present

### Requirement: `NavigationLaunched` carries stop type and stop position, never a stop identifier
`AnalyticsEvent.NavigationLaunched` SHALL have exactly two parameters: `stop_type: StopTypeParam` and
`stop_position: Int` (a 0-based ordinal, not an identifier).

#### Scenario: NavigationLaunched carries stop_type and stop_position
- **WHEN** `AnalyticsEvent.NavigationLaunched(stopType = StopTypeParam.DESTINATION, stopPosition = 2)`
  is constructed
- **THEN** its `name` is `"navigation_launched"` and its `properties` map is exactly
  `{"stop_type": "destination", "stop_position": 2}`

### Requirement: `StopEdited` carries stop type, never a trip identifier
`AnalyticsEvent.StopEdited` SHALL have exactly one parameter, `stop_type: StopTypeParam`.

#### Scenario: StopEdited carries only stop_type
- **WHEN** `AnalyticsEvent.StopEdited(stopType = StopTypeParam.INTERMEDIATE)` is constructed
- **THEN** its `name` is `"stop_edited"` and its `properties` map is exactly
  `{"stop_type": "intermediate"}`

### Requirement: `StopRemoved` carries stop type and stop count, never a trip identifier
`AnalyticsEvent.StopRemoved` SHALL have exactly two parameters: `stop_type: StopTypeParam` and
`stop_count: Int`.

#### Scenario: StopRemoved carries stop_type and stop_count
- **WHEN** `AnalyticsEvent.StopRemoved(stopType = StopTypeParam.INTERMEDIATE, stopCount = 2)` is
  constructed
- **THEN** its `name` is `"stop_removed"` and its `properties` map is exactly
  `{"stop_type": "intermediate", "stop_count": 2}`

### Requirement: `StopReordered` carries direction and stop count, never a trip identifier
`AnalyticsEvent.StopReordered` SHALL have exactly two parameters: `direction: String` restricted to
`"up"`/`"down"`, and `stop_count: Int`.

#### Scenario: StopReordered carries direction and stop_count
- **WHEN** `AnalyticsEvent.StopReordered(direction = "up", stopCount = 4)` is constructed
- **THEN** its `name` is `"stop_reordered"` and its `properties` map is exactly
  `{"direction": "up", "stop_count": 4}`

### Requirement: `StopDeparted` carries stop position and stop count, never a trip or stop identifier
`AnalyticsEvent.StopDeparted` SHALL have exactly two parameters: `stop_position: Int` (a 0-based
ordinal) and `stop_count: Int`.

#### Scenario: StopDeparted carries stop_position and stop_count
- **WHEN** `AnalyticsEvent.StopDeparted(stopPosition = 1, stopCount = 4)` is constructed
- **THEN** its `name` is `"stop_departed"` and its `properties` map is exactly
  `{"stop_position": 1, "stop_count": 4}`

### Requirement: `StopDepartureUndone` carries only stop position
`AnalyticsEvent.StopDepartureUndone` SHALL have exactly one parameter, `stop_position: Int`.

#### Scenario: StopDepartureUndone carries only stop_position
- **WHEN** `AnalyticsEvent.StopDepartureUndone(stopPosition = 1)` is constructed
- **THEN** its `name` is `"stop_departure_undone"` and its `properties` map is exactly
  `{"stop_position": 1}`

### Requirement: `PlaceSearchPerformed` carries a result count, never the query text
`AnalyticsEvent.PlaceSearchPerformed` SHALL have exactly one parameter, `suggestion_count: Int`, and
SHALL NOT carry the search query string or any other free text.

#### Scenario: PlaceSearchPerformed carries only suggestion_count
- **WHEN** `AnalyticsEvent.PlaceSearchPerformed(suggestionCount = 5)` is constructed
- **THEN** its `name` is `"place_search_performed"` and its `properties` map is exactly
  `{"suggestion_count": 5}`

### Requirement: `PlaceSuggestionSelected` carries the chosen position, never the place name
`AnalyticsEvent.PlaceSuggestionSelected` SHALL have exactly one parameter,
`suggestion_position: Int` (a 0-based ordinal), and SHALL NOT carry the selected place's name or
identifier.

#### Scenario: PlaceSuggestionSelected carries only suggestion_position
- **WHEN** `AnalyticsEvent.PlaceSuggestionSelected(suggestionPosition = 0)` is constructed
- **THEN** its `name` is `"place_suggestion_selected"` and its `properties` map is exactly
  `{"suggestion_position": 0}`

### Requirement: `OperationFailed` carries a bounded operation and error type, never free text
`AnalyticsEvent.OperationFailed` SHALL have exactly two parameters: `operation: AnalyticsOperation`
and `error_type: AnalyticsErrorType`. It SHALL NOT carry `throwable.message` or any other free-text
string. Both enum values in the emitted `properties` map SHALL be the enum's `value`, never its
`name`.

#### Scenario: OperationFailed emits bounded enum values, never a throwable message
- **WHEN** `AnalyticsEvent.OperationFailed(operation = AnalyticsOperation.SEARCH_PLACE, errorType =
  AnalyticsErrorType.NETWORK)` is constructed
- **THEN** its `name` is `"operation_failed"` and its `properties` map is exactly
  `{"operation": "search_place", "error_type": "network"}`, with no throwable message present

### Requirement: `ScreenViewed` carries a bounded screen name
`AnalyticsEvent.ScreenViewed` SHALL have exactly one parameter, `screen_name: AnalyticsScreen`, whose
emitted value SHALL be `AnalyticsScreen.value`, never `AnalyticsScreen.name`.

#### Scenario: ScreenViewed emits the screen enum's value
- **WHEN** `AnalyticsEvent.ScreenViewed(screenName = AnalyticsScreen.TRIP_DETAIL)` is constructed
- **THEN** its `name` is `"screen_viewed"` and its `properties` map is exactly
  `{"screen_name": "trip_detail"}`

### Requirement: The taxonomy contains exactly 14 events
`AnalyticsEvent` SHALL be a sealed class with exactly 14 concrete subclasses: `TripCreated`,
`TripOpened`, `StopAdded`, `RouteCalculated`, `NavigationLaunched`, `StopEdited`, `StopRemoved`,
`StopReordered`, `StopDeparted`, `StopDepartureUndone`, `PlaceSearchPerformed`,
`PlaceSuggestionSelected`, `OperationFailed`, `ScreenViewed`. No subclass SHALL declare a parameter of
a type other than `String`, `Int`, `Long`, `Double`, or `Boolean`.

#### Scenario: AnalyticsEvent has exactly 14 subclasses
- **WHEN** `AnalyticsEvent`'s sealed subclasses are enumerated
- **THEN** there are exactly 14, matching the names listed above, and none is `TripOpened` with a
  `tripId` parameter, `StopSet`, or any other legacy shape

### Requirement: Distance is banded into five fixed ranges
`DistanceBand` SHALL be a top-level enum in `core/analytics/` with exactly five constants —
`UNDER_50KM`, `RANGE_50_200KM`, `RANGE_200_500KM`, `RANGE_500_1000KM`, `OVER_1000KM` — each carrying an
explicit `value: String` matching `under_50km`, `50_200km`, `200_500km`, `500_1000km`, `over_1000km`
respectively. A mapping function SHALL classify a distance in metres into the correct band, with
band boundaries at 50km, 200km, 500km, and 1000km, lower bound inclusive.

#### Scenario: A distance exactly on a lower boundary bands into the higher range
- **WHEN** the mapping function classifies a distance of exactly 50,000 metres
- **THEN** it returns `DistanceBand.RANGE_50_200KM`, not `DistanceBand.UNDER_50KM`

#### Scenario: A distance one metre below a boundary bands into the lower range
- **WHEN** the mapping function classifies a distance of 49,999 metres
- **THEN** it returns `DistanceBand.UNDER_50KM`

#### Scenario: A zero distance bands into the lowest range
- **WHEN** the mapping function classifies a distance of 0 metres
- **THEN** it returns `DistanceBand.UNDER_50KM`

#### Scenario: A very large distance bands into the highest range
- **WHEN** the mapping function classifies a distance of 2,000,000 metres
- **THEN** it returns `DistanceBand.OVER_1000KM`

### Requirement: Duration is banded into five fixed ranges
`DurationBand` SHALL be a top-level enum in `core/analytics/` with exactly five constants —
`UNDER_1H`, `RANGE_1_3H`, `RANGE_3_6H`, `RANGE_6_12H`, `OVER_12H` — each carrying an explicit
`value: String` matching `under_1h`, `1_3h`, `3_6h`, `6_12h`, `over_12h` respectively. A mapping
function SHALL classify a duration in seconds into the correct band, with band boundaries at 1, 3, 6,
and 12 hours, lower bound inclusive.

#### Scenario: A duration exactly on a lower boundary bands into the higher range
- **WHEN** the mapping function classifies a duration of exactly 3,600 seconds
- **THEN** it returns `DurationBand.RANGE_1_3H`, not `DurationBand.UNDER_1H`

#### Scenario: A duration one second below a boundary bands into the lower range
- **WHEN** the mapping function classifies a duration of 3,599 seconds
- **THEN** it returns `DurationBand.UNDER_1H`

#### Scenario: A zero duration bands into the lowest range
- **WHEN** the mapping function classifies a duration of 0 seconds
- **THEN** it returns `DurationBand.UNDER_1H`

#### Scenario: A very large duration bands into the highest range
- **WHEN** the mapping function classifies a duration of 100,000 seconds
- **THEN** it returns `DurationBand.OVER_12H`

### Requirement: Trip count is banded into four fixed ranges
`CountBand` SHALL be a top-level enum in `core/analytics/` with exactly four constants — `ZERO`, `ONE`,
`RANGE_2_5`, `SIX_PLUS` — each carrying an explicit `value: String` matching `0`, `1`, `2_5`, `6_plus`
respectively. A mapping function SHALL classify a non-negative trip count into the correct band.

#### Scenario: Zero trips bands to the zero constant
- **WHEN** the mapping function classifies a trip count of 0
- **THEN** it returns `CountBand.ZERO`

#### Scenario: One trip bands to the one constant
- **WHEN** the mapping function classifies a trip count of 1
- **THEN** it returns `CountBand.ONE`

#### Scenario: A count at each edge of the 2-5 range bands correctly
- **WHEN** the mapping function classifies a trip count of 2, then of 5
- **THEN** both return `CountBand.RANGE_2_5`

#### Scenario: A count just above the 2-5 range bands to six-plus
- **WHEN** the mapping function classifies a trip count of 6
- **THEN** it returns `CountBand.SIX_PLUS`

### Requirement: A throwable is classified into a bounded error type by exception type only
`AnalyticsErrorClassifier` SHALL be a top-level function `(Throwable) -> AnalyticsErrorType` in
`core/analytics/`. `AnalyticsErrorType` SHALL be a top-level enum with exactly seven constants —
`NETWORK`, `TIMEOUT`, `NOT_FOUND`, `PERMISSION_DENIED`, `QUOTA_EXCEEDED`, `PERSISTENCE`, `UNKNOWN` —
each carrying an explicit `value: String` in `snake_case`. Classification SHALL inspect only the
throwable's runtime type, in this order: a `java.net.SocketTimeoutException` classifies as `TIMEOUT`;
any other `java.io.IOException` classifies as `NETWORK`; an
`android.database.sqlite.SQLiteException` classifies as `PERSISTENCE`; anything else classifies as
`UNKNOWN`. `NOT_FOUND`, `PERMISSION_DENIED`, and `QUOTA_EXCEEDED` are reserved vocabulary for a future,
finer-grained classifier and SHALL NOT be reachable from this function as of this change; this
limitation SHALL be documented with a code comment on the classifier.

#### Scenario: A socket timeout classifies as timeout
- **WHEN** `AnalyticsErrorClassifier` is called with a `SocketTimeoutException`
- **THEN** it returns `AnalyticsErrorType.TIMEOUT`

#### Scenario: A generic IOException classifies as network
- **WHEN** `AnalyticsErrorClassifier` is called with a plain `IOException` or one of its subtypes
  other than `SocketTimeoutException`
- **THEN** it returns `AnalyticsErrorType.NETWORK`

#### Scenario: A SQLiteException classifies as persistence
- **WHEN** `AnalyticsErrorClassifier` is called with an `SQLiteException`
- **THEN** it returns `AnalyticsErrorType.PERSISTENCE`

#### Scenario: Any other throwable classifies as unknown
- **WHEN** `AnalyticsErrorClassifier` is called with an `IllegalStateException` (or any type not
  covered by the three rules above)
- **THEN** it returns `AnalyticsErrorType.UNKNOWN`

### Requirement: Enumerated parameter values always emit `.value`, never `.name`
Every `AnalyticsEvent` subclass constructor SHALL place an enum-typed parameter's `.value` into the
`properties` map, never its `.name`. This applies to every enum-typed parameter in the taxonomy:
`StopTypeParam`, `DistanceBand`, `DurationBand`, `AnalyticsScreen`, `AnalyticsOperation`, and
`AnalyticsErrorType`. No `properties` map anywhere in `AnalyticsEvent` SHALL contain a
`SCREAMING_SNAKE_CASE` string.

#### Scenario: No event property is SCREAMING_SNAKE_CASE
- **WHEN** every `AnalyticsEvent` subclass is constructed with each of its enum-typed parameters set
  to every possible enum constant, and the resulting `properties` values are inspected
- **THEN** no string value matches the pattern of an uppercase-with-underscores Kotlin enum `.name`

### Requirement: User properties are typed and bounded
`AnalyticsUserProperty` SHALL be a sealed class in `core/analytics/` with exactly two concrete
subclasses: `TripCountBand(band: CountBand)` and `DistanceUnit(unit: String)` restricted to
`"metric"`/`"imperial"`. Neither subclass name SHALL collide with any `AnalyticsEvent` parameter name
(`is_first_trip`, `stop_count`, `route_state`, `stop_type`, `leg_count`, `distance_band`,
`duration_band`, `stop_position`, `direction`, `suggestion_count`, `suggestion_position`, `operation`,
`error_type`, `screen_name`).

#### Scenario: TripCountBand carries a bounded band, not a raw count
- **WHEN** `AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)` is constructed
- **THEN** it holds `CountBand.RANGE_2_5`, and no field on it is a raw `Int` trip count

#### Scenario: DistanceUnit is restricted to metric or imperial
- **WHEN** `AnalyticsUserProperty.DistanceUnit(unit = "imperial")` is constructed
- **THEN** its `unit` value is `"imperial"`
