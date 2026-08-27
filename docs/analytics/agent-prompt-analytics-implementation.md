# Agent Prompt: Implement Analytics Abstraction Layer

Use this prompt to instruct an agent to implement a provider-agnostic analytics abstraction for an
Android app. Replace the placeholder in the Context section with the actual path to the project's
tracking plan before use.

---

You are implementing a provider-agnostic analytics abstraction for an Android app (Kotlin + Hilt).
The goal is a clean, type-safe layer that can fan out to multiple backends without leaking
SDK-specific concerns into the rest of the codebase.

---

## CONTEXT YOU MUST READ FIRST

Before writing any code, read:
- The project's tracking plan (single source of truth for events): [PATH_TO_TRACKING_PLAN]
- The existing codebase to identify integration points, existing analytics code, and DI setup.

---

## ARCHITECTURE TO IMPLEMENT

Place all analytics code under `core/analytics/` — parallel to `data/`, `domain/`, and `ui/`.
Nothing in `domain/` or `data/` may reference analytics. Tracking calls belong in ViewModels only.

### Required files

| File | Purpose |
|------|---------|
| `AnalyticsTracking` | Shared interface with `logEvent`, `setUserProperty`, `trackException` |
| `AnalyticsClient : AnalyticsTracking` | What call sites depend on — no extra API |
| `AnalyticsProvider : AnalyticsTracking` | One implementation per backend; adds `shouldLog(event)` |
| `CompositeAnalyticsClient` | Fans out to all registered providers; silent no-op with zero providers |
| `AnalyticsEvent` | Sealed class — the full event taxonomy in one file |
| `AnalyticsUserProperty` | Sealed class — all user properties |
| `AnalyticsOperation` | Enum — bounded list of operation names for `trackException` |
| `<EnumName>` per bounded param | One file each — enum with `val value: String` in `snake_case` |
| `DebugAnalyticsProvider` | Logcat sink; class in `main`, DI binding in `debug` source set only |
| `AnalyticsModule` | `@Multibinds` declaration for release; each provider bound `@Binds @IntoSet` |

### Interface contract

```kotlin
interface AnalyticsTracking {
    fun logEvent(event: AnalyticsEvent)
    fun setUserProperty(property: AnalyticsUserProperty)
    fun trackException(throwable: Throwable, operation: AnalyticsOperation)
}
interface AnalyticsClient : AnalyticsTracking
interface AnalyticsProvider : AnalyticsTracking {
    fun shouldLog(event: AnalyticsEvent): Boolean
}
```

Providers must be thread-safe (`logEvent` may be called from any coroutine context).

---

## EVENT TAXONOMY RULES

Model every event from the tracking plan as a sealed class entry with typed constructor parameters.
No ad-hoc maps, no string constants scattered across ViewModels.

- Names: `snake_case`, `object_action`, past tense (e.g. `item_added`, not `add_item`)
- Parameters: `String`, `Int`, `Long`, `Double`, `Boolean` only — no objects, no lists
- No identifiers (UUIDs, PKs) — replace with aggregates (counts, bands)
- No free text — classify errors to a bounded enum (`error_type: "network"`, not `error.message`)
- Continuous values (distances, durations, counts with a long tail) → bands, not raw numbers
- Emit `.value` from enums, never `.name` — Kotlin names are `SCREAMING_SNAKE_CASE`

Each bounded parameter vocabulary is its own enum with `val value: String`.
Each enum lives in its own file (one class per file).
Where an enum maps or classifies (banding logic), write boundary tests covering zero, negatives,
and band-edge inputs.

---

## USER PROPERTIES

Set via `AnalyticsClient.setUserProperty(...)`, not passed with events.
- Set as early as the value is known (they apply forward only — past events are unaffected)
- No name is shared between a user property and an event parameter — the two name sets are disjoint

---

## EXCEPTIONS vs EVENTS

On every failure path, fire BOTH:
1. `logEvent(OperationFailed(operation, errorType))` → product analytics, bounded enums only
2. `trackException(throwable, operation)` → crash reporting only, carries the real stack trace

Product-analytics providers must ignore `trackException`. Crash-reporting providers must ignore
`logEvent` (use `shouldLog` to filter).
Never put a throwable or `error.message` into an event parameter.

---

## WHERE TRACKING CALLS LIVE

- ViewModels: yes — alongside the intent/result handling that causes them
- Composables: no — they are presentational; tracking here fires on recomposition
- Use cases / repositories: no — domain must not know it is being observed
- Screen-view events: fire from navigation destination changes, not ViewModel `init`
  (`init` misses back-navigation re-entry and double-fires on process-death restore)

---

## AUTOCAPTURE

Every SDK backend must have autocapture explicitly disabled at initialisation.
Any autocapture event deliberately left enabled must be listed in the tracking plan with a reason.
Where a backend's automatic event duplicates a declared one, map the declared event onto the
backend's reserved name and disable the automatic collection — emit from one source only.

---

## DEBUG PROVIDER

The Logcat provider class lives in `src/main/`. Its DI binding lives in `src/debug/` only.
Do NOT use a runtime `BuildConfig` flag inside `shouldLog()` as the gate — the source-set placement
IS the gate. A release build must have an explicit `@Multibinds` declaration so it compiles without
any provider bound.

---

## TRACKING PLAN AS SOURCE OF TRUTH

Every event in the code must have a tracking-plan entry.
Every tracking-plan entry must be implemented.
Drift in either direction is a defect.

After implementation, verify on-device: exercise each feature and confirm via logcat that each event
fires exactly once, with the expected name and parameters, and that no identifier, free text, place
name, or coordinate appears in any payload.

---

## WHAT TO DELIVER

1. All files listed in the architecture table above
2. Unit tests for every ViewModel change (dispatch intent → assert uiState / uiEffect)
3. Unit tests for every enum that contains mapping/banding logic (boundary cases)
4. Updated DI wiring so `AnalyticsClient` is injectable everywhere it is needed
5. Call sites added to the relevant ViewModels for every event in the tracking plan
6. User-property set calls at the correct moments (app start, after relevant mutations)
7. Confirmation that autocapture is disabled for the backend(s) being wired

Do not add events not in the tracking plan. Do not remove events that are in the tracking plan.
