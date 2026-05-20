## Context

The domain layer currently has three use cases for setting stops: `SetStartingPointUseCase`, `SetDestinationUseCase`, and `AddIntermediateStopUseCase`. All three share the same core signature `(tripId, placeName, latitude, longitude) -> Result<Stop>` and all trim the place name. The only differences are:
1. Which repository method they delegate to
2. `AddIntermediateStopUseCase` additionally enforces a 25-stop limit

The `TripDetailViewModel` injects all three and has three near-identical private methods (`setStartingPoint`, `setDestination`, `addIntermediateStop`) that follow the same pattern: set loading → call use case → dismiss dialog on success / emit error on failure.

The `MAX_STOP_COUNT` constant is duplicated in both `AddIntermediateStopUseCase` and `TripDetailViewModel`.

## Goals / Non-Goals

**Goals:**
- Reduce three use case classes to one via a `StopType` discriminator
- Eliminate duplicated `MAX_STOP_COUNT` constant
- Simplify `TripDetailViewModel` constructor (3 use case params → 1)
- Reduce three near-identical ViewModel private methods to one parameterised method
- Maintain identical runtime behaviour — this is a pure refactor with no user-facing changes

**Non-Goals:**
- Changing repository layer (`StopRepository` / `StopRepositoryImpl`) — the three repository methods have genuinely different data-layer behaviour and remain distinct
- Changing the MVI contract (`TripDetailUiState`, `TripDetailUiIntent`, `TripDetailUiEffect`) or screen composables
- Adding input validation (blank name, coordinate range checks) to the consolidated use case — the existing `SetStartingPointUseCase` and `SetDestinationUseCase` do not validate beyond trimming, and adding validation is a separate concern (the intermediate-stop-management spec already has those validations, but they are not currently implemented in the code; this refactor preserves existing behaviour)

## Decisions

### Decision 1: Single `SetStopUseCase` with `StopType` parameter

**Choice:** Add `stopType: StopType` as a fifth parameter to the `invoke` function rather than using separate overloads or a sealed class wrapper.

**Rationale:** The signature `(tripId, placeName, latitude, longitude, stopType)` is straightforward and mirrors the existing pattern. A `when` on `stopType` dispatches to the right repository method. This is the simplest approach that eliminates the duplication.

**Alternative considered:** A sealed class `SetStopRequest` wrapping all params — rejected as over-engineering for what is a simple discriminated delegation.

### Decision 2: `StopType` as an enum in `domain/model/`

**Choice:** Place `StopType` in `com.guidovezzoni.venice.domain.model`, alongside `Stop` and `StopStatus`.

**Rationale:** `StopType` is a domain concept (it discriminates the kind of stop being set). Placing it in the model package follows the existing convention for domain enums.

### Decision 3: ViewModel uses a single `setStop` helper method

**Choice:** Replace the three private methods (`setStartingPoint`, `setDestination`, `addIntermediateStop`) with one `setStop(placeName, latitude, longitude, stopType, dismissDialog)` that takes a lambda for dismissing the relevant dialog.

**Rationale:** The three methods differ only in which use case they call and which dialog flag they clear. Parameterising both eliminates the duplication while keeping the intent-handling in `onIntent()` clear.

### Decision 4: Preserve existing behaviour exactly (no new validation)

**Choice:** The consolidated `SetStopUseCase` will only trim place names and enforce the 25-stop limit for `INTERMEDIATE` — matching exactly what the three current use cases do.

**Rationale:** The `intermediate-stop-management` spec includes additional validation (blank name, coordinate range) that is not currently implemented in the code. Adding those validations would be a behaviour change, not a refactor. They should be addressed separately.

## Risks / Trade-offs

- **[Risk] Regression in stop-limit enforcement** → Mitigated by dedicated test cases in `SetStopUseCaseTest` verifying the limit applies only to `INTERMEDIATE` stops and not to `STARTING_POINT` or `DESTINATION`.
- **[Risk] Stale references to old use case names** → Mitigated by a grep sweep in the final implementation step to confirm no references remain in `app/src/`.
- **[Trade-off] Slightly larger parameter list** → The `invoke` function now takes 5 parameters instead of 4. This is acceptable for a use case that replaces three classes.
