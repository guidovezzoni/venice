## Why

Story 4.1.1 ("Trip Overview with Per-Leg Breakdowns") asks for a trip detail screen that
gives the user a complete picture of their roadtrip at a glance: ordered stops, per-leg
breakdowns, trip totals, and progress — all of which already exist on `TripDetailScreen`
from prior stories (1.x–3.2.2). Two gaps remain that stop the screen from being a coherent
"overview":

1. The top bar always shows the static placeholder "Trip Detail" instead of the actual
   trip's name, so a user with several trips open in sequence has no header cue which trip
   they are looking at.
2. When the route has not been calculated yet, or has gone stale after a stop mutation
   (which hard-deletes all legs via `InvalidateRouteUseCase`), the screen simply omits legs
   and shows "Trip totals unavailable" with no explanation — the user has to already know
   they must tap "Calculate route" below the stop list to make totals reappear.

This change closes both gaps without introducing any new screens, dialogs, or intents.

## What Changes

- Add read access to a single trip by ID, observed as a `Flow` so the header updates live
  if the trip is renamed in a future story:
  - `TripDao.observeById(id: String): Flow<TripWithStopCount?>` — new Room query, mirrors
    the existing `observeAll()` shape (same join/count subquery), returns `null` when no
    row matches.
  - `TripRepository.observeTripById(tripId: String): Flow<Trip?>` — new interface method.
  - `TripRepositoryImpl.observeTripById` — implemented via `tripDao.observeById(id).map { it?.toDomain() }`,
    reusing the existing `TripWithStopCount.toDomain()` mapper.
  - New `ObserveTripUseCase`, a thin pass-through to `TripRepository.observeTripById`,
    mirroring the existing `ObserveStopsUseCase` / `ObserveLegsUseCase` shape.
- Add `TripDetailUiState.tripName: String?` (default `null`). `TripDetailViewModel`
  collects `ObserveTripUseCase(tripId)` and updates `tripName` from the emitted `Trip.name`
  (or leaves it `null` if the trip is not yet loaded / not found). `TripDetailScreen`'s
  `TopAppBar` renders `uiState.tripName` when non-null, falling back to the existing
  `trip_detail_title` ("Trip Detail") string resource while `tripName` is `null` — no
  loading spinner or additional state is introduced for this fallback.
- Add `TripDetailUiState.isRouteRecalculationPromptVisible: Boolean` (default `false`),
  computed in the ViewModel from the same `stops`/`legs` completeness check already used for
  `formattedTotalDistance` / `formattedTotalDuration` (`stops.size >= 2 &&
  legs.size != stops.size - 1`) — i.e. it mirrors the exact visibility guard already used by
  the existing "Calculate route" button, and is `true` whenever the totals are
  "unavailable" for a trip with 2+ stops. No new persisted field, no distinction between
  "never calculated" and "stale" — a single generic prompt covers both, per product
  clarification.
- Add a new `RouteRecalculationPrompt` composable (own file, mirroring `TripTotalSummary` /
  `TripProgressSummary`) rendered above `TripTotalSummary` in `TripDetailScreen`'s
  `LazyColumn` when `isRouteRecalculationPromptVisible` is `true`. It is a plain-language,
  tappable card/row that dispatches the **existing** `OnCalculateRouteClicked` intent — no
  new intent is introduced. Disabled while `isCalculatingRoute` / `isLoading`, matching the
  existing "Calculate route" button's enablement rule.
- Add two new string resources (`trip_detail_recalculation_prompt_message`,
  `trip_detail_recalculation_prompt_action`) to the base `values/strings.xml`, mirrored and
  translated to `values-it` and `values-es-rES`.
- Update all ~17 `TripDetailScreen` previews (and add dedicated previews for the new
  composable and the new prompt-visible/prompt-hidden states) so every `UiState` field has
  non-default coverage per the project's preview-coverage guideline.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `trip-detail`: `TripDetailUiState` gains `tripName: String?` and
  `isRouteRecalculationPromptVisible: Boolean`; `TripDetailViewModel` gains a collector for
  `ObserveTripUseCase(tripId)` and extends the existing stops/legs `combine` collector to
  also derive prompt visibility; `TripDetailScreen`'s `TopAppBar` renders the trip name (with
  fallback) and the screen renders a new tappable `RouteRecalculationPrompt` composable when
  the route is incomplete for a trip with 2+ stops.
- `trip-creation`: `TripRepository` (documented here alongside `createTrip` /
  `observeTrips`) gains `observeTripById(tripId: String): Flow<Trip?>`, implemented in
  `TripRepositoryImpl` via the new DAO query and the existing `TripWithStopCount.toDomain()`
  mapper; a new `ObserveTripUseCase` wraps it, mirroring `ObserveStopsUseCase`.
- `trip-persistence`: `TripDao` gains `observeById(id: String): Flow<TripWithStopCount?>`, a
  `Flow`-returning counterpart to the existing one-shot `getById`, reusing the same
  `stopCount` join shape as `observeAll()`.
- `route-display`: the route calculation string resources requirement gains two new keys for
  the recalculation prompt's message and action label.

## Impact

- **Affected code**: `data/database/dao/TripDao.kt`, `domain/repository/TripRepository.kt`,
  `data/repository/TripRepositoryImpl.kt`, new `domain/usecase/ObserveTripUseCase.kt`,
  `ui/state/TripDetailUiState.kt`, `ui/viewmodel/TripDetailViewModel.kt`, new
  `ui/screens/tripdetail/RouteRecalculationPrompt.kt`,
  `ui/screens/tripdetail/TripDetailScreen.kt`, `res/values/strings.xml`,
  `res/values-it/strings.xml`, `res/values-es-rES/strings.xml`.
- **Affected tests**: `TripRepositoryImplTest.kt` (new `observeTripById` cases), a new
  `ObserveTripUseCaseTest.kt`, `TripDetailViewModelTest.kt` (new `tripName` collector cases
  and prompt-visibility cases, plus an extended `createViewModel(...)` factory), a new
  `RouteRecalculationPromptTest.kt` (androidTest), and `TripDetailScreenTest.kt` (androidTest,
  new header/prompt cases).
- **No schema changes** — `TripDao.observeById` reads the existing `trips` and `stops`
  tables with the same join shape as `observeAll()`. No new database version/migration.
- **No new intents** — the prompt reuses `TripDetailUiIntent.OnCalculateRouteClicked`.
- **No new dependencies, no network/API changes.**
