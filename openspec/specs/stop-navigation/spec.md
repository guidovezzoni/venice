# Stop Navigation

## Purpose

Allows the user to launch an external navigation app directly from a pending stop card. The feature exposes a Navigate button on PENDING stops, builds a `geo:` URI from the stop's persisted coordinates and place name, and surfaces a user-friendly error when no navigation app is available.

## Requirements

### Requirement: Navigate action visible on pending stops
Each stop card rendered by `StopSection`/`FilledStop` SHALL display a "Navigate" icon button when the underlying stop's status is `StopStatus.PENDING`.

#### Scenario: Pending stop shows Navigate button
- **WHEN** a stop card is rendered for a stop with `status = StopStatus.PENDING`
- **THEN** a Navigate icon button (`Icons.Filled.Navigation`) SHALL be visible on the stop card

### Requirement: Navigate action hidden on visited stops
Each stop card rendered by `StopSection`/`FilledStop` SHALL NOT display the Navigate icon button when the underlying stop's status is `StopStatus.VISITED`.

#### Scenario: Visited stop hides Navigate button
- **WHEN** a stop card is rendered for a stop with `status = StopStatus.VISITED`
- **THEN** no Navigate icon button SHALL be visible on the stop card

### Requirement: Navigate action launches external navigation app via system picker
Tapping the Navigate button SHALL dispatch `TripDetailUiIntent.OnNavigateToStopClicked(stopId)`. The ViewModel SHALL resolve the stop and emit `TripDetailUiEffect.LaunchNavigation(latitude, longitude, placeName)` using the stop's persisted coordinates and place name. The UI layer SHALL respond to this effect by launching an `Intent(Intent.ACTION_VIEW, uri)` where `uri` is a `geo:` scheme URI, allowing the Android system to present a picker across all installed apps capable of handling it.

#### Scenario: Tapping Navigate dispatches the intent with the correct stop id
- **WHEN** the user taps the Navigate button on a stop with id `"stop-42"`
- **THEN** `TripDetailUiIntent.OnNavigateToStopClicked("stop-42")` SHALL be dispatched to the ViewModel

#### Scenario: ViewModel emits LaunchNavigation with the stop's persisted coordinates and name
- **WHEN** the ViewModel receives `OnNavigateToStopClicked(stopId)` for a stop with `latitude = 45.4642`, `longitude = 9.1900`, `placeName = "Milan"`
- **THEN** `TripDetailUiEffect.LaunchNavigation(45.4642, 9.1900, "Milan")` SHALL be emitted

#### Scenario: LaunchNavigation effect results in an ACTION_VIEW intent with a geo URI
- **WHEN** `MainScreen` collects a `TripDetailUiEffect.LaunchNavigation` effect
- **THEN** it SHALL build an `Intent` with action `Intent.ACTION_VIEW` and a `geo:` scheme data URI, and attempt to start an activity for it

### Requirement: Geo URI format includes coordinates and place name
The geo URI built for a navigation request SHALL follow the format `geo:{lat},{lon}?q={lat},{lon}({placeName})`, using the stop's persisted latitude and longitude (not a text-search query) and including the place name as a label.

#### Scenario: URI is built from persisted coordinates and place name
- **WHEN** a geo URI is built for latitude `45.4642`, longitude `9.19`, and place name `"Milan"`
- **THEN** the resulting URI string SHALL equal `geo:45.4642,9.19?q=45.4642,9.19(Milan)`

#### Scenario: Place name with special characters is safely encoded
- **WHEN** a geo URI is built for a place name containing spaces or parentheses, e.g. `"Sant'Angelo (Old Town)"`
- **THEN** the resulting URI SHALL percent-encode the place name so the URI remains well-formed and parseable

### Requirement: No-app-available error is surfaced to the user
If no installed app can resolve the navigation intent, the user SHALL see a user-friendly, localized error message instead of a silent failure or a crash.

#### Scenario: No app installed shows an error message
- **WHEN** `MainScreen` attempts to resolve an `ACTION_VIEW` + `geo:` intent and no activity can handle it
- **THEN** `TripDetailUiEffect.ShowNavigationError(message)` SHALL be emitted (or handled) with a localized, user-friendly message, and no `ActivityNotFoundException` SHALL propagate to the user

### Requirement: Navigate action is disabled while the screen is loading
The Navigate button's enabled state SHALL follow `TripDetailUiState.isLoading` — disabled when `isLoading` is `true`, matching the enablement behaviour of the other stop action buttons.

#### Scenario: Navigate button disabled during loading
- **WHEN** `TripDetailUiState.isLoading` is `true` and a pending stop's Navigate button is rendered
- **THEN** the Navigate button SHALL be disabled

#### Scenario: Navigate button enabled when not loading
- **WHEN** `TripDetailUiState.isLoading` is `false` and a pending stop's Navigate button is rendered
- **THEN** the Navigate button SHALL be enabled

### Requirement: Navigate action has an accessible content description
The Navigate icon button SHALL expose a content description of the form "Navigate to {placeName}" so screen readers announce the destination.

#### Scenario: Content description includes the place name
- **WHEN** the Navigate button is rendered for a stop with `placeName = "Milan"`
- **THEN** the button's content description SHALL be `"Navigate to Milan"`

### Requirement: Navigate action requires no additional Android permissions
Launching external navigation via an implicit `ACTION_VIEW` + `geo:` intent SHALL NOT require requesting any runtime or manifest permission from the user.

#### Scenario: No permission prompt shown
- **WHEN** the user taps Navigate on a pending stop with a navigation app installed
- **THEN** the app SHALL launch the external navigation app without presenting any permission request dialog
