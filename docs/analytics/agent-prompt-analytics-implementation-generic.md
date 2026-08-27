# Agent Prompt: Design and Implement Analytics Abstraction Layer (Language-Agnostic)

Use this prompt to instruct an agent to design a tracking plan and implement a provider-agnostic
analytics abstraction for any application, regardless of language, platform, or framework.

---

You are designing a tracking plan and implementing a provider-agnostic analytics abstraction layer.
The work has two sequential phases: **Plan** (define what to track and why) then **Build** (implement
the abstraction). Do not write any production code before the tracking plan is approved.

---

## CONTEXT YOU MUST READ FIRST

Before doing anything, read:
- The existing codebase to understand: the app's purpose, its main user flows, the language and
  framework in use, any existing analytics code, and the DI/wiring setup.
- Apply idiomatic patterns for the points below that say "use your language's equivalent".

---

## PHASE 1 — DESIGN THE TRACKING PLAN

### Step 1: Identify product questions

Before defining any event, identify 3–6 product questions the analytics must answer. Every event
must answer at least one of them; an event answering none does not belong in the plan.

Good product questions are specific and actionable:
- "Do users who complete step A go on to complete step B, and where do they drop out?"
- "Which features are actually used?"
- "How often does [key operation] fail in the wild?"
- "What shape do [core domain objects] have at the time of [key action]?"

### Step 2: Draft the event dictionary

For each question, identify the events that answer it. Apply these rules:

- **Names**: `snake_case`, `object_action` order, past tense — `item_added`, not `add_item`
- **12–20 events maximum** for the initial plan. Beyond that, nobody maintains it.
- Every event has: a name, its parameters, the user action or system outcome that triggers it,
  and which product question(s) it answers.
- **No identifiers** (UUIDs, PKs) — replace with aggregates (counts, bands)
- **No free text** — classify errors to a bounded vocabulary (`error_type: "network"`)
- **Band continuous values** — distances, durations, long-tail counts become string bands, not
  raw numbers. Small bounded counts (e.g. `item_count` capped by the app) may be sent raw.
- **Parameter types**: primitives only (`string`, `int`/`number`, `float`, `boolean`)
- **Emit explicit string values**, never raw enum/constant names (which are language-specific case)

Always include an `operation_failed` (or equivalent) reliability event covering tracked failure
paths, with `operation` and `error_type` as its only parameters.

Always include a `screen_viewed` (or equivalent) navigation event with `screen_name`.

### Step 3: Draft user properties

User properties are sticky attributes set independently of events; the backend attaches them to
every subsequent event for segmentation.

- Typically 2–5 properties for an initial plan (backends cap them; spend slots deliberately)
- Set as early as the value is known — they apply forward only, never retrospectively
- No name is shared between a user property and an event parameter — the two name sets must be
  fully disjoint. The same name in two scopes causes analytics tools to silently return wrong numbers.
- Band continuous values for the same reasons as event parameters

### Step 4: Document autocapture decisions

List every event the analytics SDK(s) would collect automatically. For each, record whether it is
disabled (preferred) or deliberately left enabled with a stated reason. Where an automatic event
duplicates a declared one, the declared event is the sole source — map it to the backend's reserved
name and disable automatic collection.

### Step 5: Record the consent posture

State explicitly whether analytics fires before user consent is obtained, and why that position is
defensible (or what story will close it). "We haven't thought about it" is not a posture.

### Tracking plan output format

Produce a markdown file saved at [PATH_TO_TRACKING_PLAN] with these sections:

```
# Tracking Plan

## Product Questions
Table: # | Question | Why it matters

## Event Dictionary
One subsection per category (funnel, feature adoption, reliability, screens).
Table per subsection: Event | Parameters | Trigger | Answers

## Parameter Reference
Table: Parameter | Type | Allowed values | Notes

## User Properties
Table: Property | Type | Allowed values | Set when

## Consent Posture
Current position and its rationale.

## Autocapture
Table: Event/Property | Status (Disabled / Enabled) | Reason
```

**Stop here and present the tracking plan for review before writing any code.**
Confirm the plan is approved before proceeding to Phase 2.

---

## PHASE 2 — IMPLEMENT THE ABSTRACTION

Use the approved tracking plan as the source of truth for all implementation decisions.

---

## ARCHITECTURE TO IMPLEMENT

Place all analytics code in a dedicated cross-cutting module or package — separate from business
logic, data access, and UI layers. No business-logic or data-layer code may import or reference
analytics. Tracking calls belong in the presentation/controller layer only (ViewModel, Store,
Controller, Presenter, or equivalent).

### Required components

| Component | Purpose |
|-----------|---------|
| `AnalyticsTracking` | Shared interface/protocol/trait: `logEvent`, `setUserProperty`, `trackException` |
| `AnalyticsClient` | What call sites depend on — extends `AnalyticsTracking`, adds nothing |
| `AnalyticsProvider` | One implementation per backend — extends `AnalyticsTracking`, adds `shouldLog(event)` |
| `CompositeAnalyticsClient` | Fans out to all registered providers; silent no-op with zero providers |
| `AnalyticsEvent` | Discriminated union / sealed type / enum — the full event taxonomy in one place |
| `AnalyticsUserProperty` | Discriminated union / sealed type / enum — all user properties |
| `AnalyticsOperation` | Bounded type — list of operation names used in `trackException` |
| One type per bounded parameter vocabulary | Each with an explicit `value` field in `snake_case` |
| `ConsoleAnalyticsProvider` | Development-only logging sink — active only in dev/debug builds |
| DI wiring | Providers registered at compile time or app startup, not via ad-hoc runtime calls |

### Interface contract (expressed in pseudocode — use your language's equivalent)

```
interface AnalyticsTracking {
    logEvent(event: AnalyticsEvent): void
    setUserProperty(property: AnalyticsUserProperty): void
    trackException(error: Error, operation: AnalyticsOperation): void
}

interface AnalyticsClient extends AnalyticsTracking { }

interface AnalyticsProvider extends AnalyticsTracking {
    shouldLog(event: AnalyticsEvent): boolean
}
```

Providers must be safe to call from any thread or async context.

---

## EVENT TAXONOMY RULES

Model every event from the tracking plan as a typed entry in `AnalyticsEvent` with explicit,
named parameters. No ad-hoc dictionaries, no string constants scattered across controllers.

- **Names**: `snake_case`, `object_action` order, past tense — `item_added`, not `add_item`
- **Parameter types**: primitives only (`string`, `int`/`number`, `float`, `boolean`) — no objects,
  no arrays
- **No identifiers** (UUIDs, database PKs) — replace with aggregates (counts, bands)
- **No free text** — classify errors to a bounded type (`error_type: "network"`, not `error.message`)
- **Band continuous values** — distances, durations, counts with a long tail become string bands,
  not raw numbers (`"under_50km"`, not `47382`)
- **Emit the explicit string value**, never the raw enum/constant name — raw names are
  `SCREAMING_SNAKE_CASE` or language-specific and would pollute an otherwise `snake_case` dataset

Each bounded parameter vocabulary is its own dedicated type (one type per file/module).
Where a type contains mapping or banding logic (not just a flat label list), write unit tests
covering zero, negatives, and band-edge inputs.

---

## USER PROPERTIES

Set via `AnalyticsClient.setUserProperty(...)`, not passed with events.
The backend attaches them to every subsequent event — they are not applied retrospectively.

- Set as early as the value is known, before the first event where they matter
- No name is shared between a user property and an event parameter — the two name sets must be
  fully disjoint. The same name in two different scopes causes analytics tools to silently return
  wrong numbers.

---

## EXCEPTIONS vs EVENTS

On every failure path, fire BOTH through separate channels:

1. `logEvent(OperationFailed(operation, errorType))` → product analytics, bounded types only,
   answers *how often* this fails
2. `trackException(error, operation)` → crash reporting only, carries the real error and stack
   trace, answers *where and why* it fails

Product-analytics providers must ignore `trackException` entirely.
Crash-reporting providers ignore `logEvent` (use `shouldLog` to filter).
Never put an error object, message string, or stack trace into an event parameter.

---

## WHERE TRACKING CALLS LIVE

| Layer | Allowed |
|-------|---------|
| Presentation layer (ViewModel / Store / Controller / Presenter) | **Yes** — alongside the action/result handling that causes the event |
| View / component layer (React components, templates, composables) | **No** — fires on re-render; not testable in isolation |
| Business logic layer (use cases, services, domain) | **No** — domain must not know it is being observed |
| Data layer (repositories, API clients, DB) | **No** |

Screen-view events must be fired from **navigation/routing changes**, not from component mount or
controller construction. Construction fires once per instance (missing re-entry via back navigation)
and can double-fire on process restore.

---

## AUTOCAPTURE

Every analytics SDK backend must have autocapture explicitly disabled at initialisation.
Document any autocapture event deliberately left enabled in the tracking plan with a reason.
Where a backend's automatic event duplicates a declared event, map the declared event onto the
backend's reserved name and disable the automatic collection — one source of truth per event.

---

## DEVELOPMENT-ONLY PROVIDER

The console/logging provider must be active in development builds only, never in production.
Gate it via a build-time mechanism (environment variable, build flag, compile-time DI binding) —
not a runtime conditional inside the provider itself. A runtime flag is a second knob that can
be set wrong; build-time exclusion is the only reliable gate.

In production, if no providers are bound, the composite client must silently no-op — not throw.
Ensure the DI or wiring setup compiles cleanly with zero providers registered.

---

## TRACKING PLAN AS SOURCE OF TRUTH

The approved tracking plan from Phase 1 is the source of truth for all implementation.
Every event in the code must have a tracking-plan entry.
Every tracking-plan entry must be implemented.
Drift in either direction is a defect — update the plan before changing the code, not after.

After implementation, verify in a development build: exercise each feature and confirm via the
console provider that each event fires exactly once, with the expected name and parameters, and
that no identifier, free text, or sensitive value appears in any payload.

---

## WHAT TO DELIVER

**Phase 1**
1. Tracking plan document at [PATH_TO_TRACKING_PLAN], covering all sections in the format above
2. (Pause for approval before Phase 2)

**Phase 2**
3. All components listed in the architecture table above, in the project's language and idioms
4. Unit tests for every presentation-layer change (dispatch action → assert state / side-effect)
5. Unit tests for every type containing mapping or banding logic (boundary cases)
6. DI or wiring so `AnalyticsClient` is injectable/importable everywhere it is needed
7. Call sites in the relevant controllers/ViewModels/stores for every event in the tracking plan
8. User-property set calls at the correct moments (app start, after relevant mutations)
9. Autocapture disabled for every backend being wired, confirmed in code

Do not add events not in the tracking plan. Do not remove events that are in the tracking plan.
