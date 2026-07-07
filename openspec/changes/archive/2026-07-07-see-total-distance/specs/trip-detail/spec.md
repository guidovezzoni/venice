## MODIFIED Requirements

### Requirement: TripDetailUiState represents the screen state
`TripDetailUiState` SHALL be a data class with:
- `tripId: String` (default `""`)
- `startingPoint: Stop?` (default `null`)
- `destination: Stop?` (default `null`)
- `intermediateStops: List<Stop>` (default `emptyList()`)
- `isLoading: Boolean` (default `false`)
- `isSetStartingPointDialogVisible: Boolean` (default `false`)
- `isSetDestinationDialogVisible: Boolean` (default `false`)
- `isAddStopDialogVisible: Boolean` (default `false`)
- `isEditStopDialogVisible: Boolean` (default `false`)
- `editingStop: Stop?` (default `null`)
- `canAddMoreStops: Boolean` (default `false`)
- `isRemoveStopDialogVisible: Boolean` (default `false`)
- `stopToRemove: Stop?` (default `null`)
- `placeSuggestions: List<PlaceSuggestion>` (default `emptyList()`)
- `isSearchingPlaces: Boolean` (default `false`)
- `searchError: String?` (default `null`)
- `selectedPlaceDetail: PlaceDetail?` (default `null`)
- `isResolvingPlace: Boolean` (default `false`)
- `placeDetailError: String?` (default `null`)
- `legs: List<Leg>` (default `emptyList()`)
- `isCalculatingRoute: Boolean` (default `false`)
- `routeError: String?` (default `null`)
- `formattedTotalDistance: String?` (default `null`) — pre-computed, locale-aware formatted
  sum of every leg's `distanceMetres`, using the same `formatDistance` unit/rounding rules
  as individual legs. `null` when the route is not complete (`legs.size != stops.size - 1`,
  or fewer than 2 stops), meaning the total is shown as "unavailable" rather than as zero
  or a stale value. Formatting and completeness are computed entirely in the ViewModel; the
  composable performs no arithmetic, locale detection, or string-resource resolution.

#### Scenario: Default initial state
- **WHEN** `TripDetailUiState()` is created with defaults
- **THEN** `tripId` is `""`, `startingPoint` is `null`, `destination` is `null`, `intermediateStops` is empty, `isLoading` is `false`, `isSetStartingPointDialogVisible` is `false`, `isSetDestinationDialogVisible` is `false`, `isAddStopDialogVisible` is `false`, `isEditStopDialogVisible` is `false`, `editingStop` is `null`, `canAddMoreStops` is `false`, `isRemoveStopDialogVisible` is `false`, `stopToRemove` is `null`, `placeSuggestions` is empty, `isSearchingPlaces` is `false`, `searchError` is `null`, `selectedPlaceDetail` is `null`, `isResolvingPlace` is `false`, `placeDetailError` is `null`, `legs` is empty, `isCalculatingRoute` is `false`, `routeError` is `null`, and `formattedTotalDistance` is `null`

#### Scenario: Total distance present when route is complete
- **WHEN** `legs.size == stops.size - 1` and `stops.size >= 2`
- **THEN** `formattedTotalDistance` is a non-null string equal to `formatDistance` applied to the sum of every leg's `distanceMetres`

#### Scenario: Total distance unavailable when route is incomplete or absent
- **WHEN** `legs` is empty, or `legs.size` is neither `0` nor `stops.size - 1`, or `stops.size < 2`
- **THEN** `formattedTotalDistance` is `null`

### Requirement: TripDetailViewModel drives the screen
`TripDetailViewModel` SHALL, in addition to its existing stops-only and legs-only
collectors (unchanged), also maintain an independent
`combine(ObserveStopsUseCase(tripId), ObserveLegsUseCase(tripId))` collector that computes
and updates `formattedTotalDistance` on every emission from either source:
- Completeness: `legs.size == stops.size - 1 && stops.size >= 2`.
- When complete: sum `leg.distanceMetres` across all `legs`, format the sum via the
  existing `formatDistance(sum, Locale.getDefault(), application.resources)`, and set
  `formattedTotalDistance` to the formatted string.
- When not complete: set `formattedTotalDistance = null`.
- This collector is independent of, and does not alter, the existing stops-only collector
  (`startingPoint`, `destination`, `intermediateStops`, `canAddMoreStops`,
  `formattedStopCoordinates`) or the existing legs-only collector (`legs`,
  `formattedLegDistances`, `formattedLegDurations`).

#### Scenario: Total updates when route is calculated
- **WHEN** `OnCalculateRouteClicked` is dispatched and `CalculateRouteUseCase` succeeds, producing one leg per consecutive stop pair
- **THEN** `uiState.formattedTotalDistance` becomes the formatted sum of all leg distances, without any additional user action

#### Scenario: Total sums exactly two legs
- **WHEN** legs of `distanceMetres = 10000` and `distanceMetres = 2500` are observed for a trip with exactly 3 placed stops
- **THEN** `uiState.formattedTotalDistance` equals `formatDistance(12500, Locale.getDefault(), resources)`

#### Scenario: Total unavailable with no legs but a full stop set
- **WHEN** `stops.size >= 2` and no legs exist for the trip (route never calculated)
- **THEN** `uiState.formattedTotalDistance` is `null`

#### Scenario: Total unavailable with an incomplete leg set
- **WHEN** `legs.size` is neither `0` nor `stops.size - 1` for the current `stops`
- **THEN** `uiState.formattedTotalDistance` is `null`

#### Scenario: Total unavailable with fewer than two stops
- **WHEN** `stops.size < 2`
- **THEN** `uiState.formattedTotalDistance` is `null`, regardless of any legs present

#### Scenario: Total becomes unavailable after a route-invalidating stop mutation
- **WHEN** `uiState.formattedTotalDistance` holds a non-null value and a stop mutation (add, remove, move, or edit) invalidates all legs for the trip
- **THEN** `uiState.formattedTotalDistance` transitions to `null` once the legs flow emits the now-empty list

#### Scenario: Total respects the active locale's unit system
- **WHEN** the device locale is an imperial-unit locale (e.g. `en-US`)
- **THEN** `uiState.formattedTotalDistance` is expressed in miles, consistent with `formattedLegDistances` for the same legs

### Requirement: TripDetailScreen renders leg summaries between stops
The trip detail screen SHALL render a `LegSummary` composable between each pair of consecutive stops when legs are available. The leg for a given pair is matched by `fromStopId` and `toStopId`. The screen SHALL additionally render a `TripTotalSummary` composable, defined in `ui/screens/tripdetail/TripTotalSummary.kt`, as a `LazyColumn` item placed after the calculate-route button / route-error item block:
- `TripTotalSummary` accepts `modifier: Modifier = Modifier` and `formattedTotalDistance: String?`.
- When `formattedTotalDistance` is non-null, it displays the `trip_detail_total_distance_label` text alongside the value.
- When `formattedTotalDistance` is `null`, it displays the `trip_detail_total_distance_unavailable` text instead.
- `TripTotalSummary` performs no locale detection, unit conversion, string-resource-driven arithmetic, or business-logic branching beyond the null-fallback render — it is purely presentational, mirroring `LegSummary` and `TripProgressSummary`.

#### Scenario: Legs displayed between stops
- **WHEN** 3 stops exist and 2 legs are available
- **THEN** a `LegSummary` is rendered between stop 1 and stop 2, and between stop 2 and stop 3

#### Scenario: No legs available
- **WHEN** 3 stops exist but no legs are available
- **THEN** no `LegSummary` composables are rendered

#### Scenario: Total distance summary displayed with a value
- **WHEN** `uiState.formattedTotalDistance` is `"12.5 km"`
- **THEN** `TripTotalSummary` displays the `trip_detail_total_distance_label` text and the value `"12.5 km"`

#### Scenario: Total distance summary displayed as unavailable
- **WHEN** `uiState.formattedTotalDistance` is `null`
- **THEN** `TripTotalSummary` displays the `trip_detail_total_distance_unavailable` text and no numeric value

### Requirement: TripDetailScreen route preview coverage
The trip detail screen previews SHALL include:
- A preview with legs displayed between stops
- A preview with `isCalculatingRoute = true`
- A preview with `routeError` set
- A preview with a non-null `formattedTotalDistance` (satisfying non-default `UiState`-field preview coverage)
- A preview with `formattedTotalDistance = null` alongside a populated stop/leg set, showing the unavailable state

`TripTotalSummary` SHALL itself have previews covering both the value-present and value-absent (unavailable) states, each wrapped in `HeadingToVeniceTheme` with `showBackground = true`.

#### Scenario: Route previews exist
- **WHEN** the composable is inspected in Android Studio
- **THEN** preview variants for legs, calculating state, error state, total-present state, and total-unavailable state are all visible

#### Scenario: TripTotalSummary previews exist
- **WHEN** `TripTotalSummary` is inspected in Android Studio
- **THEN** at least two preview variants are visible: one with a non-null `formattedTotalDistance` and one with `null`
